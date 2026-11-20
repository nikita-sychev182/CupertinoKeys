// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.common

import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.ColorUtils

/** Frosted material fallback: entirely local rendering, never samples other apps. */
class CupertinoColors(private val base: Colors, val dark: Boolean, private val density: Float) : Colors by base {
    fun setSurface(view: View, nativeBlur: Boolean) {
        view.background = FrostedSurfaceDrawable(dark, density, nativeBlur)
    }
    override fun selectAndColorDrawable(attr: TypedArray, color: ColorType): Drawable = when (color) {
        ColorType.KEY_BACKGROUND, ColorType.FUNCTIONAL_KEY_BACKGROUND, ColorType.SPACE_BAR_BACKGROUND,
        ColorType.ACTION_KEY_BACKGROUND, ColorType.POPUP_KEYS_BACKGROUND,
        ColorType.ACTION_KEY_POPUP_KEYS_BACKGROUND, ColorType.MORE_SUGGESTIONS_WORD_BACKGROUND ->
            FlatKeyDrawable(base.get(color), dark, density)
        else -> base.selectAndColorDrawable(attr, color)
    }

    override fun setBackground(view: View, color: ColorType) {
        // MIUI's automatic darkening must not recolor an explicitly selected palette.
        if (android.os.Build.VERSION.SDK_INT >= 29) view.isForceDarkAllowed = false
        when (color) {
            ColorType.MAIN_BACKGROUND -> view.background = FrostedSurfaceDrawable(dark, density)
            ColorType.STRIP_BACKGROUND -> view.setBackgroundColor(Color.TRANSPARENT)
            else -> base.setBackground(view, color)
        }
    }
}

/** A flat translucent fill: the shared material supplies depth, not a bevel per key. */
private class FlatKeyDrawable(private val tint: Int, private val dark: Boolean, private val density: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = RectF()
    private var pressed = false
    private var opacity = 255
    private var filter: ColorFilter? = null

    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        val next = android.R.attr.state_pressed in state
        if (next == pressed) return false
        pressed = next
        invalidateSelf()
        return true
    }
    override fun draw(canvas: Canvas) {
        shape.set(bounds)
        shape.inset(0.4f * density, 0.4f * density)
        val radius = minOf(6f * density, shape.width() * 0.20f)
        paint.colorFilter = filter
        paint.style = Paint.Style.FILL
        paint.color = if (pressed) ColorUtils.blendARGB(tint, if (dark) Color.WHITE else Color.BLACK, 0.16f) else tint
        // Let the locally drawn backdrop subtly tint the keys. No shadow, raised edge,
        // highlight, or vertical gradient: these made the keys look like plastic buttons.
        paint.alpha = Color.alpha(paint.color) * opacity / 255 * (if (dark) 205 else 244) / 255
        canvas.drawRoundRect(shape, radius, radius, paint)
    }
    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { filter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
}

private class FrostedSurfaceDrawable(private val dark: Boolean, private val density: Float, private val nativeBlur: Boolean = false) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private var base: Shader? = null
    private var coolLight: Shader? = null
    private var warmLight: Shader? = null
    private var opacity = 255
    override fun onBoundsChange(bounds: Rect) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        clip.reset()
        val radius = 10f * density
        clip.addRoundRect(RectF(bounds), floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), Path.Direction.CW)
        val alpha = if (nativeBlur) 120 else 255
        base = LinearGradient(0f, 0f, 0f, h,
            ColorUtils.setAlphaComponent(if (dark) 0xFF25262B.toInt() else 0xFFE0E1E6.toInt(), alpha),
            ColorUtils.setAlphaComponent(if (dark) 0xFF22252C.toInt() else 0xFFD7DAE1.toInt(), alpha), Shader.TileMode.CLAMP)
        // Broad low-contrast tones suggest frosted material without colored spotlights.
        // This is an opaque fallback, not a sample of application content underneath.
        coolLight = RadialGradient(w * 0.08f, h * 0.58f, w * 0.9f,
            if (dark) 0x263F626B else 0x187B9CAB, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        warmLight = RadialGradient(w * 0.95f, h * 0.85f, w * 0.8f,
            if (dark) 0x24796C88 else 0x169D8DA8, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    }
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clip)
        paint.color = Color.WHITE
        paint.alpha = opacity
        for (shader in arrayOf(base, coolLight, warmLight)) {
            paint.shader = shader
            canvas.drawRect(bounds, paint)
        }
        paint.shader = null
        paint.color = if (dark) 0x08FFFFFF else 0x20FFFFFF
        paint.alpha = Color.alpha(paint.color) * opacity / 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.4f * density
        canvas.drawPath(clip, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
