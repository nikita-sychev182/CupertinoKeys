// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.dictionary.LocalUserDictionary.Words
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.settings.SettingsActivity
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real IME view and InputConnection, without shell input injection.
 * Only available in debugNoMinify. Uses disposable text in KeyboardTestActivity.
 */
class KeyboardSmokeInstrumentation : Instrumentation() {
    private lateinit var activity: Activity
    private val checks = mutableListOf<String>()
    private var phase = "input"

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        phase = arguments?.getString("phase") ?: "input"
        start()
    }

    private fun <T> main(block: () -> T): T {
        val result = AtomicReference<T>()
        val error = AtomicReference<Throwable>()
        runOnMainSync { try { result.set(block()) } catch (e: Throwable) { error.set(e) } }
        error.get()?.let { throw it }
        return result.get()
    }

    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun editor() = views(activity.window.decorView).filterIsInstance<EditText>().single()
    private fun text() = main { editor().text.toString() }
    private fun button(label: String) {
        main { views(activity.window.decorView).filterIsInstance<Button>().single { it.text == label }.performClick() }
        waitForIdleSync()
        SystemClock.sleep(250)
    }
    private fun check(name: String, condition: Boolean) {
        if (!condition) error("FAILED: $name")
        checks += "PASS: $name"
    }
    private fun keyboard() = KeyboardSwitcher.getInstance().mainKeyboardView
        ?: error("Select Cupertino Keys as the current keyboard first")
    private fun ime() = KeyboardSwitcher::class.java.getDeclaredField("mLatinIME")
        .apply { isAccessible = true }.get(KeyboardSwitcher.getInstance()) as LatinIME
    private fun language() = main { requireNotNull(keyboard().keyboard).mId.locale.language }

    private fun navigationTap(id: Int) {
        main {
            val target = keyboard().rootView.findViewById<View>(id)
            val position = IntArray(2)
            target.getLocationInWindow(position)
            val insets = android.inputmethodservice.InputMethodService.Insets()
            ime().onComputeInsets(insets)
            val x = position[0] + target.width / 2
            val y = position[1] + target.height / 2
            check("navigation target $id is inside IME touch region", insets.touchableRegion.contains(x, y))
            val now = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, now + if (action == MotionEvent.ACTION_UP) 50 else 0,
                    action, x.toFloat(), y.toFloat(), 0)
                keyboard().rootView.dispatchTouchEvent(event)
                event.recycle()
            }
        }
        waitForIdleSync()
        SystemClock.sleep(500)
    }

    private fun touch(code: Int, swipe: Float = 0f, cancel: Boolean = false) {
        val origin = main {
            val view = keyboard()
            val key = requireNotNull(view.keyboard).getKey(code) ?: error("Key $code unavailable")
            floatArrayOf(key.x + key.width / 2f + view.paddingLeft, key.y + key.height / 2f + view.paddingTop)
        }
        val down = SystemClock.uptimeMillis()
        fun event(action: Int, x: Float) = main {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, origin[1], 0)
            keyboard().dispatchTouchEvent(event)
            event.recycle()
        }
        event(MotionEvent.ACTION_DOWN, origin[0])
        if (swipe != 0f) for (step in 1..10) {
            SystemClock.sleep(20)
            event(MotionEvent.ACTION_MOVE, origin[0] + swipe * step / 10)
        } else SystemClock.sleep(50)
        event(if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, origin[0] + swipe)
        waitForIdleSync()
        SystemClock.sleep(150)
    }

    private fun dictionaryCount(): Int = targetContext.contentResolver.query(Words.CONTENT_URI,
        arrayOf(Words.WORD), "word=? AND locale=?", arrayOf("cupertinotestword", "en_US"), null).use { it!!.count }

    override fun onStart() {
        val output = Bundle()
        try {
            if (phase.startsWith("preview_")) SettingsActivity.forceNight = phase == "preview_dark"
            activity = startActivitySync(Intent(targetContext, KeyboardTestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (phase.startsWith("preview_")) main { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
            button("Text")
            repeat(30) {
                if (!main { KeyboardSwitcher.getInstance().mainKeyboardView?.isShown == true }) SystemClock.sleep(200)
            }
            check("IME is visible", main { keyboard().isShown })
            if (phase == "work_setup") {
                if (language() != "en") touch(32, 250f)
                button("Add test word")
                check("work-profile test word ready in personal IME dictionary", dictionaryCount() == 1)
                "ctw".forEach { touch(it.code) }
            } else if (phase == "work_cleanup") {
                button("Delete test word")
                check("work-profile test word removed", dictionaryCount() == 0)
            } else if (phase == "navigation") {
                check("custom bottom panel visible", main {
                    keyboard().rootView.findViewById<View>(R.id.cupertino_navigation_bar).isShown
                })
                val beforeLanguage = language()
                touch(','.code)
                check("comma key available in first alphabet", text() == ",")
                button("Clear")
                val beforeText = text()
                navigationTap(R.id.cupertino_language)
                check("bottom globe switches language without text insertion", language() != beforeLanguage && text() == beforeText)
                touch(','.code)
                check("comma key available in second alphabet", text() == ",")
                button("Clear")
                navigationTap(R.id.cupertino_language)
                check("bottom globe cycles back", language() == beforeLanguage)
                navigationTap(R.id.cupertino_hide)
                check("bottom right button hides IME", main { !ime().isInputViewShown })
                button("Text")
                SystemClock.sleep(500)
                check("IME reopens with custom panel", main {
                    ime().isInputViewShown && keyboard().rootView.findViewById<View>(R.id.cupertino_navigation_bar).isShown
                })
            } else if (phase == "geometry") {
                val diagnostics = main {
                    val root = keyboard().rootView
                    val frame = root.findViewById<View>(R.id.main_keyboard_frame)
                    val navigation = root.findViewById<View>(R.id.cupertino_navigation_bar)
                    val location = IntArray(2)
                    frame.getLocationInWindow(location)
                    val insets = root.rootWindowInsets
                    val keys = requireNotNull(keyboard().keyboard)
                    val keysBottom = keys.sortedKeys.maxOf { it.y + it.height }
                    "root=${root.width}x${root.height} frame=${frame.width}x${frame.height}@${location.toList()} " +
                        "keysView=${keyboard().height} occupied=${keys.mOccupiedHeight} lastKeyBottom=$keysBottom " +
                        "keyPadding=${keyboard().paddingTop},${keyboard().paddingBottom} " +
                        "padding=${root.paddingLeft},${root.paddingTop},${root.paddingRight},${root.paddingBottom} " +
                        "footer=${navigation.isShown}:${navigation.height} " +
                        if (android.os.Build.VERSION.SDK_INT >= 30) "caption=${insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.captionBar())} " +
                            "captionVisible=${insets.isVisible(android.view.WindowInsets.Type.captionBar())} " +
                            "nav=${insets.getInsets(android.view.WindowInsets.Type.navigationBars())} " +
                            "gestures=${insets.getInsets(android.view.WindowInsets.Type.systemGestures())}" else ""
                }
                check("IME background has no top padding", main { keyboard().rootView.paddingTop == 0 })
                check("custom panel does not duplicate navigation padding above globe", main {
                    !keyboard().rootView.findViewById<View>(R.id.cupertino_navigation_bar).isShown || keyboard().paddingBottom == 0
                })
                check("reported IME top matches visible keyboard frame", main {
                    val frame = keyboard().rootView.findViewById<View>(R.id.main_keyboard_frame)
                    val location = IntArray(2)
                    frame.getLocationInWindow(location)
                    val insets = android.inputmethodservice.InputMethodService.Insets()
                    ime().onComputeInsets(insets)
                    insets.contentTopInsets >= 0 && insets.contentTopInsets == location[1]
                })
                sendStatus(1, Bundle().apply { putString("stream", "GEOMETRY: $diagnostics\n") })
                SystemClock.sleep(8000)
            } else
            if (phase.startsWith("preview_")) {
                val russian = phase == "preview_dark"
                if (language() != if (russian) "ru" else "en") touch(32, 250f)
                (if (russian) "привет" else "hello").forEach { touch(it.code) }
                val diagnostics = main {
                    val view = keyboard()
                    val key = requireNotNull(view.keyboard).getKey(if (russian) 'п'.code else 'h'.code)!!
                    val params = helium314.keyboard.keyboard.KeyboardView::class.java.getDeclaredField("mKeyDrawParams")
                        .apply { isAccessible = true }.get(view) as helium314.keyboard.keyboard.internal.KeyDrawParams
                    "palette=${Settings.getValues().mColors.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT)} " +
                        "key=${key.visualAttributes?.mTextColor} view=${view.keyVisualAttribute?.mTextColor} " +
                        "params=${params.mTextColor} selected=${key.selectTextColor(params)}"
                }
                sendStatus(1, Bundle().apply { putString("stream", "COLOR_CHECK: $diagnostics\n") })
                sendStatus(1, Bundle().apply { putString("stream", "PREVIEW_READY\n") })
                SystemClock.sleep(12000)
                SettingsActivity.forceNight = null
            } else if (phase == "persistence") {
                check("personal word survives process restart", dictionaryCount() == 1)
                button("Delete test word")
                check("personal word deleted", dictionaryCount() == 0)
            } else {
                if (language() != "ru") touch(32, -250f)
                check("Russian available", language() == "ru")
                "привет".forEach { touch(it.code) }
                check("Russian key taps", text() == "привет")
                touch(32)
                check("space tap inserts one space", text() == "привет ")
                val before = text()
                touch(32, 250f)
                check("right space swipe switches to English without insertion", language() == "en" && text() == before)
                "test".forEach { touch(it.code) }
                check("English key taps", text().endsWith("test"))
                touch(KeyCode.DELETE)
                check("backspace deletes character", text().endsWith("tes"))
                val beforeLeft = text()
                touch(32, -250f)
                check("left space swipe switches to Russian without insertion", language() == "ru" && text() == beforeLeft)
                touch(32, cancel = true)
                check("cancelled touch inserts nothing", text() == beforeLeft)
                button("Clear")
                touch(32, 3f)
                check("small movement remains a space tap", text() == " " && language() == "ru")
                button("Number")
                "123".forEach { touch(it.code) }
                check("numeric field", text() == "123")
                button("Password")
                check("password disables learning and suggestions", main {
                    val settings = Settings.getInstance().current
                    settings.mIncognitoModeEnabled && !settings.mSuggestionsEnabled
                })
                if (language() != "en") touch(32, 250f)
                "test".forEach { touch(it.code) }
                check("password field accepts key taps", text() == "test")
                button("No learning")
                check("app no-learning flag enables incognito", main { Settings.getInstance().current.mIncognitoModeEnabled })
                "test".forEach { touch(it.code) }
                check("no-learning field accepts key taps", text() == "test")
                button("Text")
                button("Add test word")
                button("Add test word")
                check("personal word saved without duplicates", dictionaryCount() == 1)
                // A second instrumentation invocation checks persistence after process restart.
            }
            output.putString("stream", checks.joinToString("\n", "\n", "\n"))
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            SettingsActivity.forceNight = null
            output.putString("stream", checks.joinToString("\n") + "\n" + error.stackTraceToString())
            finish(Activity.RESULT_CANCELED, output)
        }
    }
}
