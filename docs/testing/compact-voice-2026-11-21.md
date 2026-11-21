# Compact dictation on Xiaomi 14

The external Google Voice Typing IME owns its layout. On Xiaomi 14 / Android 16,
it displayed a tall dark panel above a separate light system navigation strip.
Cupertino's window was no longer visible, so changing its keyboard padding could
not reposition Google's controls.

The development branch now uses Google's public recognition service with our own
80 dp popup, 8 dp above the footer microphone. The original keyboard and editor
insets remain unchanged. Final text goes through LatinIME's normal text-input
path; partial recognition appears only in the popup. The checkmark finishes
dictation; the cross cancels. No audio is stored by Cupertino Keys.

This adds runtime RECORD_AUDIO permission, requested only after a microphone tap.
Granting permission does not automatically begin recording: the user taps again
in the intended field. Google can process audio online. The keyboard still has
no INTERNET permission. An external enabled Google voice IME is the fallback
when no supported public recognition service can be discovered.

## Verified

- APK installed over the existing app on Xiaomi 14; signature verified.
- 15 device assertions in `voice_compact`: panel dimensions, 8 dp anchor gap,
  screen bounds, stable keyboard/editor geometry, partial/final results,
  duplicate results, cancellation, password-field switches, hiding, typing,
  and retaining Cupertino as the current IME.
- The scenario uses synthetic recognition callbacks; it never starts recording.
- 7 existing voice/privacy assertions passed, including password/no-learning
  suppression. Both merged-manifest privacy checks passed.
- All 24 selected JVM regression tests passed.
- Visual inspection of the captured test screen confirmed compact bottom placement.

Actual Google recognition and the new runtime permission flow still need user
confirmation. Work-profile dictation and other devices are not verified by these
checks. The published `v0.1.0` remains unchanged and predates this feature.

Local APK SHA-256:
`ed14625d3d59a32d775e2ee437fd124fd0349b02d10030b66d4936f96f203d4f`.

Android API references:
[SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer),
[RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).
