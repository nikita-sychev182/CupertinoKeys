# Device checks for Cupertino Keys

Physical-device results: [Mi 8, 2026-11-20](testing/mi8-2026-11-20.md).
Android 16 regression results: [Xiaomi 14, 2026-11-21](testing/xiaomi14-2026-11-21.md).

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

Use `--phase emoji_dark` and `--phase emoji_light` to inspect the emoji category strip
in both themes. Each phase saves a screenshot and checks that the strip reveals the
themed keyboard background instead of inheriting a static light XML background.

Use `--phase voice` to check discovery of enabled Google Voice Typing and suppression
in password/no-learning fields. This does not start recording. Manually tap the
bottom-right microphone in an ordinary text field, dictate disposable text, and check
insertion and return to Cupertino Keys. Hold the microphone to hide the keyboard.
Repeat actual dictation in a Shelter app separately; the automated phase does not
verify external Google behavior or work-profile voice policy.

Use `--phase clipboard` for a five-entry copy/paste test and feedback-setting checks.
It refuses to run if the existing system clip contains a URI or intent that cannot
be reliably restored. The test temporarily enables history, sound, and vibration,
copies disposable text through Android's clipboard, opens the pinned history button,
and inserts each entry through its visible list item. It also checks sensitive clips,
password/no-learning fields, and disabling collection. It restores preferences and
the original text clipboard and deletes its own entries. Audible/tactile feedback
still needs a human check; silent and Do Not Disturb modes remain respected.

## Work profile on Xiaomi 14

Status: the user confirmed the combined manual personal/Shelter check passed;
HyperOS denies shell access to user 11, so work-profile behavior was not independently
verified through ADB.
Standard Android shares the personal
profile's IME with work apps; this is not a copy/sync of two installations.

1. Install the local APK in the personal profile, enable Cupertino Keys, and select it.
   Builds from `tools/build-local.ps1 -Task Build` can be opened in the file manager
   without ADB because `testOnly` is disabled.
2. In the personal-profile settings, change keyboard height and add a disposable
   English dictionary entry `cupertinoworktest` with shortcut `cwt`.
3. Open ordinary text fields in personal and work apps. Confirm both show Cupertino
   Keys with the same height and RU/EN languages. Check that `cwt` offers the test
   word in both, with English selected and suggestions enabled.
4. Open settings from the keyboard while in a work app, change the height again,
   and verify it carries back to a personal app. Delete the test dictionary entry
   and confirm it disappears in both profiles after reopening the keyboard.
5. Pause/resume the work profile and restart the phone; repeat the appearance and
   dictionary checks. Password/no-learning fields must still suppress personalized
   suggestions and learning.

Do not clone/install a second copy merely to synchronize settings: its storage is
separate. If the work app cannot use Cupertino Keys, check the organization's
keyboard allowlist. If HyperOS actually uses separate IME instances, record the OS
build and profile type before implementing a supported local bridge; this build
does not claim to synchronize independent Android users or Xiaomi Second space.

For Android 16 regression checks use `tools/test-device.py --phase geometry` and
`--phase navigation` with the same `--serial` / `--adb` options. These verify reported
IME insets and dispatch touches through our own decor view to the bottom buttons;
they do not bypass the OS input-injection restriction. `--phase work_setup` adds
`cupertinotestword` / `ctw` in the personal IME for a manual work-profile check;
`--phase work_cleanup` removes that disposable entry afterward.

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
