// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

/**
 * Positions the blur outline without contributing padding or minimum size to the IME window.
 * InsetDrawable must not be used here: DecorView treats its paint insets as layout padding,
 * shrinking the full-height input view and breaking reported content/touch insets.
 */
internal class CupertinoBlurBackground(
    private val backdrop: Drawable,
    private val left: Int,
    private val top: Int,
    private val right: Int,
    private val bottom: Int
) : Drawable() {
    override fun onBoundsChange(bounds: Rect) {
        val area = Rect(bounds.left + left, bounds.top + top, bounds.right - right, bounds.bottom - bottom)
        if (area.isEmpty) area.setEmpty()
        backdrop.bounds = area
    }

    override fun draw(canvas: Canvas) {
        if (!backdrop.bounds.isEmpty) backdrop.draw(canvas)
    }

    override fun getOutline(outline: Outline) {
        if (backdrop.bounds.isEmpty) outline.setEmpty() else backdrop.getOutline(outline)
    }

    override fun setAlpha(alpha: Int) {
        backdrop.alpha = alpha
        invalidateSelf()
    }

    override fun getAlpha() = backdrop.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backdrop.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Drawable API")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
