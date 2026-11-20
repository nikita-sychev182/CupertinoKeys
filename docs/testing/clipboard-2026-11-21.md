# Graphite theme, feedback controls, and clipboard — Xiaomi 14

The dark surface is now neutral graphite (#19191B to #141416) with much subtler
color overlays. Keycaps use neutral grays. The normal IME uses a painted material,
not live transparency or a sampled application background.

The settings home now exposes the existing sound, vibration, and history controls.
History remains opt-in. A clipboard button is pinned beside suggestions by default;
existing user toolbar customizations are respected. Text history defaults to one-hour
retention and supports more than five entries; existing retention settings remain.
File collection defaults off. Sensitive-marked clips and copies made in password,
no-learning, or incognito fields are excluded from collection.

Device instrumentation passed 15 assertions: five real system clipboard copies,
five list-item insertions into the editor, pinned-button visibility, sound/vibration
settings, disabled collection, and sensitive/password/no-learning exclusions.
Test entries were removed after completion. Light/dark screenshots were captured in
the disposable test Activity. No microphone, network, or additional permissions added.

An initial test could not restore a pre-existing URI image clip because Android
denied re-granting its provider URI. The user was informed that it must be copied
again. The runner now refuses URI/intent clips before modifying the clipboard and
guarantees preference/history cleanup even if clipboard restoration fails.

Physical sound/vibration perception and copying across Shelter's clipboard policy
remain manual checks. The shared personal IME setup is unchanged.

All 23 selected JVM tests passed; source/merged-manifest privacy checks passed.
APK signatures v1/v2 verified. Installed in the personal profile on Xiaomi 14.
Deliverable: `build/deliverables/CupertinoKeys-0.1.0-clipboard.apk`.
SHA-256: `a0b9ecf59eb6e13b2a3158c03d9fdd836358091484f15a1dfeba0314ff362f3e`.

![Graphite dark keyboard](graphite-dark.png)
![Quick feedback and history settings](feedback-settings.png)
