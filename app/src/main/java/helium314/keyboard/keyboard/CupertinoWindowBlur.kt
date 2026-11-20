// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import helium314.keyboard.latin.R
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.common.CupertinoColors
import helium314.keyboard.latin.settings.Settings
import java.util.function.Consumer

/** GPU-composited background blur; no screenshots, pixel access, or overlay permission. */
object CupertinoWindowBlur {
    private var active: BlurSession? = null

    @JvmStatic fun attach(ime: LatinIME, input: View) {
        detach()
        val window = ime.window?.window
        if (Build.VERSION.SDK_INT >= 31 && window != null) active = BlurSession(window, input, ime)
    }
    @JvmStatic fun refresh() { if (Build.VERSION.SDK_INT >= 31) active?.show() }
    @JvmStatic fun hide() { if (Build.VERSION.SDK_INT >= 31) active?.hide() }
    @JvmStatic fun detach() {
        if (Build.VERSION.SDK_INT >= 31) active?.close()
        active = null
    }
    internal fun isActive() = Build.VERSION.SDK_INT >= 31 && active?.background?.isShowing == true

    @RequiresApi(31)
    private class BlurSession(private val host: Window, private val input: View, private val ime: LatinIME) : AutoCloseable {
        private val manager = host.windowManager
        private val frame = input.findViewById<View>(R.id.main_keyboard_frame)
        var background: Dialog? = null
            private set
        private var closed = false
        private var listening = false
        private var failed = false
        private var windowVisible = ime.isInputViewShown
        private var appliedSpec: String? = null
        private val update = Runnable { refreshSurface() }
        private val blurListener = Consumer<Boolean> { requestUpdate() }
        private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> requestUpdate() }

        init {
            host.setBackgroundBlurRadius(0)
            input.addOnLayoutChangeListener(layoutListener)
            frame?.addOnLayoutChangeListener(layoutListener)
            runCatching {
                manager.addCrossWindowBlurEnabledListener(input.context.mainExecutor, blurListener)
                listening = true
            }
            requestUpdate()
        }
        fun requestUpdate() {
            input.removeCallbacks(update)
            if (!closed) input.post(update)
        }
        fun show() {
            windowVisible = true
            requestUpdate()
        }
        private fun refreshSurface() {
            if (closed || frame == null) return
            val settings = Settings.getValues() ?: return
            val colors = settings.mColors as? CupertinoColors
            val enabled = colors != null && !settings.mIsFloatingKeyboard && !ime.isFullscreenMode && !failed && windowVisible &&
                input.isShown && frame.isShown && input.windowToken != null && frame.width > 0 && frame.height > 0 &&
                runCatching { manager.isCrossWindowBlurEnabled }.getOrDefault(false)
            if (!enabled) {
                dismissBackground()
                colors?.setSurface(frame, false)
                return
            }
            val location = IntArray(2)
            // LAYOUT_IN_SCREEN positions this child in display coordinates, not in
            // the IME's status-bar-inset coordinate space.
            frame.getLocationOnScreen(location)
            val density = input.resources.displayMetrics.density
            val spec = "${colors.dark}:${location.toList()}:${frame.width}:${frame.height}:$density"
            if (appliedSpec == spec && background?.isShowing == true) return
            try {
                val dialog = background ?: Dialog(input.context, R.style.CupertinoBlurWindow).also { dialog ->
                    dialog.setCancelable(false)
                    val window = requireNotNull(dialog.window)
                    window.attributes = window.attributes.apply {
                        token = input.windowToken
                        type = WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA
                        gravity = Gravity.TOP or Gravity.LEFT
                        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        dimAmount = 0f
                        windowAnimations = 0
                        title = "Cupertino keyboard backdrop"
                        setFitInsetsTypes(0)
                    }
                    window.setBackgroundDrawable(GradientDrawable().apply {
                        setColor(0x011C1C20)
                        cornerRadius = CupertinoColors.SURFACE_CORNER_RADIUS_DP * density
                    })
                    // PhoneWindow needs its decor installed before the blur API is called.
                    window.decorView.isForceDarkAllowed = false
                    window.setBackgroundBlurRadius((28f * density).toInt().coerceAtMost(120))
                    background = dialog
                }
                val window = requireNotNull(dialog.window)
                window.attributes = window.attributes.apply {
                    x = location[0]
                    y = location[1]
                    width = frame.width
                    height = frame.height
                }
                if (!dialog.isShowing) dialog.show()
                colors.setSurface(frame, true)
                appliedSpec = spec
            } catch (_: RuntimeException) {
                // Unsupported vendor window behavior must never take down text input.
                failed = true
                dismissBackground()
                colors.setSurface(frame, false)
            }
        }
        fun hide() {
            windowVisible = false
            input.removeCallbacks(update)
            dismissBackground()
        }
        private fun dismissBackground() {
            runCatching { background?.dismiss() }
            background = null
            appliedSpec = null
        }
        override fun close() {
            closed = true
            hide()
            input.removeOnLayoutChangeListener(layoutListener)
            frame?.removeOnLayoutChangeListener(layoutListener)
            if (listening) runCatching { manager.removeCrossWindowBlurEnabledListener(blurListener) }
        }
    }
}
