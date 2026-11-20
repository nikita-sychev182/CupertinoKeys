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
            GlassKeyDrawable(base.get(color), dark, density)
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

private class GlassKeyDrawable(private val tint: Int, private val dark: Boolean, private val density: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = RectF()
    private var pressed = false
    private var opacity = 255
    private var filter: ColorFilter? = null
    private var surface: Shader? = null
    private var rim: Shader? = null

    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        val next = android.R.attr.state_pressed in state
        if (next == pressed) return false
        pressed = next
        shaders()
        invalidateSelf()
        return true
    }
    override fun onBoundsChange(bounds: Rect) = shaders()
    private fun shaders() {
        val base = if (pressed) ColorUtils.blendARGB(tint, if (dark) Color.WHITE else Color.BLACK, 0.16f) else tint
        surface = LinearGradient(0f, bounds.top.toFloat(), 0f, bounds.bottom.toFloat(),
            ColorUtils.blendARGB(base, Color.WHITE, if (dark) 0.035f else 0.18f),
            ColorUtils.blendARGB(base, if (dark) Color.BLACK else Color.WHITE, 0.02f), Shader.TileMode.CLAMP)
        rim = LinearGradient(0f, bounds.top.toFloat(), bounds.width() * 0.4f, bounds.bottom.toFloat(),
            if (dark) 0x20FFFFFF else 0x85FFFFFF.toInt(), if (dark) 0x04FFFFFF else 0x18FFFFFF, Shader.TileMode.CLAMP)
    }
    override fun draw(canvas: Canvas) {
        shape.set(bounds)
        shape.inset(0.4f * density, 0.4f * density)
        shape.bottom -= 0.7f * density
        val radius = minOf(6f * density, shape.width() * 0.20f)
        paint.alpha = opacity
        paint.colorFilter = filter
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = if (dark) 0x35000000 else 0x1C303746
        paint.alpha = Color.alpha(paint.color) * opacity / 255
        canvas.save()
        canvas.translate(0f, 0.8f * density)
        canvas.drawRoundRect(shape, radius, radius, paint)
        canvas.restore()
        paint.color = Color.WHITE
        paint.alpha = opacity
        paint.shader = surface
        canvas.drawRoundRect(shape, radius, radius, paint)
        paint.shader = rim
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.4f * density
        canvas.drawRoundRect(shape, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
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
            ColorUtils.setAlphaComponent(if (dark) 0xFF19191B.toInt() else 0xFFE2E5EB.toInt(), alpha),
            ColorUtils.setAlphaComponent(if (dark) 0xFF141416.toInt() else 0xFFD4DBE5.toInt(), alpha), Shader.TileMode.CLAMP)
        coolLight = RadialGradient(w * 0.12f, h * 0.65f, w * 0.7f,
            if (dark) 0x066D7580 else 0x455EADE2, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        warmLight = RadialGradient(w * 0.95f, h * 0.25f, w * 0.65f,
            if (dark) 0x057C7482 else 0x35E5BDCF, Color.TRANSPARENT, Shader.TileMode.CLAMP)
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
        paint.color = if (dark) 0x10FFFFFF else 0x60FFFFFF
        paint.alpha = Color.alpha(paint.color) * opacity / 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.7f * density
        canvas.drawPath(clip, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { opacity = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
