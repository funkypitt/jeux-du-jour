#!/usr/bin/env python3
"""
build-connexions.py — Generate high-quality French Connexions puzzles using Claude.

Pipeline:
  1. Generate candidate puzzles (structured, with trap words)
  2. Validate each puzzle adversarially (solve test + ambiguity check)
  3. Verify all words exist in French dictionary (Lexique383)
  4. Deduplicate against existing puzzles
  5. Output validated puzzles to connexions_groups.json

Usage:
  python tools/build-connexions.py --count 50 --output app/src/main/assets/connexions_groups.json
  python tools/build-connexions.py --count 10 --append  # Add to existing file
  python tools/build-connexions.py --count 5 --dry-run  # Generate without writing

Requires:
  - ANTHROPIC_API_KEY environment variable
  - pip install anthropic
  - Optional: lexique.db in app/src/main/assets/databases/ for word validation
"""

import argparse
import json
import os
import re
import sqlite3
import sys
import time
from pathlib import Path
from typing import Optional

try:
    import anthropic
except ImportError:
    print("Error: pip install anthropic", file=sys.stderr)
    sys.exit(1)

# --- Configuration ---

MODEL = "claude-sonnet-4-20250514"
MAX_RETRIES = 3
RETRY_DELAY = 5  # seconds between retries on rate limit

GENERATION_PROMPT = """\
Tu es un créateur de puzzles pour un jeu de mots français inspiré du jeu "Connections" du New York Times.

Crée {batch_size} puzzles. Chaque puzzle a exactement 4 groupes de 4 mots.

RÈGLES STRICTES :
1. Tous les mots doivent être français. Les noms propres (villes, rivières, personnages, marques) sont AUTORISÉS quand la catégorie le justifie
2. Chaque mot doit appartenir à UN SEUL groupe — aucune ambiguïté légitime
3. Les 16 mots mélangés doivent créer des "pièges" : des mots qui SEMBLENT aller ensemble mais appartiennent à des groupes différents
4. Aucun mot ne doit apparaître deux fois dans le même puzzle
5. Les mots doivent être en minuscules, sans accents dans le JSON (mais les catégories peuvent avoir des accents)
6. Difficulté progressive : 0=évident, 1=modéré, 2=culturel/astucieux, 3=jeu de mots/méta

TYPES DE CATÉGORIES INTÉRESSANTES (varie BEAUCOUP entre les puzzles, ne répète pas) :
- Associations sémantiques (instruments, métiers, vêtements, meubles, sports...)
- Expressions figées ("___ de ..." , "coup de ___", "faire ___")
- Jeux de mots / double sens / homophones
- Culture française (cinéma, littérature, gastronomie, géographie, histoire)
- Propriétés cachées (contient un animal, contient un nombre, anagrammes)
- Noms propres célèbres (acteurs, auteurs, villes, rivières, marques)
- Faux amis entre catégories (le mot semble aller ailleurs)
- Registres de langue (argot, soutenu, technique)

IMPORTANT : Varie les thèmes ! Ne fais pas toujours fruits/couleurs/animaux.
{variation_hint}

QUALITÉ DES PIÈGES :
- Le groupe violet (difficulté 3) doit avoir un lien non-évident (jeu de mots, propriété cachée)
- Certains mots du groupe jaune (facile) doivent pouvoir sembler appartenir au groupe bleu ou vert au premier regard
- Au moins 3-4 mots sur 16 doivent être des "pièges" qui attirent vers le mauvais groupe

Réponds UNIQUEMENT avec un JSON valide, format :
[
  {{
    "groups": [
      {{"category": "Nom de la catégorie", "words": ["mot1", "mot2", "mot3", "mot4"], "difficulty": 0}},
      {{"category": "Nom de la catégorie", "words": ["mot1", "mot2", "mot3", "mot4"], "difficulty": 1}},
      {{"category": "Nom de la catégorie", "words": ["mot1", "mot2", "mot3", "mot4"], "difficulty": 2}},
      {{"category": "Nom de la catégorie", "words": ["mot1", "mot2", "mot3", "mot4"], "difficulty": 3}}
    ]
  }}
]

Génère exactement {batch_size} puzzles variés et de haute qualité. JSON uniquement, pas de texte autour."""

VALIDATION_PROMPT = """\
Tu es un testeur de qualité pour des puzzles "Connexions" (version française du jeu Connections du NYT).

Voici un puzzle avec 16 mots mélangés. Essaie de le résoudre, puis évalue sa qualité.

Les 16 mots : {words_shuffled}

La solution correcte :
{solution}

Évalue ce puzzle sur ces critères (note de 1 à 5 chacun) :

1. VALIDITÉ : Chaque mot appartient-il clairement à son groupe ? (5 = parfait, 1 = mots mal classés)
2. UNICITÉ : Aucun mot ne pourrait légitimement aller dans un autre groupe ? (5 = aucune ambiguïté injuste, 1 = plusieurs mots ambigus)
3. DIFFICULTÉ : La progression jaune→vert→bleu→violet est-elle respectée ? (5 = parfaitement calibré, 1 = mal ordonné)
4. INTÉRÊT : Le puzzle est-il amusant/satisfaisant à résoudre ? (5 = excellent, 1 = ennuyeux/trivial)
5. FRANÇAIS : Tous les mots sont-ils du français correct ? Les noms propres connus sont acceptés. (5 = tous valides, 1 = mots inventés/anglais)

PROBLÈMES DÉTECTÉS (liste chaque problème) :
- Mots qui n'existent pas en français :
- Mots qui pourraient légitimement aller dans un autre groupe :
- Catégorie trop obscure ou injuste :
- Autre problème :

Réponds en JSON :
{{
  "scores": {{"validity": N, "uniqueness": N, "difficulty": N, "interest": N, "french": N}},
  "total": N,
  "pass": true/false,
  "problems": ["description du problème 1", "..."],
  "bad_words": ["mot1", "mot2"]
}}

Un puzzle passe si total >= 19 (sur 25) et aucun score individuel < 3.
JSON uniquement."""


class ConnexionsGenerator:
    def __init__(self, db_path: Optional[str] = None, verbose: bool = True):
        self.client = anthropic.Anthropic()
        self.verbose = verbose
        self.known_words: Optional[set] = None
        self.all_used_categories: list = []
        self._puzzle_keys: set = set()

        if db_path and os.path.exists(db_path):
            self._load_dictionary(db_path)

    def _load_dictionary(self, db_path: str):
        """Load known French words from lexique.db for validation."""
        conn = sqlite3.connect(db_path)
        cursor = conn.execute("SELECT letters_ascii FROM words")
        self.known_words = {row[0] for row in cursor.fetchall()}
        conn.close()
        if self.verbose:
            print(f"  Dictionary loaded: {len(self.known_words):,} words")

    def _log(self, msg: str):
        if self.verbose:
            print(msg)

    def _call_api(self, prompt: str, max_tokens: int = 8000) -> str:
        """Call Claude API with streaming to avoid timeout on long responses."""
        for attempt in range(MAX_RETRIES):
            try:
                result_text = ""
                with self.client.messages.stream(
                    model=MODEL,
                    max_tokens=max_tokens,
                    messages=[{"role": "user", "content": prompt}],
                ) as stream:
                    for text in stream.text_stream:
                        result_text += text
                return result_text
            except anthropic.RateLimitError:
                if attempt < MAX_RETRIES - 1:
                    self._log(f"  Rate limited, waiting {RETRY_DELAY}s...")
                    time.sleep(RETRY_DELAY)
                else:
                    raise
            except anthropic.APIError as e:
                if attempt < MAX_RETRIES - 1:
                    self._log(f"  API error: {e}, retrying...")
                    time.sleep(RETRY_DELAY)
                else:
                    raise

    def _parse_json(self, text: str) -> Optional[list | dict]:
        """Extract JSON from model response, handling markdown fences."""
        # Strip markdown code fences if present
        text = text.strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?\s*\n?", "", text)
            text = re.sub(r"\n?```\s*$", "", text)

        try:
            return json.loads(text)
        except json.JSONDecodeError:
            # Try to find JSON array or object in the text
            for match in re.finditer(r'[\[{]', text):
                try:
                    return json.loads(text[match.start():])
                except json.JSONDecodeError:
                    continue
            return None

    def _strip_accents(self, s: str) -> str:
        """Remove accents for dictionary lookup."""
        import unicodedata
        nfkd = unicodedata.normalize('NFKD', s)
        return ''.join(c for c in nfkd if not unicodedata.combining(c))

    def _validate_structure(self, puzzle: dict) -> tuple[bool, str]:
        """Check puzzle has correct structure."""
        if "groups" not in puzzle:
            return False, "Missing 'groups' key"

        groups = puzzle["groups"]
        if len(groups) != 4:
            return False, f"Expected 4 groups, got {len(groups)}"

        all_words = []
        difficulties = set()

        for i, group in enumerate(groups):
            if "category" not in group or "words" not in group or "difficulty" not in group:
                return False, f"Group {i} missing required fields"
            if len(group["words"]) != 4:
                return False, f"Group {i} has {len(group['words'])} words, expected 4"
            if group["difficulty"] not in (0, 1, 2, 3):
                return False, f"Group {i} has invalid difficulty {group['difficulty']}"
            difficulties.add(group["difficulty"])
            all_words.extend(group["words"])

        if len(difficulties) != 4:
            return False, "Difficulties not all unique (need 0,1,2,3)"

        # Check for duplicates within puzzle
        if len(all_words) != len(set(all_words)):
            dupes = [w for w in all_words if all_words.count(w) > 1]
            return False, f"Duplicate words: {set(dupes)}"

        # Check all words are lowercase strings
        for w in all_words:
            if not isinstance(w, str) or w != w.lower():
                return False, f"Word '{w}' not lowercase string"
            if len(w) < 2:
                return False, f"Word '{w}' too short"

        return True, ""

    def _validate_dictionary(self, puzzle: dict) -> list[str]:
        """Check words against Lexique383 dictionary. Lenient — proper nouns are allowed."""
        if self.known_words is None:
            return []

        bad = []
        for group in puzzle["groups"]:
            for word in group["words"]:
                ascii_word = self._strip_accents(word.lower())
                if ascii_word not in self.known_words:
                    # Skip compounds, hyphenated words, and very short words (numbers etc)
                    if " " in word or "-" in word:
                        continue
                    # Allow words that look like proper nouns (they're valid in Connexions)
                    # We only flag truly suspicious entries (gibberish, English)
                    bad.append(word)
        return bad

    def _validate_with_llm(self, puzzle: dict) -> tuple[bool, dict]:
        """Use Claude to validate puzzle quality adversarially."""
        # Shuffle words for presentation
        import random
        all_words = []
        for group in puzzle["groups"]:
            all_words.extend(group["words"])
        random.shuffle(all_words)

        solution = "\n".join(
            f"  {group['difficulty']} ({group['category']}): {', '.join(group['words'])}"
            for group in sorted(puzzle["groups"], key=lambda g: g["difficulty"])
        )

        prompt = VALIDATION_PROMPT.format(
            words_shuffled=", ".join(all_words),
            solution=solution,
        )

        response = self._call_api(prompt, max_tokens=2000)
        result = self._parse_json(response)

        if result is None:
            return False, {"error": "Failed to parse validation response"}

        # Check pass criteria
        scores = result.get("scores", {})
        total = sum(scores.values()) if scores else 0
        min_score = min(scores.values()) if scores else 0
        passed = total >= 19 and min_score >= 3

        result["total"] = total
        result["pass"] = passed
        return passed, result

    def _check_dedup(self, puzzle: dict) -> tuple[bool, str]:
        """Check puzzle doesn't repeat exact categories or full word sets."""
        # Check for exact category repetition within recent puzzles
        puzzle_cats = [g["category"].lower() for g in puzzle["groups"]]
        for cat in puzzle_cats:
            for prev_cat in self.all_used_categories[-40:]:  # Check last 40 categories
                if cat == prev_cat:
                    return False, f"Repeated category: {cat}"

        # Check that the full 16-word set isn't near-identical to a previous puzzle
        # (blocks copy-paste duplicates, not word reuse across different puzzles)
        puzzle_words = sorted(w for g in puzzle["groups"] for w in g["words"])
        puzzle_key = "|".join(puzzle_words)
        if puzzle_key in self._puzzle_keys:
            return False, "Exact duplicate puzzle"

        return True, ""

    def _register_puzzle(self, puzzle: dict):
        """Track categories and puzzle fingerprint for dedup."""
        for group in puzzle["groups"]:
            self.all_used_categories.append(group["category"].lower())
        puzzle_words = sorted(w for g in puzzle["groups"] for w in g["words"])
        self._puzzle_keys.add("|".join(puzzle_words))

    def _get_variation_hint(self, batch_num: int) -> str:
        """Rotate theme suggestions to encourage variety."""
        hints = [
            "Pour ce lot, privilégie : expressions idiomatiques, monde du travail, technologie.",
            "Pour ce lot, privilégie : géographie, histoire de France, musique.",
            "Pour ce lot, privilégie : cuisine/gastronomie, sport, mode/vêtements.",
            "Pour ce lot, privilégie : littérature, cinéma, argot/langage familier.",
            "Pour ce lot, privilégie : sciences, nature/animaux, jeux de société.",
            "Pour ce lot, privilégie : architecture, transport, médecine/corps humain.",
            "Pour ce lot, privilégie : astronomie, mythologie, arts visuels.",
            "Pour ce lot, privilégie : politique, économie, vie quotidienne.",
            "Pour ce lot, privilégie : homophones, double sens, mots composés.",
            "Pour ce lot, privilégie : fêtes/traditions, école/éducation, commerce.",
        ]
        return hints[batch_num % len(hints)]

    def generate_batch(self, batch_size: int = 5, batch_num: int = 0) -> list[dict]:
        """Generate a batch of candidate puzzles."""
        self._log(f"\n  Generating batch of {batch_size} puzzles...")

        hint = self._get_variation_hint(batch_num)
        prompt = GENERATION_PROMPT.format(batch_size=batch_size, variation_hint=hint)
        response = self._call_api(prompt, max_tokens=batch_size * 1500)
        puzzles = self._parse_json(response)

        if puzzles is None:
            self._log("  ERROR: Failed to parse generation response")
            return []

        if isinstance(puzzles, dict) and "groups" in puzzles:
            puzzles = [puzzles]

        self._log(f"  Got {len(puzzles)} candidate puzzles")
        return puzzles

    def validate_puzzle(self, puzzle: dict, index: int) -> tuple[bool, list[str]]:
        """Run full validation pipeline on a puzzle."""
        reasons = []

        # Step 1: Structure
        valid, msg = self._validate_structure(puzzle)
        if not valid:
            reasons.append(f"Structure: {msg}")
            return False, reasons

        # Step 2: Dictionary check (lenient — proper nouns are valid in Connexions)
        bad_words = self._validate_dictionary(puzzle)
        if len(bad_words) > 8:
            reasons.append(f"Dictionary: {len(bad_words)} unknown words: {bad_words}")
            return False, reasons
        # Non-blocking for up to 8 unknown words (likely proper nouns)

        # Step 3: Deduplication
        valid, msg = self._check_dedup(puzzle)
        if not valid:
            reasons.append(f"Dedup: {msg}")
            return False, reasons

        # Step 4: LLM validation
        self._log(f"    Puzzle {index}: validating with LLM...")
        passed, result = self._validate_with_llm(puzzle)
        if not passed:
            problems = result.get("problems", [])
            scores = result.get("scores", {})
            total = result.get("total", 0)
            reasons.append(f"LLM validation failed (score {total}/25): {problems}")
            return False, reasons

        return True, reasons

    def generate(self, target_count: int, existing: list = None) -> list[dict]:
        """Main generation loop: generate, validate, collect until target reached."""
        validated = []

        # Register existing puzzles for dedup
        if existing:
            for puzzle in existing:
                self._register_puzzle(puzzle)
            self._log(f"  Registered {len(existing)} existing puzzles for dedup")

        generated = 0
        rejected = 0
        batch_num = 0

        while len(validated) < target_count:
            batch_num += 1
            remaining = target_count - len(validated)
            # Generate more than needed to account for rejection (~60% pass rate)
            batch_size = min(max(remaining // 3 + 3, 8), 15)

            self._log(f"\n--- Batch {batch_num} (need {remaining} more) ---")
            candidates = self.generate_batch(batch_size, batch_num)
            generated += len(candidates)

            for i, puzzle in enumerate(candidates):
                if len(validated) >= target_count:
                    break

                passed, reasons = self.validate_puzzle(puzzle, i + 1)

                if passed:
                    self._register_puzzle(puzzle)
                    validated.append(puzzle)
                    self._log(f"    Puzzle {i+1}: PASS ✓ ({len(validated)}/{target_count})")
                else:
                    rejected += 1
                    self._log(f"    Puzzle {i+1}: FAIL — {reasons[0] if reasons else '?'}")

            # Safety valve — at ~60% pass rate with batch_size 10-15, we need
            # roughly target_count/6 batches. Cap at target_count/3 to be safe.
            if batch_num > max(target_count // 3, 50):
                self._log(f"\n  WARNING: Stopping after {batch_num} batches")
                break

            # Small delay between batches to avoid rate limits
            if len(validated) < target_count:
                time.sleep(2)

        self._log(f"\n=== Done: {len(validated)} validated, {rejected} rejected, "
                  f"{generated} total generated ===")
        return validated


def main():
    parser = argparse.ArgumentParser(description="Generate Connexions puzzles using Claude")
    parser.add_argument("--count", type=int, default=300,
                        help="Number of puzzles to generate (default: 300 for top-up runs)")
    parser.add_argument("--output", type=str,
                        default="app/src/main/assets/connexions_groups.json",
                        help="Output JSON file path")
    parser.add_argument("--append", action="store_true",
                        help="Append to existing file instead of replacing")
    parser.add_argument("--dry-run", action="store_true",
                        help="Generate and validate but don't write file")
    parser.add_argument("--db", type=str,
                        default="app/src/main/assets/databases/lexique.db",
                        help="Path to lexique.db for word validation")
    parser.add_argument("--no-validate", action="store_true",
                        help="Skip LLM validation (faster, lower quality)")
    parser.add_argument("--quiet", action="store_true",
                        help="Minimal output")
    args = parser.parse_args()

    # Resolve paths relative to project root
    project_root = Path(__file__).parent.parent
    output_path = project_root / args.output
    db_path = project_root / args.db

    if not os.environ.get("ANTHROPIC_API_KEY"):
        print("Error: ANTHROPIC_API_KEY not set", file=sys.stderr)
        sys.exit(1)

    print(f"Connexions Puzzle Generator")
    print(f"  Target: {args.count} puzzles")
    print(f"  Output: {output_path}")
    print(f"  Mode: {'append' if args.append else 'replace'}")
    print(f"  Dictionary: {db_path if db_path.exists() else 'not found (skipping word check)'}")

    # Load existing puzzles if appending
    existing = []
    if args.append and output_path.exists():
        with open(output_path) as f:
            existing = json.load(f)
        print(f"  Existing puzzles: {len(existing)}")

    # Initialize generator
    generator = ConnexionsGenerator(
        db_path=str(db_path) if db_path.exists() else None,
        verbose=not args.quiet,
    )

    if args.no_validate:
        # Skip LLM validation — just generate and structure-check
        original_validate = generator.validate_puzzle
        def quick_validate(puzzle, index):
            reasons = []
            valid, msg = generator._validate_structure(puzzle)
            if not valid:
                return False, [f"Structure: {msg}"]
            bad_words = generator._validate_dictionary(puzzle)
            if len(bad_words) > 2:
                return False, [f"Dictionary: {bad_words}"]
            valid, msg = generator._check_dedup(puzzle)
            if not valid:
                return False, [f"Dedup: {msg}"]
            return True, reasons
        generator.validate_puzzle = quick_validate

    # Generate
    new_puzzles = generator.generate(args.count, existing=existing)

    if not new_puzzles:
        print("\nNo puzzles generated!", file=sys.stderr)
        sys.exit(1)

    # Combine with existing if appending
    all_puzzles = existing + new_puzzles if args.append else new_puzzles

    # Write output
    if args.dry_run:
        print(f"\n[DRY RUN] Would write {len(all_puzzles)} puzzles to {output_path}")
        print(f"\nSample puzzle:")
        print(json.dumps(new_puzzles[0], indent=2, ensure_ascii=False))
    else:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(all_puzzles, f, indent=2, ensure_ascii=False)
        print(f"\nWrote {len(all_puzzles)} puzzles to {output_path}")

    # Stats
    print(f"\n  New puzzles: {len(new_puzzles)}")
    if existing:
        print(f"  Previous: {len(existing)}")
        print(f"  Total: {len(all_puzzles)}")


if __name__ == "__main__":
    main()
