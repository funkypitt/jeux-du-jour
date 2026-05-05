#!/usr/bin/env python3
"""
build-motdujour.py — Extract charming/archaic/familiar words from the Littré DB
for the "Mot du Jour" feature in Jeux du Jour.

Selects ~2500 words that are:
- Short enough to display nicely (4-14 letters)
- Have concise definitions (not giant encyclopedic entries)
- Have etymology (adds charm)
- Interesting POS tags (s.m., s.f., v.a., adj. — not just variants)
- Filters out overly technical/medical/botanical terms

Outputs: mot_du_jour.json — a lightweight asset (~1-2 MB) with:
  [{terme, prononciation, nature, definition, etymologie}, ...]

Usage:
  python tools/build-motdujour.py --db /path/to/littre.db
  python tools/build-motdujour.py  # uses default path
"""

import argparse
import json
import os
import re
import sqlite3
import sys
from pathlib import Path
from html.parser import HTMLParser


class HTMLStripper(HTMLParser):
    """Strip HTML tags, keeping text content."""
    def __init__(self):
        super().__init__()
        self.result = []
        self.skip = False

    def handle_starttag(self, tag, attrs):
        if tag in ('sup', 'sub'):
            self.skip = True

    def handle_endtag(self, tag):
        if tag in ('sup', 'sub'):
            self.skip = False

    def handle_data(self, data):
        if not self.skip:
            self.result.append(data)

    def get_text(self):
        return ''.join(self.result).strip()


def strip_html(html: str) -> str:
    """Remove HTML tags from a string."""
    if not html:
        return ""
    stripper = HTMLStripper()
    stripper.feed(html)
    return stripper.get_text()


def clean_definition(corps: str) -> str:
    """Extract the first meaningful definition from the corps field."""
    if not corps:
        return ""

    text = strip_html(corps)

    # Take first 2-3 sentences max (up to ~300 chars)
    # Split on common sentence boundaries
    sentences = re.split(r'(?<=[.!?])\s+', text)

    result = ""
    for sent in sentences:
        if len(result) + len(sent) > 350:
            break
        result += sent + " "
        # Stop after first real definition sentence (avoid examples/citations)
        if len(result) > 60:
            break

    result = result.strip()
    # Remove leading numbers like "1° " or "1. "
    result = re.sub(r'^\d+[°.]\s*', '', result)
    return result


def clean_etymology(etymologie: str) -> str:
    """Extract a clean etymology snippet."""
    if not etymologie:
        return ""

    text = strip_html(etymologie)
    # Take first sentence or up to 200 chars
    text = text[:200]
    # Cut at last complete sentence
    last_period = text.rfind('.')
    if last_period > 50:
        text = text[:last_period + 1]
    return text.strip()


def is_interesting_word(terme: str, nature: str, corps: str, etymologie: str) -> bool:
    """Filter for words that are charming, archaic, or interesting."""

    terme_lower = terme.lower()

    # Reject: too technical (medical/anatomical/botanical/chemical prefixes)
    boring_prefixes = [
        'hydro', 'électro', 'photo', 'thermo', 'pneumo', 'cardio',
        'neuro', 'gastro', 'hémo', 'héma', 'patho', 'psycho',
        'rhino', 'laryngo', 'broncho', 'ostéo', 'arthro',
    ]
    for prefix in boring_prefixes:
        if terme_lower.startswith(prefix):
            return False

    # Reject: Greek/Latin compound technical terms (too many syllables, too obscure)
    if len(terme) > 10 and any(x in terme_lower for x in ['ologie', 'graphie', 'métrie', 'scopie', 'ectomie', 'plasie']):
        return False

    # Reject: nature field suggests it's just a variant or reference
    nature_text = strip_html(nature) if nature else ""
    if 'voy.' in nature_text.lower() or 'voyez' in nature_text.lower():
        return False

    # Prefer: words with clear POS (noun, verb, adjective, adverb)
    good_natures = ['s. m.', 's. f.', 'v. a.', 'v. n.', 'v. réfl.', 'adj.', 'adv.']
    if nature_text and not any(n in nature_text for n in good_natures):
        return False

    # Reject: definition is too short (probably a cross-reference)
    corps_text = strip_html(corps) if corps else ""
    if len(corps_text) < 30:
        return False

    # Reject: definition starts with "Voy." or "V." (cross-reference)
    if corps_text.startswith(('Voy.', 'V. ', 'Voyez')):
        return False

    # Prefer: has etymology (adds depth and charm)
    if not etymologie or len(strip_html(etymologie)) < 10:
        return False

    return True


def score_word(terme: str, nature: str, corps: str, etymologie: str) -> float:
    """Score a word for interestingness (higher = more likely to include)."""
    score = 0.0
    terme_lower = terme.lower()
    corps_text = strip_html(corps)
    etym_text = strip_html(etymologie)

    # Bonus: short, punchy definitions (easier to read in-app)
    if 30 < len(corps_text) < 150:
        score += 2.0
    elif len(corps_text) < 250:
        score += 1.0

    # Bonus: archaic/regional markers in definition
    archaic_markers = ['vieux', 'ancien', 'autrefois', 'jadis', 'désuet',
                       'populaire', 'familier', 'terme provincial',
                       'terme de féodalité', 'terme de marine',
                       'terme de vénerie', 'terme de fauconnerie',
                       'terme de blason', 'terme de manége']
    for marker in archaic_markers:
        if marker in corps_text.lower():
            score += 3.0
            break

    # Bonus: concrete/tangible definitions (tools, food, trades)
    concrete_markers = ['instrument', 'outil', 'celui qui', 'celle qui',
                        'sorte de', 'espèce de', 'nom donné', 'boisson',
                        'plat', 'étoffe', 'pièce', 'petit']
    for marker in concrete_markers:
        if marker in corps_text.lower():
            score += 1.5
            break

    # Bonus: has Greek/Latin etymology (charming)
    if any(x in etym_text.lower() for x in ['latin', 'grec', 'provenç', 'italien', 'espagn', 'arabe']):
        score += 1.0

    # Bonus: word length 5-9 (sweet spot for memorability)
    if 5 <= len(terme) <= 9:
        score += 1.0

    # Penalty: very common words (everyone knows these)
    common_words = {
        'maison', 'table', 'chaise', 'porte', 'livre', 'homme', 'femme',
        'enfant', 'manger', 'boire', 'faire', 'dire', 'avoir', 'être',
        'grand', 'petit', 'blanc', 'noir', 'rouge', 'bleu',
    }
    if terme_lower in common_words:
        score -= 5.0

    return score


def main():
    parser = argparse.ArgumentParser(description="Extract Mot du Jour words from Littré DB")
    parser.add_argument("--db", type=str,
                        default="/home/freedomfighter/.local/share/Trash/files/littré/littre_app/assets/littre.db",
                        help="Path to littre.db")
    parser.add_argument("--output", type=str,
                        default="app/src/main/assets/mot_du_jour.json",
                        help="Output JSON path (relative to project root)")
    parser.add_argument("--count", type=int, default=2500,
                        help="Target number of words to extract")
    parser.add_argument("--min-score", type=float, default=2.0,
                        help="Minimum interestingness score")
    args = parser.parse_args()

    project_root = Path(__file__).parent.parent
    output_path = project_root / args.output

    if not os.path.exists(args.db):
        print(f"Error: Littré DB not found at {args.db}", file=sys.stderr)
        sys.exit(1)

    print(f"Mot du Jour Extractor")
    print(f"  Source: {args.db}")
    print(f"  Output: {output_path}")
    print(f"  Target: {args.count} words")

    conn = sqlite3.connect(args.db)
    cursor = conn.execute("""
        SELECT terme, prononciation, nature, corps, etymologie
        FROM entries
        WHERE length(terme) BETWEEN 4 AND 14
          AND etymologie IS NOT NULL
          AND length(etymologie) > 20
          AND length(corps) BETWEEN 50 AND 2000
    """)

    candidates = []
    total_scanned = 0

    for row in cursor:
        total_scanned += 1
        terme, prononciation, nature, corps, etymologie = row

        if not is_interesting_word(terme, nature, corps, etymologie):
            continue

        score = score_word(terme, nature, corps, etymologie)
        if score < args.min_score:
            continue

        # Clean and format
        definition = clean_definition(corps)
        etym = clean_etymology(etymologie)
        nature_clean = strip_html(nature) if nature else ""

        if not definition or len(definition) < 20:
            continue

        candidates.append({
            "score": score,
            "entry": {
                "terme": terme.capitalize(),
                "prononciation": prononciation.strip() if prononciation else None,
                "nature": nature_clean,
                "definition": definition,
                "etymologie": etym if etym else None,
            }
        })

    print(f"  Scanned: {total_scanned} entries")
    print(f"  Candidates: {len(candidates)}")

    # Sort by score descending, take top N
    candidates.sort(key=lambda x: x["score"], reverse=True)
    selected = [c["entry"] for c in candidates[:args.count]]

    # Sort final output alphabetically for deterministic file content
    selected.sort(key=lambda x: x["terme"].lower())

    # Remove None fields to save space
    for entry in selected:
        if entry["prononciation"] is None:
            del entry["prononciation"]
        if entry["etymologie"] is None:
            del entry["etymologie"]

    # Write output
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(selected, f, ensure_ascii=False, indent=1)

    file_size = output_path.stat().st_size
    print(f"\n  Selected: {len(selected)} words")
    print(f"  File size: {file_size / 1024:.1f} KB")
    print(f"  That's {len(selected) / 365:.1f} years of daily words!")

    # Show some samples
    import random
    print(f"\n  Sample words:")
    samples = random.sample(selected, min(8, len(selected)))
    for s in samples:
        print(f"    {s['terme']} ({s['nature']}) — {s['definition'][:80]}...")

    conn.close()


if __name__ == "__main__":
    main()
