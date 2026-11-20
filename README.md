# Cupertino Keys — Android keyboard

Early development fork of [HeliBorg/HeliBoard](https://github.com/HeliBorg/HeliBoard),
based on the original **v4.2** commit `0c60dffb03a4e2e7a37cb82210f5299f93c03979`.
The working branch is `cupertino`; `upstream` points at the original repository.
This is an independent project, not an Apple or HeliBoard release. Changes in this fork are AI-assisted.
Original licenses and attribution are retained below and in the license files.

The current prototype adds iOS 26-inspired light/dark materials, rounded keycaps,
Inter typography, a wider spacebar, a simpler bottom row, RU/EN setup, and horizontal
spacebar swipes to switch languages. A comma key is available beside the spacebar in RU/EN.
Inter is bundled under the SIL Open Font License;
original vector icons are used instead of Apple assets.
It is **not a pixel-identical iOS keyboard**: the bottom accessory area, animations,
and touch feel still need refinement. Light EN and dark RU previews were inspected on
Mi 8; see [visual verification](docs/testing/cupertino-visuals.md).

The keyboard uses a locally drawn frosted surface. Xiaomi 14 testing exposed that
Android's native window blur covers the full IME window, including the application
above the keys; a background outline does not restrict that region. Native blur is
therefore allowed only if the keyboard occupies the entire window, which the normal
full-height IME popup layout does not. No application screenshots are captured or stored.
Keycaps use flat, slightly translucent fills over this surface, without individual
shadows or bevels. See the latest [material comparison](docs/testing/flat-material-2026-11-21.md).

On Android 16 with gesture navigation, the Cupertino theme replaces the system IME
caption bar with a bottom-left language globe and bottom-right Google voice button.
Hold the microphone to hide the keyboard. In password/no-learning fields it becomes
a hide button. The globe cycles enabled languages; long-press opens Android's keyboard picker. The system
gesture bar stays visible. Older versions, floating/fullscreen modes, and devices that
do not allow caption-bar replacement retain the system panel. See the
[Xiaomi 14 regression checks](docs/testing/xiaomi14-2026-11-21.md).
The latest [reference-style and emoji checks](docs/testing/ios-reference-2026-11-21.md)
cover flatter keycaps, reduced height, the voice button, and themed emoji categories.

Voice input hands control to an enabled Google Voice Typing keyboard. Cupertino Keys
does not record audio or request microphone permission. Google may send audio to its
servers under its own settings; this optional external service is separate from the
local dictionary and is not an offline recognition guarantee.

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
to pin it. The pinned clipboard button beside suggestions opens history; hold the
button to paste the current system clip. File collection defaults to off. History
does not collect in password/no-learning/incognito fields or save clips explicitly
marked sensitive. Existing customized toolbar and retention settings are preserved.
Tapping the clipboard button while history is off offers to enable it. The settings
home explains when Do Not Disturb or silent mode suppresses feedback and exposes the
separate option for vibration during Do Not Disturb.

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
.\gradlew.bat :app:testDebugUnitTest --tests '*LocalDictionaryProviderTest' --tests '*CupertinoDefaultsTest' --tests '*InputPrivacyTest'
.\gradlew.bat :app:assembleDebugNoMinify
python tools/check_privacy.py --merged
```

The `Cupertino Keys` GitHub Actions workflow is configured to run these checks and upload a development APK.
Tests cover dictionary persistence, locale separation, duplicate handling, private URIs,
first-run language defaults, changes to no-learning flags on API 29 and 36, and Android 16
window geometry/blur boundaries. All 23 cases passed locally on JDK 21.
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
profile behavior still requires the [device checks](docs/DEVICE_TESTING.md#work-profile-on-xiaomi-14).
Password and no-learning fields retain their privacy restrictions in either profile.

Primary device: **Xiaomi 14 / Android 16**. Secondary: **Xiaomi Mi 8 / Android 10 (API 29), MIUI 12**.
See [device test steps](docs/DEVICE_TESTING.md).

---

# Original HeliBoard documentation
HeliBoard is a privacy-conscious and customizable open-source keyboard, based on AOSP / OpenBoard.
Does not use internet permission, and thus is 100% offline.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/helium314.keyboard/)
[<img src="https://user-images.githubusercontent.com/663460/26973090-f8fdc986-4d14-11e7-995a-e7c5e79ed925.png" alt="Get APK from GitHub" height="80">](https://github.com/HeliBorg/HeliBoard/releases/latest)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/helium314.keyboard)

## Table of Contents

- [Features](#features)
- [Contributing](#contributing-)
   * [Reporting Issues](#reporting-issues)
   * [Translations](#translations)
   * [To Community](#to-community)
   * [Code Contribution](CONTRIBUTING.md)
- [Links](#links)
- [License](#license)
- [Credits](#credits)
  * [Funding](#funding)

# Features
<ul>
  <li>Add dictionaries for suggestions and spell check</li>
  <ul>
    <li>build your own, or get them  <a href="https://codeberg.org/Helium314/aosp-dictionaries#dictionaries">here</a> (quality may vary)</li>
    <li>additional dictionaries for emojis or scientific symbols can be used to provide suggestions (similar to "emoji search")</li>
    <li>note that for Korean layouts, suggestions only work using <a href="https://github.com/openboard-team/openboard/commit/83fca9533c03b9fecc009fc632577226bbd6301f">this dictionary</a>, the tools in the dictionary repository are not able to create working dictionaries</li>
  </ul>
  <li>Customize keyboard themes (style, colors and background image)</li>
  <li>Emoji search (inline and separate, requires <a href="https://codeberg.org/Helium314/aosp-dictionaries">emoji dictionary</a>)</li>
  <ul>
    <li>can follow the system's day/night setting on Android 10+ (and on some versions of Android 9)</li>
    <li>can follow dynamic colors for Android 12+</li>
  </ul>
  <li>Customize keyboard <a href="https://github.com/HeliBorg/HeliBoard/blob/main/layouts.md">layouts</a> (only available when disabling <i>use system languages</i>)</li>
  <li>Customize special layouts, like symbols, number,  or functional key layout</li>
  <li>Multilingual typing</li>
  <li>Glide typing (<i>only with closed source library</i> ☹️)</li>
  <ul>
    <li>library not included in the app, as there is no compatible open source library available</li>
    <li>can be extracted from GApps packages ("<i>swypelibs</i>"), or downloaded <a href="https://github.com/erkserkserks/openboard/tree/46fdf2b550035ca69299ce312fa158e7ade36967/app/src/main/jniLibs">here</a> (click on the file and then "raw" or the tiny download button)</li>
  </ul>
  <li>Clipboard history</li>
  <li>One-handed mode</li>
  <li>Split keyboard</li>
  <li>Number pad</li>
  <li>Backup and restore your settings and learned word / history data</li>
</ul>

For [FAQ](https://github.com/HeliBorg/HeliBoard/wiki/FAQ), [hidden features](https://github.com/HeliBorg/HeliBoard/wiki/9.-Hidden-features) and more information about the app and features, please visit the [wiki](https://github.com/HeliBorg/HeliBoard/wiki)

# Contributing ❤

## Reporting Issues

Whether you encountered a bug, or want to see a new feature in HeliBoard, you can contribute to the project by opening a new issue [here](https://github.com/HeliBorg/HeliBoard/issues). Your help is always welcome!

Before opening a new issue, be sure to check the following:
 - **Does the issue already exist?** Make sure a similar issue has not been reported by browsing [existing issues](https://github.com/HeliBorg/HeliBoard/issues?q=). Please search open and closed issues. In case of feature requests you could also check the [FAQ](https://github.com/HeliBorg/HeliBoard/wiki/FAQ) and [hidden features](https://github.com/HeliBorg/HeliBoard/wiki/9.-Hidden-features).
 - **Is the issue still relevant?** Make sure your issue is not already fixed in the latest version of HeliBoard.
 - **Is it a single topic?** If you want to suggest multiple things, open multiple issues.
 - **Did you use the issue template?** It is important to make life of our kind contributors easier by avoiding issues that miss key information to their resolution.
 - **Is it written by a human?** Do not use LLMs or similar to generate issues. Having LLMs help with translation or similar is acceptable, but must be disclosed. See also [AI_USAGE.md](AI_USAGE.md)
Note that issues that that ignore part of the issue template will likely get treated with very low priority, as often they are needlessly hard to read or understand (e.g. huge screenshots, not providing a proper description, or addressing multiple topics). Blatant violation of the guidelines may result in the issue getting closed.

If you're interested, you can read the following useful text about effective bug reporting (a bit longer read): https://www.chiark.greenend.org.uk/~sgtatham/bugs.html

## Translations
Translations can be added using [Weblate](https://translate.codeberg.org/projects/heliboard/). You will need an account to update translations and add languages. Add the language you want to translate to in Languages -> Manage translated languages in the top menu bar.
Updating translations in a PR will not be accepted, as it may cause conflicts with Weblate translations.

Some notes on translations
* when translating metadata, translating the changelogs is rather useless. It's available as it was requested by translators.
* the `hidden_features_message` is horrible to translate with Weblate, and serves little benefit as it's just a copy of what's already in the wiki: https://github.com/HeliBorg/HeliBoard/wiki/9.-Hidden-features. It's been made available in the app on user request/contribution.

## To Community
There is the [discussions on GitHub](https://github.com/HeliBorg/HeliBoard/discussions), or if you prefer a more open network there is [Lemmy](https://lemmy.world/c/Heliboard).
You can share your themes, layouts and dictionaries with other people:
* Themes can be saved and loaded using the menu on top-right in the _adjust colors_ screen
  * you can share custom colors in a separate [discussion section](https://github.com/HeliBorg/HeliBoard/discussions/categories/custom-colors)
  * there are theme collections available at [Star-Trowa/heliboard-themes](https://github.com/Star-Trowa/heliboard-themes) and [PickleHik3/droid-tings](https://github.com/PickleHik3/droid-tings)
* Custom keyboard layouts are text files whose content you can edit, copy and share
  * this applies to main keyboard layouts and to special layouts adjustable in advanced settings
  * see [layouts.md](layouts.md) for details
  * you can share custom layouts in a separate [discussion section](https://github.com/HeliBorg/HeliBoard/discussions/categories/custom-layout)
  * [Roccobot's Layout Maker](https://roccobot.github.io/HeliBoard-RLM/) is a browser-based editor for json layout files
* Creating dictionaries is a little more work
  * first you will need a wordlist in the format from [here](https://codeberg.org/Helium314/aosp-dictionaries/src/branch/main/wordlists/sample.combined) and in the repository readme
    * you can load the wordlist dictionary directly in HeliBoard like you would load a dictionary, but other AOSP keyboard forks will likely need the .dict file
  * then you need to compile the dictionary using [external tools](https://github.com/remi0s/aosp-dictionary-tools)
    * when you want to add the dictionary to the [repository](https://codeberg.org/Helium314/aosp-dictionaries/), no need to compile to a .dict file
  * the resulting file (and ideally the wordlist too) can be shared with other users
  * note that there will not be any further dictionaries added to this app, but you can add dictionaries to the [dictionaries repository](https://codeberg.org/Helium314/aosp-dictionaries)

## Code Contribution
See [Contribution Guidelines](CONTRIBUTING.md)

# Links
* Info
  * [Wiki](https://github.com/HeliBorg/HeliBoard/wiki), including FAQ, help on customizing layouts, and gesture data gathering
  * [Layout documentation](layouts.md) (more technical info regarding layout customization)
  * [For creating custom dictionaries](https://codeberg.org/Helium314/aosp-dictionaries#wordlist-information) (see also top of the linked readme)
* Community
  * [Lemmy](https://lemmy.world/c/Heliboard)
  * [Reddit](https://www.reddit.com/r/HeliBoard)
  * GitHub [discussions](https://github.com/HeliBorg/HeliBoard/discussions)
* Other
  * [Translations](https://translate.codeberg.org/projects/heliboard/)
  * [Dictionaries](https://codeberg.org/Helium314/aosp-dictionaries)
  * [k3lp](https://codeberg.org/k3lp/k3lp) is a WIP library for keyboard layout parsing that will be implemented in HeliBoard when ready (created by [FlorisBoard](https://github.com/florisboard/florisboard/) maintainers)
  * [swipe-o-scope](https://codeberg.org/eclexic/swipe-o-scope) for visualizing gesture data as created when using gesture data gathering

# License

HeliBoard (as a fork of OpenBoard) is licensed under GNU General Public License v3.0.

 > Permissions of this strong copyleft license are conditioned on making available complete source code of licensed works and modifications, which include larger works using a licensed work, under the same license. Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.

See repo's [LICENSE](/LICENSE) file.

Since the app is based on Apache 2.0 licensed AOSP Keyboard, an [Apache 2.0](LICENSE-Apache-2.0) license file is provided.
The icon is licensed under [Creative Commons BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). A [license file](LICENSE-CC-BY-SA-4.0) is also included.

# Credits
- Icon by [Fabian OvrWrt](https://github.com/FabianOvrWrt) with contributions from [The Eclectic Dyslexic](https://github.com/the-eclectic-dyslexic)
- [OpenBoard](https://github.com/openboard-team/openboard)
- [AOSP Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- [LineageOS](https://review.lineageos.org/admin/repos/LineageOS/android_packages_inputmethods_LatinIME)
- [Simple Keyboard](https://github.com/rkkr/simple-keyboard)
- [Indic Keyboard](https://gitlab.com/indicproject/indic-keyboard)
- [FlorisBoard](https://github.com/florisboard/florisboard/)
- Our [contributors](https://github.com/HeliBorg/HeliBoard/graphs/contributors)

## Funding

This project is funded through [NGI Mobifree Fund](https://nlnet.nl/mobifree), a fund established by [NLnet](https://nlnet.nl) with financial support from the European Commission's [Next Generation Internet](https://ngi.eu) program. Learn more at the [NLnet project page](https://nlnet.nl/project/GestureTyping).

[<img src="https://nlnet.nl/logo/banner.png" alt="NLnet foundation logo" width="20%" />](https://nlnet.nl)

Further the project benefits from donations provided by many users (thank you all!).
