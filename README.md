# Personal Finance (Android)

Offline personal finance manager: income/expense tracking with payment methods, categories, search/filter/sort,
calendar, monthly/yearly reports with interactive charts, loans & partial repayments, PIN + biometric lock,
Bengali/English, light/dark, selectable currency (default ৳ BDT), JSON backup/restore, CSV export.

* Kotlin, Jetpack Compose (Material 3), Room (KSP), Navigation-Compose, AndroidX Biometric. No internet permission, no analytics, no ads.
* minSdk 26 (Android 8.0), targetSdk/compileSdk 36 (Android 16). Portrait + landscape + tablets/foldables.
* Build in the cloud: see START_HERE.md. Build locally: open this folder in Android Studio and press Run, or `gradlew assembleDebug`.
* Release/signing/Play: see PUBLISHING.md.

Structure: `data/` (Room entities, DAO, migrations, backup), `util/` (formatting, settings, PIN lock, biometrics), `ui/` (screens, charts, theme, navigation).
