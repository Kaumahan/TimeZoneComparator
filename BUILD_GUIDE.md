# Time Zone Comparator – Android build & Google Play guide

> **New to this? Read `START_HERE.md` first.** It is the simplest route (GitHub builds the file for you, and it creates the signing key automatically).


This project wraps your web app (`www/index.html`) in a native Android shell using Capacitor.
The app is fully offline: no internet permission, no tracking, no accounts.

> I could not compile the `.aab` where this project was generated (no Android SDK / internet),
> so the build has not been run end-to-end. The steps below are the standard Capacitor flow.
> If a step fails, paste the error and I'll fix it.

## 0. Do this first: pick your permanent app ID

Open `capacitor.config.json` and change `appId` from `com.yourname.timezonecomparator`
to something you own, e.g. `com.juandelacruz.timezonecomparator`.
**It can never be changed after you upload to Google Play.**

---

## Option A – Build in the cloud (no Android Studio needed)

1. Create a free GitHub account and a new **private** repository.
2. Upload everything in this folder to it (drag and drop in the browser works).
3. Create your signing key (needs Java installed; one time only):

   ```bash
   keytool -genkey -v -keystore release.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 release.jks > keystore.b64        # macOS: base64 -i release.jks -o keystore.b64
   ```
   **Back up `release.jks` and its passwords somewhere safe (not in the repo).**

4. In the repo go to Settings → Secrets and variables → Actions → New repository secret and add:
   - `KEYSTORE_BASE64` – the contents of `keystore.b64`
   - `KEYSTORE_PASSWORD` – the keystore password
   - `KEY_ALIAS` – `upload`
   - `KEY_PASSWORD` – the key password

5. Go to the Actions tab → "Build signed AAB" → Run workflow.
6. When it finishes, download the `app-release-aab` artifact. Unzip it: `app-release.aab` is the file you upload.

Each run bumps `versionCode` automatically, so every new build is accepted by Google Play.

## Option B – Build locally with Android Studio

Requires Node 20+, Android Studio, JDK 21.

```bash
npm install
npx cap add android
npm run setup-android   # notification icon + alarm permissions
npm run assets          # generates launcher icons + splash from /resources
npx cap sync android
npx cap open android
```
In Android Studio: Build → Generate Signed App Bundle / APK → Android App Bundle → create or choose your keystore → release.
Output: `android/app/release/app-release.aab`.

After editing `www/index.html`, run `npx cap sync android` and rebuild.

---

## Alarms & notifications (new)

The app can schedule meeting alarms in any time zone. In the Android app they are scheduled with the
phone's alarm system, so they fire even when the app is closed (and are re-scheduled after a reboot).
They are also refreshed every time you open the app, so open it at least every couple of weeks.

* Permissions added by `scripts/android-setup.sh`: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`,
  `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM` (Android 12 only) and `USE_EXACT_ALARM` (granted automatically on Android 13+).
* Alerts use one of six loud 24-second alarm tones (`www/sounds/tone_*.wav`): Classic bell, Digital beeps, Melody,
  Siren, Chime, Pulse. Pick one per alarm in the app (with a Preview button). Each tone is its own notification
  channel, so you can also swap any of them for a phone ringtone in Android Settings → Apps → this app → Notifications.
* Alerts are played on the phone's **Alarm volume** (not the notification volume). Turn it up in Settings → Sound.
  If your phone plays them quietly, tell me the phone model.
* Up to about 400 upcoming alerts are scheduled in total (for one weekday alarm that is roughly 10 weeks ahead),
  and the list is refreshed every time the app opens.
* On Android 12+ the user may need to allow "Alarms & reminders" for the app so alerts arrive on the exact minute.
  The app shows a banner with a shortcut to that setting if it is not allowed.
* Alerts are high-priority notifications with sound and vibration. They do not take over the screen like the
  built-in Clock app, and they follow the phone's silent / Do Not Disturb settings.
* In the website / PWABuilder version, alerts only ring while the page is open.

**Play Console:** exact-alarm permissions are restricted. `USE_EXACT_ALARM` is meant for alarm-clock and calendar
apps; if Google objects, delete the `add_perm USE_EXACT_ALARM` line in `scripts/android-setup.sh` and rebuild
(users will then be asked to allow "Alarms & reminders" once). In App content you may be asked to declare why the
app uses them. Answer honestly: it schedules user-created meeting reminders / alarms.

## Google Play Console checklist

**Account:** a developer account costs a one-time US$25. Personal accounts created recently
must run a closed test with at least 12 testers for 14 continuous days before you can apply
for production access. Check Google's current rules in the Console, they change.

1. Create app → name "Time Zone Comparator", language, App (not game), Free.
2. Opt in to **Play App Signing** when uploading the first bundle (recommended, default).
3. Testing → Internal testing → Create release → upload `app-release.aab` → add yourself as a tester → roll out. Test on a real phone.
4. Main store listing: copy text from `store/listing.md`, upload `store/play-icon-512.png`,
   `store/feature-graphic-1024x500.png`, and at least 2 phone screenshots (take them from the installed app).
5. App content:
   - Privacy policy URL: host `PRIVACY_POLICY.md` (e.g. GitHub Pages or a Gist) and paste the link.
   - Data safety: **No data collected, no data shared**.
   - Ads: No. Content rating questionnaire: answer No to everything → Everyone.
   - Target audience: 18+ (or 13+; not children).
   - Government app / financial features / health: No.
6. Closed testing (if required) → then Production → Create release → submit for review.

## Target API level

Google requires new apps and updates to target a recent Android API level, and the bar rises every year.
Because `package.json` uses `latest`, Capacitor will generate a project that targets the current level.
If the Console warns about it, run `npm install @capacitor/android@latest @capacitor/core@latest`
and rebuild.

## Files

| Path | Purpose |
|---|---|
| `www/index.html` | The app itself (offline, no external requests) |
| `capacitor.config.json` | App ID, name, colors |
| `resources/` | Icon & splash sources used by `capacitor-assets` |
| `store/` | Play listing graphics + copy |
| `.github/workflows/build-aab.yml` | Cloud build that outputs the signed `.aab` |
| `PRIVACY_POLICY.md` | Privacy policy to host and link in the Console |
