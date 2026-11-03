# ScreenKeeper

Android app that keeps the phone screen on indefinitely, over any app, whether or not the phone is charging. Built for sideloading (target device: Samsung Galaxy S24, Android 14+; minimum Android 10).

## How it works

- A foreground service adds an invisible 1x1 overlay window with `FLAG_KEEP_SCREEN_ON`. While that window exists, the display does not time out, whichever app is in front.
- Brightness is not changed; the screen stays at your normal brightness.
- A persistent notification is shown while it is on (Android requirement for foreground services).

## Features

- On/off toggle in the app
- Quick Settings tile ("Keep screen on")
- Optional auto-start after reboot (only if it was on before the reboot)
- Automatically restarts after the app is updated, if it was on

## Install (sideload)

1. Get `dist/ScreenKeeper-debug.apk` onto the phone (download from GitHub on the phone, or copy over USB).
2. Open it and allow your browser/file manager to "Install unknown apps" when prompted.
   - Alternatively, with USB debugging enabled: `adb install -r dist/ScreenKeeper-debug.apk`
3. Open ScreenKeeper and grant:
   - **Display over other apps** (required)
   - **Notifications** (recommended)
   - **Battery: Unrestricted** (recommended, so Samsung does not kill the service)
4. Tap **Turn on**.
5. Optional: pull down Quick Settings, tap the pencil/edit button and drag the **Keep screen on** tile in.

### Samsung-specific tips

- Settings > Battery > Background usage limits: make sure ScreenKeeper is not in "Sleeping apps" or "Deep sleeping apps"; add it to "Never sleeping apps".
- If Play Protect warns about an unknown app, choose "Install anyway" (the app is unsigned by Play).

## Limitations

- Pressing the power button still turns the screen off. Unlock the phone and it is kept on again.
- The lock screen uses its own short timeout; the overlay is hidden behind the lock screen.
- While some system screens are open (e.g. permission dialogs, the package installer), Android hides third-party overlays, so the normal timeout can apply on those screens.
- AMOLED burn-in risk increases with long static images on screen.

## Build

Requirements: JDK 17+ and the Android SDK (platform 35, build-tools 34+).

```bash
# Install the Android SDK command-line tools, then:
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Debug builds are signed with the committed `keystore/debug.keystore` (password `android`), so newer builds install over older ones without uninstalling. This key is public; do not use it for anything other than debug builds.
