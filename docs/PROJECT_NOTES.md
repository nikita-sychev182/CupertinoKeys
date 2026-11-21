# Cupertino Keys - implementation notes

Early development fork of [HeliBorg/HeliBoard](https://github.com/HeliBorg/HeliBoard),
based on the original **v4.2** commit `0c60dffb03a4e2e7a37cb82210f5299f93c03979`.
The working branch is `cupertino`; `upstream` points at the original repository.
This is an independent project, not an Apple or HeliBoard release. Changes in this fork are AI-assisted.
Original licenses and attribution are retained in [the upstream documentation](../README.upstream.md) and license files.

The current prototype adds iOS 26-inspired light/dark materials, rounded keycaps,
Inter typography, a wider spacebar, a simpler bottom row, RU/EN setup, and horizontal
spacebar swipes to switch languages. A comma key is available beside the spacebar in RU/EN;
holding it inserts a period without inserting a comma.
Inter is bundled under the SIL Open Font License;
original vector icons are used instead of Apple assets.
It is **not a pixel-identical iOS keyboard**: the bottom accessory area, animations,
and touch feel still need refinement. Light EN and dark RU previews were inspected on
Mi 8; see [visual verification](testing/cupertino-visuals.md).

The keyboard uses native background blur on supported Android 12+ devices. A separate,
non-touchable child window covers only the keyboard, leaving the application above
it sharp. Its neutral translucent surface takes its tint from the content actually
drawn underneath; an app with a solid background produces a uniform tint. The system
composites this effect without the keyboard reading or storing application pixels.
When blur is unavailable, floating, or fullscreen, the surface is opaque and neutral.
Flat, slightly translucent keycaps have no individual shadows or bevels. Native blur
and changing background colors were verified on Xiaomi 14 / Android 16; see
[glass and suggestion-row checks](testing/glass-suggestions-2026-11-21.md).
The upper surface corners use an 18 dp radius, with 6 dp of inner space above the content.

On Android 16 with gesture navigation, the Cupertino theme replaces the system IME
caption bar with a bottom-left language globe and bottom-right Google voice button.
Hold the microphone for a small palette with Clipboard and Hide actions. In password/no-learning fields it becomes
a hide button. The globe cycles enabled languages; long-press opens Android's keyboard picker. The system
gesture bar stays visible. Older versions, floating/fullscreen modes, and devices that
do not allow caption-bar replacement retain the system panel. See the
[Xiaomi 14 regression checks](testing/xiaomi14-2026-11-21.md).
The latest [reference-style and emoji checks](testing/ios-reference-2026-11-21.md)
cover flatter keycaps, reduced height, the voice button, and themed emoji categories.

With the Android 16 bottom panel, empty suggestion content is hidden while its height
is reserved. Candidate updates and word deletion therefore do not move the application's
message composer. Clipboard access is in the microphone's long-press palette,
including with suggestions and emoji open. The duplicate beside the suggestions is
hidden when this bottom panel is available. Explicit tools, app-provided
suggestions, emoji categories, and clipboard controls remain accessible. Older system
panels retain the existing row so its clipboard button is not lost.
The Cupertino Enter/action key has no long-press popup menu; emoji use their dedicated
outlined smile button. Normal Enter/Send/Next actions remain controlled by the application.
See [stable-height and Enter checks](testing/stable-suggestions-2026-11-21.md).

Voice input now uses Google's public SpeechRecognizer service with a compact 80 dp
popup above the microphone, preserving keyboard height and the current IME. RECORD_AUDIO
is requested at runtime after a microphone tap; returning from the permission prompt
does not automatically start recording. No audio is stored by Cupertino Keys. Google
may send audio to its servers; this is separate from the local dictionary and is not
an offline guarantee. If no supported public Google recognition service is available,
the enabled external Google Voice Typing keyboard remains the fallback. The published
0.1.0 release predates this compact panel and uses only the external voice keyboard.

Privacy changes:
- No internet permission, shared Android dictionary permissions, or contacts access.
- Personal dictionary uses a non-exported app-private SQLite provider in `noBackupFilesDir`.
- The existing on-device prediction/learning engine and bundled RU/EN dictionaries are retained.
- Cloud backup and device-transfer exclusions cover both credential and device-protected storage.
- Gesture sample collection and its sharing provider are disabled; input diagnostics and crash-log export are disabled.
- Clipboard history is off by default. Password/no-learning field handling is inherited from HeliBoard.
- No existing system dictionary is imported automatically. Uninstalling erases app-private learned data.

Sound, vibration, and clipboard-history switches are available directly on the main
settings screen. History is opt-in; it supports multiple text entries (five-entry
copy/paste verified on Xiaomi 14), with a default retention of one hour. Hold an entry
to pin it. On the Cupertino bottom panel, hold the microphone and select Clipboard;
older layouts retain their pinned clipboard button and hold-to-paste shortcut. File collection defaults to off. History
does not collect in password/no-learning/incognito fields or save clips explicitly
marked sensitive. Existing customized toolbar and retention settings are preserved.
Tapping the clipboard button while history is off offers to enable it. The settings
home explains when Do Not Disturb or silent mode suppresses feedback and exposes the
separate option for vibration during Do Not Disturb.

Android's red spelling underlines are controlled by the selected system spell checker,
independently of the active keyboard. Select **Cupertino Keys** in Android's spelling
settings to use this fork's local checker, which follows the keyboard language. Its
previous inherited name was **HeliBoard SpellChecker**. App-specific proofreaders may
apply their own rules.

The keyboard necessarily sends committed text to the application being typed into.
Manual export features inherited from HeliBoard are user-initiated; the keyboard
does not upload its dictionary. Rooted-device tools and vendor migration utilities
must be evaluated separately from Android's documented backup APIs.

## Build and verify

The application ID is `dev.cupertinokeys.keyboard` (`.debug` for development builds),
so it can coexist with HeliBoard. The display name is **Cupertino Keys**.

Use Android Studio with JDK 21 (required by Robolectric for API 36 tests) and the SDK/NDK versions pinned by upstream
(compile/target SDK 37, min SDK 21, NDK 28.0.13004108). Then:

```powershell
python tools/check_privacy.py
.\gradlew.bat :app:testDebugUnitTest --tests '*LocalDictionaryProviderTest' --tests '*CupertinoDefaultsTest' --tests '*InputPrivacyTest' --tests '*CupertinoBlurBackgroundTest' --tests '*SpellCheckerSessionTest'
.\gradlew.bat :app:assembleDebugNoMinify
python tools/check_privacy.py --merged
```

The `Cupertino Keys` GitHub Actions workflow is configured to run these checks and upload a development APK.
Tests cover dictionary persistence, locale separation, duplicate handling, private URIs,
first-run language defaults, changes to no-learning flags on API 29 and 36, and Android 16
window geometry/blur boundaries, and punctuation-only spell-check requests. All 24 current cases passed locally on JDK 21;
native child-window boundaries and suggestion-row transitions are checked on-device.
Source checks alone do not establish runtime behavior.

On Windows, paths containing spaces or `!` require special handling for the upstream
NDK scripts. `tools/build-local.ps1 -Task Build` (or `-Task Test`) uses a checked `K:`
mapping and an ignored `.tools` SDK/JDK/Gradle installation. It expects JDK 21 under
`.tools/jdk`, the pinned Gradle distribution under `.tools/gradle`, and the SDK under
`.tools/android-sdk`. It does not download tools or change system-wide environment variables.
The APK is written to `app/build/intermediates/apk/debugNoMinify/`.
The local build script disables `testOnly` so this development APK can also be
installed by opening the file on a phone.

## Personal and work profiles

Install and select Cupertino Keys in the personal profile. Standard Android work
profiles share the personal profile's input method, so the same keyboard settings
and app-private personal dictionary are expected in both personal and work apps.
Change settings and edit the dictionary through the personal-profile app; a second
installation in the work profile has separate app storage and is not a sync mechanism.
No network or cross-profile data bridge is needed for the shared keyboard.

This follows [Android Enterprise's shared-IME behavior](https://developers.google.com/android/work/requirements?api=playemm#412_ime_management).
An administrator can restrict which keyboards are permitted. Separate Android users
and Xiaomi Second space are outside this shared-IME setup. Xiaomi 14 / HyperOS work
profile behavior still requires the [device checks](DEVICE_TESTING.md#work-profile-on-xiaomi-14).
Password and no-learning fields retain their privacy restrictions in either profile.

Primary device: **Xiaomi 14 / Android 16**. Secondary: **Xiaomi Mi 8 / Android 10 (API 29), MIUI 12**.
See [device test steps](DEVICE_TESTING.md).
