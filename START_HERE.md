# Easiest way to get your Google Play file (.aab)

No Android Studio, no commands, no PWABuilder. You only need a free GitHub account and a web browser.
GitHub builds the app for you on its own computers.

## Step 1: Choose your app ID (2 minutes)

Open `capacitor.config.json` and change this line to something unique to you:

    "appId": "com.yourname.timezonecomparator",

Example: `"appId": "com.juandelacruz.timezones"`.
Use lowercase letters and numbers only, with dots between parts. You can NEVER change it after uploading to Google Play.

## Step 2: Put the project on GitHub

1. Go to github.com, sign up (free) and click **New repository**.
2. Name it `timezone-app`, choose **Private**, click **Create repository**.
3. Click **uploading an existing file**.
4. Unzip this project on your computer, open the folder, select EVERYTHING inside it
   (including the `www`, `resources`, `scripts`, `android-extras` and `store` folders) and drag it into the browser.
5. Click **Commit changes**.

**Check:** in the repository you should see a folder `.github` → `workflows` → `build-aab.yml`.
If `.github` is missing (hidden folders are sometimes skipped when dragging):
click **Add file → Create new file**, type `.github/workflows/build-aab.yml` as the name,
open that same file from the unzipped folder in Notepad, copy everything, paste it, and click **Commit changes**.

## Step 3: Build

1. Click the **Actions** tab. If asked, click **I understand my workflows, go ahead and enable them**.
2. Click **Build Android AAB** on the left, then **Run workflow → Run workflow**.
3. Wait about 5 to 10 minutes. A green tick means success.
4. Open the finished run and scroll to **Artifacts** at the bottom. Download:
   - **app-release-aab**: unzip it to get `app-release.aab`. This is the file for Google Play.
   - **SAVE-THIS-signing-key**: unzip and keep it somewhere safe (cloud drive plus USB).
     It holds your signing key and passwords. The first build creates it for you.

If the build fails (red cross), open the failed step, copy the last 30 lines of the log and send them to me.

## Step 4: Upload to Google Play

Play Console → your app → Testing → Internal testing → Create new release → upload `app-release.aab`.
Keep **Play App Signing** on when asked. The rest of the checklist is in `BUILD_GUIDE.md`.

## Future updates

Edit `www/index.html` in GitHub (pencil icon), then run the workflow again. Each build gets a higher
version number automatically.
To keep using the same signing key, open the file `KEYSTORE-INFO.txt` from the saved download and add the
three repository secrets it lists (Settings → Secrets and variables → Actions → New repository secret).
If you lose the key, Play Console lets you request an upload key reset.

## Test it on your phone first (recommended, no Play needed)

1. In the **Actions** tab, click **Build test APK (install on your phone)** → **Run workflow**.
2. When it turns green, download the **test-apk** artifact and unzip it to get `app-debug.apk`.
3. Send the file to your phone (email, cloud drive or USB) and tap it to install.
   Android will ask you to allow installs from this source; allow it for that one app (Files, Chrome or Drive).
   Play Protect may show a warning because it is a test build. Choose **Install anyway**.
4. Open the app and check:
   - Add an alarm a few minutes ahead, allow notifications, then fully close the app. It should still alert.
   - Tap "Send a test alert in 5 seconds" and lock the phone. It should still arrive.
   - If a banner asks to allow "Alarms & reminders", tap it and switch it on.
   - Add and remove clocks, and drag the hour slider.

The test APK is for you only. Never upload it to Google Play. Use the `.aab` from the other workflow for that.
