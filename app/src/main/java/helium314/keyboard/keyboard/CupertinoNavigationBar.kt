// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.res.ColorStateList
import android.graphics.Insets
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.CupertinoColors
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings

/** Android 16's public IME caption-bar API. Never hides the navigation/gesture bar. */
object CupertinoNavigationBar {
    private var active: Session? = null

    @RequiresApi(30)
    internal fun insetsForKeyboard(insets: WindowInsets, customPanelVisible: Boolean): WindowInsets {
        if (!customPanelVisible) return insets
        val navigation = insets.getInsets(WindowInsets.Type.navigationBars())
        // The bottom panel now occupies this space. MainKeyboardView's fitsSystemWindows
        // must not add the navigation inset a second time between keys and the panel.
        return WindowInsets.Builder(insets).setInsets(WindowInsets.Type.navigationBars(),
            Insets.of(navigation.left, navigation.top, navigation.right, 0)).build()
    }

    @JvmStatic fun attach(ime: LatinIME, input: View) {
        detach()
        if (Build.VERSION.SDK_INT >= 36) active = Session(ime, input)
    }

    @JvmStatic fun refresh() {
        if (Build.VERSION.SDK_INT >= 36) active?.requestUpdate()
    }

    @JvmStatic fun detach() {
        if (Build.VERSION.SDK_INT >= 36) active?.close()
        active = null
    }

    @JvmStatic fun height(input: View): Int = input.findViewById<View>(R.id.cupertino_navigation_bar)
        ?.let { if (it.visibility == View.VISIBLE) it.height else 0 } ?: 0

    @RequiresApi(36)
    private class Session(private val ime: LatinIME, private val input: View) {
        private val bar = input.findViewById<View>(R.id.cupertino_navigation_bar)
        private val globe = input.findViewById<ImageButton>(R.id.cupertino_language)
        private val voice = input.findViewById<ImageButton>(R.id.cupertino_voice)
        private val clipboard = input.findViewById<ImageButton>(R.id.cupertino_clipboard)
        private var requested = false
        private var closed = false
        private val update = Runnable { if (!closed) input.requestApplyInsets() }

        init {
            clipboard.setOnClickListener {
                ime.onCodeInput(helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.CLIPBOARD, -1, -1, false)
            }
            clipboard.setOnLongClickListener {
                ime.onCodeInput(helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.CLIPBOARD_PASTE, -1, -1, false)
                true
            }
            globe.setOnClickListener {
                val subtypes = SubtypeSettings.getEnabledSubtypes(true)
                val current = RichInputMethodManager.getInstance().currentSubtype.rawSubtype
                if (subtypes.size > 1)
                    ime.switchToSubtype(subtypes[(subtypes.indexOf(current) + 1) % subtypes.size])
            }
            // Keep Android's IME picker available when replacing its switcher button.
            globe.setOnLongClickListener {
                ime.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                true
            }
            voice.setOnClickListener {
                if (GoogleVoiceInput.isAllowed(Settings.getValues())) GoogleVoiceInput.start(ime)
                else ime.requestHideSelf(0)
            }
            voice.setOnLongClickListener { ime.requestHideSelf(0); true }
            input.setOnApplyWindowInsetsListener { view, insets ->
                apply(input.rootWindowInsets ?: insets)
                view.onApplyWindowInsets(insetsForKeyboard(insets, bar.visibility == View.VISIBLE))
            }
            requestUpdate()
        }

        fun requestUpdate() {
            input.removeCallbacks(update)
            input.post(update)
        }

        private fun apply(insets: WindowInsets) {
            val settings = Settings.getValues() ?: return
            val caption = insets.getInsetsIgnoringVisibility(WindowInsets.Type.captionBar()).bottom
            val gestures = insets.getInsets(WindowInsets.Type.systemGestures()).bottom
            val enabled = settings.mColors is CupertinoColors && !settings.mIsFloatingKeyboard &&
                !ime.isFullscreenMode && caption > 0 && gestures > 0
            if (requested != enabled) {
                requested = enabled
                if (enabled) input.windowInsetsController?.hide(WindowInsets.Type.captionBar())
                else input.windowInsetsController?.show(WindowInsets.Type.captionBar())
            }
            val visible = enabled && !insets.isVisible(WindowInsets.Type.captionBar())
            bar.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                val navigation = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                val height = maxOf((48 * input.resources.displayMetrics.density).toInt(), caption - navigation)
                if (bar.layoutParams.height != height) {
                    bar.layoutParams = bar.layoutParams.apply { this.height = height }
                }
                val tint = ColorStateList.valueOf(settings.mColors.get(ColorType.KEY_TEXT))
                globe.imageTintList = tint
                voice.imageTintList = tint
                clipboard.imageTintList = tint
                clipboard.isForceDarkAllowed = false
                globe.isForceDarkAllowed = false
                voice.isForceDarkAllowed = false
                val voiceAllowed = GoogleVoiceInput.isAllowed(settings)
                voice.setImageResource(if (voiceAllowed) R.drawable.cupertino_microphone else R.drawable.cupertino_hide)
                voice.contentDescription = input.context.getString(if (voiceAllowed)
                    R.string.cupertino_voice_description else R.string.space_swipe_hide_keyboard_entry)
                voice.tooltipText = voice.contentDescription
            }
            input.findViewById<helium314.keyboard.latin.suggestions.SuggestionStripView>(R.id.suggestion_strip_view)
                ?.updateAutoVisibility()
        }

        fun close() {
            closed = true
            input.removeCallbacks(update)
            input.setOnApplyWindowInsetsListener(null)
            if (requested) input.windowInsetsController?.show(WindowInsets.Type.captionBar())
            bar.visibility = View.GONE
        }
    }
}
