#!/usr/bin/env bash
set -euo pipefail

DEST="app/src/main/assets/kjv.sqlite"
URL="https://github.com/midvash/bible-data/raw/main/versions/en/kjv/kjv.sqlite"
mkdir -p "$(dirname "$DEST")"
curl --fail --location --retry 3 --retry-delay 2 "$URL" --output "$DEST"

python3 - <<'PY'
import sqlite3
path = "app/src/main/assets/kjv.sqlite"
db = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
tables = {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
if "verses" not in tables:
    raise SystemExit("Bible database is missing the verses table")
count = db.execute("SELECT COUNT(*) FROM verses").fetchone()[0]
books = db.execute("SELECT COUNT(DISTINCT book_id) FROM verses").fetchone()[0]
if count != 31102 or books != 66:
    raise SystemExit(f"Unexpected Bible data: {books} books, {count} verses; expected 66 books and 31,102 verses")
checks = [
    (1, 1, 1, "In the beginning God created the heaven and the earth."),
    (43, 3, 16, None),
    (66, 22, 21, None),
]
for book, chapter, verse, expected in checks:
    row = db.execute("SELECT text FROM verses WHERE book_id=? AND chapter=? AND number=?", (book, chapter, verse)).fetchone()
    if not row or not row[0].strip():
        raise SystemExit(f"Missing test verse {book}:{chapter}:{verse}")
    if expected and row[0] != expected:
        raise SystemExit("Genesis 1:1 text did not match the expected KJV text")
print(f"Verified KJV dataset: {books} books and {count} verses.")
db.close()
PY
