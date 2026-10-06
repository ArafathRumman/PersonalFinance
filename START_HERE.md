# Personal Finance – START HERE (no programming knowledge needed)

> Prefer Android Studio on your computer? Read **START_HERE_BANGLA.txt** (step-by-step in Bangla).

You will use a free website called **GitHub** to turn this project into an installable **APK** app file.
You do not need Android Studio. Plan for about 30 minutes the first time.

## Part 1 – Put the project on GitHub

1. Go to **github.com** and create a free account (or sign in).
2. Click the **+** at the top right → **New repository**.
3. Repository name: `PersonalFinance`. Choose **Private** (important – only you can see it). Click **Create repository**.
4. On your computer, **unzip** `PersonalFinance-Source.zip`. You get a folder with files like `app`, `gradle`, `START_HERE.md`.
5. On the new empty repository page click the link **"uploading an existing file"**.
6. Open the unzipped folder, press **Ctrl+A** (select everything inside it) and **drag it into the browser page**. Wait until every file finishes uploading.
   *Tip: GitHub accepts up to 100 files per upload. If it complains, upload the `app` folder first, then everything else.*
7. Scroll down and click the green **Commit changes** button.
8. Click the **Code** tab. Check that you can see a folder called **`.github`**.
   - If you see it → go to Part 2.
   - If you do NOT see it (some computers hide it): click **Add file → Create new file**, type exactly
     `.github/workflows/build-apk.yml` as the file name, open `HOW_TO_ADD_WORKFLOW_MANUALLY/build-apk.yml.txt` from the unzipped folder, copy ALL the text, paste it, and click **Commit changes**.

## Part 2 – Build the app

1. Click the **Actions** tab. If GitHub shows a green button *"I understand my workflows, go ahead and enable them"*, click it.
2. Click **Build Android APK and AAB** on the left, then **Run workflow** (right side) → **Run workflow**.
   (It also starts by itself every time you upload files.)
3. Wait about **8–15 minutes**. A yellow circle means "working", a green ✔ means success, a red ✖ means a problem.
4. **If it is red ✖:** click the run, click the red step, copy the red error text and send it to Claude. It will be fixed.

## Part 3 – Download and install on your Samsung phone

1. Open the **Code** tab → on the right side click **Releases** → the newest one.
2. Under **Assets** tap/click the file ending in **.apk**:
   - `PersonalFinance.apk` – signed with your own key (see PUBLISHING.md). Use this for sharing.
   - `PersonalFinance-TEST-ONLY.apk` – a test version. Fine for trying the app on your own phone, but do not share it widely.
3. Easiest: open this same Releases page **in your phone's browser**, log in to GitHub, and tap the APK to download it.
   (Or download on a computer and send it to your phone with Google Drive, Quick Share, or a USB cable.)
4. On the phone tap the downloaded file → if Samsung says **"For your security, your phone is not allowed to install unknown apps from this source"**, tap **Settings**, switch on **Allow from this source**, press back, then tap **Install**.
5. Open **Personal Finance**. Done!

## Part 4 – Share the app with other people

- Send them the **`PersonalFinance.apk`** file (Google Drive link, Telegram, email, USB, Quick Share; some apps such as WhatsApp may refuse .apk files – put it in a zip or use a Drive link).
- They install it the same way as step 4 above (allow "unknown apps" once).
- To publish on **Google Play** you need the `.aab` file – read **PUBLISHING.md**.

## Updating the app later

Upload the changed files to GitHub again. A new build starts. Because every build is signed with the same key and has a higher version number, the new APK installs **over** the old one and **keeps all the user's data**.

## Keeping your data safe

Inside the app: **Settings → Create backup** saves a file you can keep in Google Drive. **Settings → Restore backup** brings everything back (also on a different phone).
