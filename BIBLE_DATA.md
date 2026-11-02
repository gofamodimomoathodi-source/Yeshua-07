# Complete Bible data

The build workflow downloads the King James Version SQLite dataset from:

https://github.com/midvash/bible-data

Source file:
https://github.com/midvash/bible-data/raw/main/versions/en/kjv/kjv.sqlite

The dataset's metadata identifies the KJV as public domain and reports 66 books, 1,189 chapters, and 31,102 verses. The workflow validates the database schema, expected counts, and representative verses before building. Review the upstream `SOURCES.md` and `versions/en/kjv/metadata.json` for current provenance and licensing details before commercial distribution.

The database is downloaded during GitHub Actions rather than committed as a binary file. This keeps the repository smaller. The generated APK contains the SQLite database as an asset so Scripture reading works offline after installation. The app uses the database table `verses` with columns `book_id`, `chapter`, `number`, and `text`. Numeric book IDs 1–66 follow the standard Protestant canon order.

This is KJV text data only. It does not include original-language alignment or a complete Greek/Hebrew lexicon.


## Additional translations (NKJV, LSB, AMP)
The app UI and database loader support these translation slots, but their text is not included because these versions are copyrighted. Obtain an authorized database/license from the relevant rights holder or provider, then place the SQLite file in `app/src/main/assets/` as `nkjv.sqlite`, `lsb.sqlite`, or `amp.sqlite`. The database should have a `verses` table with columns `book_id`, `chapter`, `number`, and `text`, with book IDs 1–66 in standard Protestant Bible order. The Gradle Android asset system packages files present in this directory into the APK. The user interface will report when a selected translation database is missing.
