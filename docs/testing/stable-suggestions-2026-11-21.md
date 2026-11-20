# Stable suggestions and Enter

This supersedes the collapsing-row behavior in the earlier glass report. Changing
the keyboard height whenever candidates appeared or disappeared moved the host app's
message composer. Empty candidate content now uses `INVISIBLE`, preserving its
measured height. IME content/touch insets count that reserved row as well as visible
rows. The bottom clipboard remains available regardless of candidate visibility.

The Cupertino Enter/action key no longer gets the engine's default long-press popup
(emoji, clipboard, and navigation shortcuts). Normal action handling is unchanged.
The dedicated emoji key uses an original outline smile vector with a curved mouth,
inspired by iPhone controls; Apple artwork is not bundled. Reference:
[Apple's emoji keyboard guide](https://support.apple.com/en-mo/102507).

Verified on Xiaomi 14, Android 16:

- 29 device assertions: constant frame height and reported editor inset after every
  character of `hello` and every Backspace; empty/nonempty suggestions; app-driven
  field clearing; continued typing; Enter held for 1.2 seconds opens no menu or emoji
  and still inserts a newline; normal hide/reopen behavior.
- 4 geometry assertions pass. At this device's density, the frame is consistently
  1200×919 px, including the 120 px candidate area and 18 px upper padding.
- 22 selected JVM regression cases and both merged-manifest privacy checks pass.

Tests dispatch events through the app's own views because HyperOS blocks system touch
injection. The updated build is installed over the existing personal-profile app.
