#!/usr/bin/env python3
"""
build-lexique.py — Convert Lexique383.tsv to Room-compatible SQLite .db or JSON.

Usage:
    python build-lexique.py Lexique383.tsv --format db -o ../app/src/main/assets/databases/lexique.db
    python build-lexique.py Lexique383.tsv --format json -o words.json

The TSV is available from http://www.lexique.org/
"""

import argparse
import csv
import hashlib
import json
import re
import sqlite3
import struct
import sys
import unicodedata
from pathlib import Path


def strip_accents(s: str) -> str:
    nfkd = unicodedata.normalize("NFD", s)
    return "".join(c for c in nfkd if unicodedata.category(c) != "Mn")


def is_alpha_only(s: str) -> bool:
    return bool(re.match(r"^[a-zA-ZÀ-ÿ]+$", s))


def sorted_unique_letters(s: str) -> str:
    return "".join(sorted(set(s.lower())))


def compute_room_identity_hash(entities_ddl: list[str]) -> str:
    """Compute the Room identity hash from DDL statements."""
    combined = "".join(entities_ddl)
    return hashlib.md5(combined.encode("utf-8")).hexdigest()


VALID_CGRAMS = {"NOM", "ADJ", "VER", "ADV"}


def load_lexique(tsv_path: str):
    """Load and filter words from Lexique383.tsv."""
    words = {}  # ortho -> best entry (highest frequency)

    with open(tsv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            ortho = row.get("ortho", "").strip()
            if not ortho:
                continue

            # Filter: alpha only, 2-15 letters
            if not is_alpha_only(ortho):
                continue
            if len(ortho) < 2 or len(ortho) > 15:
                continue

            # Filter: valid grammatical category
            cgram = row.get("cgram", "").strip()
            if cgram not in VALID_CGRAMS:
                continue

            # Filter: frequency > 0
            try:
                freq = float(row.get("freqlemfilms2", "0") or "0")
            except ValueError:
                freq = 0.0
            if freq <= 0:
                continue

            letters_ascii = strip_accents(ortho).lower()
            letter_count = len(ortho)
            letter_set = sorted_unique_letters(letters_ascii)
            is_common = freq > 10.0

            entry = {
                "ortho": ortho,
                "letters_ascii": letters_ascii,
                "letter_count": letter_count,
                "letter_set": letter_set,
                "frequency": freq,
                "cgram": cgram,
                "is_common": is_common,
            }

            # Deduplicate: keep highest frequency
            if ortho not in words or freq > words[ortho]["frequency"]:
                words[ortho] = entry

    # Sort by letters_ascii for deterministic ordering
    result = sorted(words.values(), key=lambda w: w["letters_ascii"])
    return result


# The DDL must match Room's generated schema exactly
WORDS_TABLE_DDL = (
    "CREATE TABLE IF NOT EXISTS `words` ("
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
    "`ortho` TEXT NOT NULL, "
    "`letters_ascii` TEXT NOT NULL, "
    "`letter_count` INTEGER NOT NULL, "
    "`letter_set` TEXT NOT NULL, "
    "`frequency` REAL NOT NULL, "
    "`cgram` TEXT NOT NULL, "
    "`is_common` INTEGER NOT NULL)"
)


def write_db(words: list[dict], output_path: str):
    """Write words to Room-compatible SQLite database."""
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        path.unlink()

    conn = sqlite3.connect(str(path))
    c = conn.cursor()

    # Create words table
    c.execute(WORDS_TABLE_DDL)

    # Create indices for common queries
    c.execute("CREATE INDEX IF NOT EXISTS `index_words_letter_count` ON `words` (`letter_count`)")
    c.execute("CREATE INDEX IF NOT EXISTS `index_words_letters_ascii` ON `words` (`letters_ascii`)")
    c.execute("CREATE INDEX IF NOT EXISTS `index_words_is_common` ON `words` (`is_common`)")
    c.execute("CREATE INDEX IF NOT EXISTS `index_words_letter_set` ON `words` (`letter_set`)")

    # NOTE: Do NOT create room_master_table here.
    # Room will create it and populate the identity hash itself
    # when the DB is loaded via createFromAsset().

    # Insert words
    for w in words:
        c.execute(
            "INSERT INTO `words` "
            "(`ortho`, `letters_ascii`, `letter_count`, `letter_set`, `frequency`, `cgram`, `is_common`) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                w["ortho"],
                w["letters_ascii"],
                w["letter_count"],
                w["letter_set"],
                w["frequency"],
                w["cgram"],
                1 if w["is_common"] else 0,
            ),
        )

    conn.commit()

    # Stats
    c.execute("SELECT COUNT(*) FROM words")
    total = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM words WHERE is_common = 1")
    common = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM words WHERE letter_count = 5 AND is_common = 1")
    five_common = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM words WHERE length(letter_set) = 7 AND is_common = 1")
    pangram_candidates = c.fetchone()[0]

    conn.close()

    print(f"Wrote {total} words to {output_path}")
    print(f"  Common words (freq > 10): {common}")
    print(f"  Common 5-letter words (for Le Mot): {five_common}")
    print(f"  Pangram candidates (7 unique letters, common): {pangram_candidates}")
    print(f"  (Room will compute identity hash at runtime)")


def write_json(words: list[dict], output_path: str):
    """Write words to JSON format (for local-2p-games or other uses)."""
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)

    # Simplified format for JSON
    json_words = [
        {
            "w": w["ortho"],
            "a": w["letters_ascii"],
            "n": w["letter_count"],
            "f": round(w["frequency"], 2),
            "c": 1 if w["is_common"] else 0,
        }
        for w in words
    ]

    with open(str(path), "w", encoding="utf-8") as f:
        json.dump(json_words, f, ensure_ascii=False)

    print(f"Wrote {len(json_words)} words to {output_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Convert Lexique383.tsv to Room .db or JSON"
    )
    parser.add_argument("input", help="Path to Lexique383.tsv")
    parser.add_argument(
        "--format",
        choices=["db", "json"],
        default="db",
        help="Output format (default: db)",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Output file path (default: lexique.db or lexique.json)",
    )
    args = parser.parse_args()

    if args.output is None:
        args.output = "lexique.db" if args.format == "db" else "lexique.json"

    print(f"Loading {args.input}...")
    words = load_lexique(args.input)
    print(f"Filtered to {len(words)} unique words")

    if args.format == "db":
        write_db(words, args.output)
    else:
        write_json(words, args.output)

    print("Done!")


if __name__ == "__main__":
    main()
