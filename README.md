# YHWH Bible — Android starter

A Kotlin + Jetpack Compose starter for the YHWH Bible study app.

## Current prototype features
- Home screen and bottom navigation
- Translation selector for KJV, NKJV, LSB, and AMP; KJV is included by the standard build, while NKJV/LSB/AMP require authorized text databases
- Example Greek/Hebrew word-study entries (not a complete lexicon)
- PDF import from Android document picker
- Native Android PDF rendering, page navigation, and basic zoom
- Demo profile screen (not real authentication)
- GitHub Actions workflow to build a debug APK artifact

## Build in Android Studio
1. Open this folder in a recent Android Studio version.
2. Let Gradle sync.
3. Select the `app` run configuration and run on an Android phone or emulator.

The GitHub Actions workflow uses Gradle 8.9, JDK 17, Android SDK platform 35, and builds `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub manual upload
Upload the *contents* of this folder to the root of your repository, preserving the folder structure. Ensure hidden folders such as `.github` are included.

Then open the repository's **Actions** tab, select **Build YHWH Android APK**, and run the workflow if it has not started automatically. When successful, open the workflow run and download the `YHWH-debug-apk` artifact. GitHub gives you a ZIP containing the APK.

## Prototype limitations
This is not a production-ready Bible application. It does not include a complete Bible database, word-level original-language alignment, a real account system, cloud sync, PDF text search/selection, annotations, or release signing. The Scripture and lexicon examples are illustrative. Only bundle Bible and lexicon resources whose licences permit redistribution. KJV copyright status may vary by jurisdiction.

Do not store signing keys, passwords, or private API credentials in this repository.


## Complete 66-book KJV dataset
The Bible screen is wired to a SQLite database with all 66 books (31,102 verses). GitHub Actions downloads and verifies the KJV dataset during the build, then packages it into the APK for offline reading. See `BIBLE_DATA.md` and `scripts/download_bible.sh` for source and validation details. The first build requires internet access in GitHub Actions.


## Bible translation support
The Bible reader includes a translation selector for KJV, NKJV, LSB, and AMP. The automated build currently downloads and verifies the KJV database. The other translations are copyrighted and are not bundled in this starter. To enable one, obtain an authorized database and place it in `app/src/main/assets/` using the matching filename: `nkjv.sqlite`, `lsb.sqlite`, or `amp.sqlite`. Each database must use the same `verses` table schema as the KJV database (`book_id`, `chapter`, `number`, `text`) and standard book IDs 1–66. The app checks for the selected database and shows a clear message when it is not installed. Do not redistribute licensed text unless your license permits it.
