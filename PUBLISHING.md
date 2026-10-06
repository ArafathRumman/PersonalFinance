# Publishing checklist (signing key, Google Play, version numbers)

## 1. Why you need your own signing key
Android only installs apps that are digitally signed. The signature is also how Android knows a new version comes from the same author. **I did not create any key or password for you**: you choose the password, so only you can sign your app.

Until you add your key, the build still works but names the files `...TEST-ONLY...` (signed with a temporary test key). Never publish those.

## 2. Create your key (one time, no tools needed)
1. In your GitHub repository click **Actions → "Create my signing key (run ONCE)" → Run workflow**.
2. Type a password of your own (8+ letters/numbers, no spaces) – **write it down on paper** – and your name. Click **Run workflow**.
3. When it turns green, open the run and download the artifact **YOUR-SIGNING-KEY-keep-private** (a zip). Unzip it: you get `release.keystore`, `KEYSTORE_BASE64.txt`, `READ_ME.txt`.
4. **Back up `release.keystore` and your password in at least two safe places** (not on GitHub). If you lose them you can never update your app for the people who installed it.
5. In your repository open **Settings → Secrets and variables → Actions → New repository secret** and add 4 secrets:

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | everything inside `KEYSTORE_BASE64.txt` |
| `KEYSTORE_PASSWORD` | the password you typed |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | the same password you typed |

6. Delete the downloaded artifact from the run page (trash icon) and make sure the repository stays **Private**.
7. Run **Build Android APK and AAB** again. Now the files are named `PersonalFinance.apk` and `PersonalFinance.aab` and are signed with your key.

(Advanced: you can build on your own computer by copying `keystore.properties.example` to `keystore.properties`, filling in your own values, and running `gradlew assembleRelease bundleRelease`.)

## 3. Google Play
1. Create a Google Play Console developer account (one-time US$25 fee, paid to Google).
2. **Application ID:** open `app/build.gradle.kts` and set `applicationId` to something unique to you, e.g. `com.yourname.personalfinance`. Do this **before the first upload** – it can never be changed afterwards.
3. Create the app in Play Console and upload `PersonalFinance.aab` (not the APK). Accept "Play App Signing".
4. Play requires a privacy policy link and the "Data safety" form. Answer: **no data collected, no data shared**. The in-app Privacy text in Settings can be used as the base of your policy.
5. Version numbers: `versionCode` is set automatically from the GitHub run number, so each new build is higher than the last (required by Play). `versionName` (what users see) can be changed in `app/build.gradle.kts` ("1.0.0").

## 4. Changing the database in future versions
Never delete user data. If you add fields later, follow the instructions at the top of `app/src/main/java/com/taka/personalfinance/data/Migrations.kt`.
