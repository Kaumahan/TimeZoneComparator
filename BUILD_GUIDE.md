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
npm run setup-native    # widget + alarm engine + calendar bridge
npm run assets          # generates launcher icons + splash from /resources
npx cap sync android
npx cap open android
```
In Android Studio: Build → Generate Signed App Bundle / APK → Android App Bundle → create or choose your keystore → release.
Output: `android/app/release/app-release.aab`.

After editing `www/index.html`, run `npx cap sync android` and rebuild.

---

## Native features (widget, alarm engine, Google Calendar)

`scripts/android-native.sh` (run by both GitHub workflows, or `npm run setup-native` locally) copies the Java code
from `android-extras/` into the generated Android project and registers it in the manifest and `MainActivity`.
Nothing here uses the network: everything runs on the phone.

If a build ever fails with Java or resource errors from one of these, switch that feature off without touching code:
in GitHub go to Settings → Secrets and variables → Actions → **Variables** → New repository variable, then add
`DISABLE_WIDGET`, `DISABLE_ALARM` or `DISABLE_CALENDAR` with the value `true`, and run the build again.
(Without the alarm engine the app falls back to ordinary scheduled notifications.) Send me the error text.

### Alarms

* **Engine:** alarms are scheduled with Android's alarm-clock API (`AlarmManager.setAlarmClock`, the same one the
  Clock app uses). They fire on time even in Doze mode and need no "exact alarm" permission. When one fires the app
  shows a loud alarm notification on the phone's **Alarm volume**, repeating until you dismiss it (or 2 minutes).
* **Survives restarts and updates:** the schedule is saved on the phone and re-created after a reboot **and after the
  app is updated** (Android clears alarms on update). It is also refreshed whenever you open the app.
* **Sounds:** six 24-second tones in `www/sounds/tone_*.wav`: Classic bell, Digital beeps, Melody, Siren, Chime, Pulse.
  Pick one per alarm (with a Preview button).
* **Up to about 300 upcoming alerts** are scheduled in total (a Mon–Fri alarm covers roughly 10 weeks ahead).
* **Alarm status panel** (Alarms section → "Alarm status & help"): shows which engine is active, whether notifications are
  allowed, how many alerts are scheduled and the next one, and whether battery optimisation could delay alarms.
  It has shortcuts to the app settings and battery settings. Use it first if an alarm does not ring.
* **If alarms are late or silent:** (1) raise the Alarm volume in Settings → Sound; (2) allow notifications for the
  app; (3) set the app's battery usage to **Unrestricted** and allow auto-start (Xiaomi, Oppo, Vivo, Samsung and Huawei
  are strict); (4) Do Not Disturb can silence alarms.
* Permissions added by `scripts/android-setup.sh`: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`,
  `WAKE_LOCK`, and `SCHEDULE_EXACT_ALARM` (Android 12 only, used by the fallback path).
* In the website / PWABuilder version, alerts only ring while the page is open.

### Google Calendar

* **Show my meetings** lists the next 7 days of events from the calendars on the phone. Google Calendar events
  appear when your Google account is added to the phone and calendar sync is on. Each event shows its time in your
  clocks, and **Alarm** opens a pre-filled alarm for it. All-day events are shown but cannot have an alarm.
* The calendar icon on an alarm opens your calendar app (normally Google Calendar) with a new event filled in.
  It needs no permission.
* Uses the `READ_CALENDAR` permission, asked only when you tap **Show my meetings**. Events are read on the phone and
  never sent anywhere.
* **Play Console:** in the Data safety form, calendar data is read on the device only and is not collected or shared.
  Keep the privacy policy's calendar section.

### Meeting finder, work hours, daylight saving (all in `www/index.html`, no native code)

* **Find a meeting time:** pick a date and up to 5 clocks. A bar per person shows who is at work (green), awake (amber) or
  asleep (dark) in 30-minute steps, and the best windows are listed in every person's own time. A window may run past your
  midnight. Each window has **Alarm**, **Calendar** and **Share** buttons. If nobody can meet without someone being up at
  night it lists the best compromise.
* **Work hours:** tap the badge on any clock to set its own hours and work days (for example Sunday to Thursday), or use
  the same hours for every clock. The badge, the widget and the meeting finder all follow them.
* **Daylight saving:** a clock shows a warning when its offset changes within 14 days, and the meeting finder warns when a
  chosen date is near a change. Each clock also shows its offset from the Manila clock (for example "−12h vs PHT").
* **Every city:** search accepts any time zone name (about 400), and the alarm form lists them all.
* **12h / 24h:** the toggle in the header also changes the widget and alarm texts.
* **Share:** uses the phone's share sheet when available, otherwise copies the text.

### Alarm buttons

When an alarm rings, the notification has **Snooze 5 min** and **Dismiss** buttons. Dismissing stops the sound at once.
A snoozed alarm rings again after 5 minutes (it is not restored after a restart).

### Home screen widget

* **Look:** the first clock is shown large (time, AM/PM, date, city), the others in compact rows below. Each clock has a
  sun or moon icon and a coloured dot: green = working, red = off hours, grey = day off (using your work hours).
* **Sizes:** resize it freely. Short widgets switch to a compact two-clock layout, taller ones show up to 4 clocks.
* In the app tap **Add widget**, or press and hold an empty spot on the home screen → Widgets → **Time Zone Clocks**.
  Choose which clocks appear with the **Widget** button on each clock. Tapping the widget opens the app.
* The times tick by themselves. The dots and icons refresh every 15 minutes and whenever you change something.

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
