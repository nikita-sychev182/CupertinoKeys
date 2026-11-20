// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.CupertinoColors
import helium314.keyboard.latin.settings.Settings
import java.util.function.Consumer

/** Public cross-window blur API only. Never captures, stores or reads app pixels. */
object CupertinoWindowBlur {
    private var active: AutoCloseable? = null

    @JvmStatic fun attach(window: Window?, input: View) {
        detach()
        if (Build.VERSION.SDK_INT < 31 || window == null) return
        active = BlurSession(window, input)
    }

    @JvmStatic fun detach() {
        active?.close()
        active = null
    }

    @RequiresApi(31)
    private class BlurSession(private val window: Window, private val input: View) : AutoCloseable {
        private val originalBackground = window.decorView.background
        private val manager = window.windowManager
        private var closed = false
        private var listening = false
        private var appliedSpec: String? = null
        private val update = Runnable { refresh() }
        private val blurListener = Consumer<Boolean> { input.post(update) }
        private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            input.removeCallbacks(update)
            input.post(update)
        }

        init {
            input.addOnLayoutChangeListener(layoutListener)
            runCatching {
                manager.addCrossWindowBlurEnabledListener(input.context.mainExecutor, blurListener)
                listening = true
            }
            input.post(update)
        }

        private fun refresh() {
            if (closed) return
            val settings = Settings.getValues() ?: return
            val colors = settings.mColors as? CupertinoColors
            val frame = input.findViewById<View>(R.id.main_keyboard_frame) ?: return
            val decor = window.decorView
            val enabled = colors != null && !settings.mIsFloatingKeyboard &&
                runCatching { manager.isCrossWindowBlurEnabled }.getOrDefault(false) &&
                frame.width > 0 && frame.height > 0 && decor.width > 0
            if (!enabled) {
                if (appliedSpec == "opaque:${colors?.dark}") return
                appliedSpec = "opaque:${colors?.dark}"
                window.setBackgroundBlurRadius(0)
                window.setBackgroundDrawable(originalBackground)
                colors?.setSurface(frame, false)
                return
            }
            val location = IntArray(2)
            frame.getLocationInWindow(location)
            val left = location[0].coerceIn(0, decor.width)
            val top = location[1].coerceIn(0, decor.height)
            val right = (decor.width - left - frame.width).coerceAtLeast(0)
            val bottom = (decor.height - top - frame.height).coerceAtLeast(0)
            val density = input.resources.displayMetrics.density
            val spec = "${colors.dark}:$left:$top:$right:$bottom:${frame.width}:${frame.height}:$density"
            if (appliedSpec == spec) return
            appliedSpec = spec
            val radius = 19f * density
            val backdrop = GradientDrawable().apply {
                setColor(if (colors.dark) 0x5030333C else 0x50E7EAF0)
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            }
            // Window background outline limits blur to the keyboard, not the app above it.
            window.setBackgroundDrawable(InsetDrawable(backdrop, left, top, right, bottom))
            window.setBackgroundBlurRadius((28f * density).toInt())
            colors.setSurface(frame, true)
        }

        override fun close() {
            closed = true
            input.removeCallbacks(update)
            input.removeOnLayoutChangeListener(layoutListener)
            if (listening) manager.removeCrossWindowBlurEnabledListener(blurListener)
            window.setBackgroundBlurRadius(0)
            window.setBackgroundDrawable(originalBackground)
        }
    }
}
