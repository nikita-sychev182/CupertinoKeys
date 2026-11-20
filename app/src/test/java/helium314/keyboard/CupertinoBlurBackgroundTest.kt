// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import helium314.keyboard.keyboard.CupertinoBlurBackground
import helium314.keyboard.keyboard.CupertinoNavigationBar
import helium314.keyboard.latin.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class CupertinoBlurBackgroundTest {
    @Test fun customPanelConsumesOnlyTheDuplicateBottomNavigationInset() {
        val original = android.view.WindowInsets.Builder()
            .setInsets(android.view.WindowInsets.Type.statusBars(), android.graphics.Insets.of(0, 134, 0, 0))
            .setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.of(12, 0, 0, 48))
            .setInsets(android.view.WindowInsets.Type.systemGestures(), android.graphics.Insets.of(0, 0, 0, 144))
            .build()
        val adjusted = CupertinoNavigationBar.insetsForKeyboard(original, true)
        assertEquals(0, adjusted.getInsets(android.view.WindowInsets.Type.systemBars()).bottom)
        assertEquals(134, adjusted.getInsets(android.view.WindowInsets.Type.systemBars()).top)
        assertEquals(12, adjusted.getInsets(android.view.WindowInsets.Type.navigationBars()).left)
        assertEquals(144, adjusted.getInsets(android.view.WindowInsets.Type.systemGestures()).bottom)
        assertEquals(48, original.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom)
        assertEquals(original, CupertinoNavigationBar.insetsForKeyboard(original, false))
    }

    @Test fun blurDoesNotMoveInputOrChangeItsHeightAndTouchCoordinates() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.CupertinoImeWindow)
        val activity = controller.setup().get()
        try {
            val input = FrameLayout(activity)
            val keys = View(activity)
            input.addView(keys, FrameLayout.LayoutParams(MATCH_PARENT, 800, Gravity.BOTTOM))
            activity.setContentView(input)
            val window = activity.window
            val decor = window.decorView
            fun geometry(): List<Int> {
                decor.measure(View.MeasureSpec.makeMeasureSpec(1152, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2560, View.MeasureSpec.EXACTLY))
                decor.layout(0, 0, 1152, 2560)
                val position = IntArray(2)
                keys.getLocationInWindow(position)
                // IME derives reported content/touch insets from input height minus keyboard height.
                return listOf(input.height, input.height - keys.height, position[0], position[1],
                    decor.paddingLeft, decor.paddingTop, decor.paddingRight, decor.paddingBottom)
            }
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val baseline = geometry()
            // Blur refreshes on layout changes, rotations and theme changes; none may affect layout.
            for (top in listOf(1680, 1400, 600, 1680)) {
                window.setBackgroundDrawable(CupertinoBlurBackground(ColorDrawable(Color.GRAY), 0, top, 0, 96))
                assertEquals(baseline, geometry(), "Blur inset $top must only affect painting")
            }
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            assertEquals(baseline, geometry())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun blurBackgroundHasNoLayoutPaddingOrMinimumSize() {
        val background = CupertinoBlurBackground(ColorDrawable(Color.GRAY), 10, 1680, 20, 96)
        val padding = Rect(1, 2, 3, 4)
        assertFalse(background.getPadding(padding))
        assertEquals(Rect(), padding)
        assertEquals(0, background.minimumWidth)
        assertEquals(0, background.minimumHeight)
    }

    @Test fun blurOutlineStaysInsideKeyboardRegion() {
        val backdrop = GradientDrawable().apply { setColor(Color.GRAY); cornerRadius = 19f }
        val background = CupertinoBlurBackground(backdrop, 10, 1680, 20, 96)
        background.setBounds(0, 0, 1152, 2560)
        val outline = Outline()
        background.getOutline(outline)
        val rectangle = Rect()
        assertTrue(outline.getRect(rectangle))
        assertEquals(Rect(10, 1680, 1132, 2464), rectangle)
        assertEquals(19f, outline.radius)
    }
}
