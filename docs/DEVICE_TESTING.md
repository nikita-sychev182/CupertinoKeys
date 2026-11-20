# Device checks for Cupertino Keys

Physical-device results: [Mi 8, 2026-11-20](testing/mi8-2026-11-20.md).

## Connect and identify

Enable USB debugging, connect the phone, and accept its USB authorization dialog.
With Android platform-tools on PATH:

```powershell
adb devices -l
adb -s SERIAL shell getprop ro.product.model
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell getprop ro.build.version.sdk
```

Use an explicit serial when both phones are connected. Do not assume the Mi 8's
Android version from its MIUI version.

## Install

Download the development APK artifact from a successful `Cupertino Keys` workflow run,
or build `assembleDebugNoMinify` locally. Then:

```powershell
adb -s SERIAL install -r -t app/build/intermediates/apk/debugNoMinify/CupertinoKeys_0.1.0-debugNoMinify.apk
```

Open Cupertino Keys, enable it in Android's keyboard settings and select it.
Do not test on real passwords or private messages; use disposable text.
GitHub Actions debug signing keys are not persistent between runners, so updating
a CI build can require uninstalling the old development build (which erases its dictionary).
Local builds retain their development signing key under `.tools/android-user-home`.
Release signing remains a release-preparation task.

## Repeatable device smoke test

With the phone unlocked and Cupertino Keys selected manually:

```powershell
python tools/test-device.py --serial SERIAL --adb .tools/android-sdk/platform-tools/adb.exe
```

The debug-only instrumentation dispatches MotionEvents to the actual keyboard View
and verifies committed text in its own disposable Activity. It works without MIUI's
separate USB security toggle. This covers the IME handlers and InputConnection, but
does not test Android's system touch dispatcher or physical finger gestures.
Two process launches verify word persistence; the test word is removed afterward.
The script leaves Cupertino Keys selected and opens the disposable test screen.
Results go to ignored `.tools/test-results/device-*.txt`. The test Activity and runner
are absent from release builds. A system-level input-injection test remains manual
when MIUI blocks shell input commands.

Use `--screenshots` to capture light English and dark Russian previews in the same
disposable screen. This only overrides the keyboard theme in the test process and
restores automatic theme selection afterward. Screenshots and color diagnostics are
saved under `.tools/test-results/`. Check the rendered text contrast visually: MIUI
can recolor Canvas text even when the application passes the correct Paint color.

## Acceptance checks

- Fresh install starts with Russian and English available; switching language does not require network access.
- A tap on space inserts one space. A deliberate left/right swipe changes language without inserting space.
- A small finger movement remains a tap; vertical movement and cancelled gestures do not switch language accidentally.
- Verify backspace repeat, shift/caps lock, punctuation, enter/search/send actions and selected-text replacement.
- Test Telegram or another messenger, browser fields, a multiline notes field, email and numeric fields.
- Compare light/dark appearance, Russian row widths, popup clipping and suggestion labels on both phones.
- Check portrait/landscape, gesture navigation, three-button navigation and increased display/font scaling.
- Add a dummy personal word and shortcut; confirm suggestions, edit/delete them, restart and verify persistence.
- Confirm the same dummy word does not appear in another keyboard's system personal dictionary.
- Confirm password and app-requested no-learning fields do not teach words or show personalized suggestions.
- Reboot with a screen lock: keyboard must work before first unlock and resume normally after unlock.
- Inspect installed permissions and merged manifest; test Android backup/restore and Xiaomi transfer behavior before claiming device-level backup isolation.

Record OS build, APK commit, input application, reproduction steps, and a screenshot
containing only disposable text for each issue. No raw typing logs are required.
