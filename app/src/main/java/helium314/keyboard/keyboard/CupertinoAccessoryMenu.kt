// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

/** Small key-style accessory palette; does not take focus from the editor. */
internal object CupertinoAccessoryMenu {
    /** Also allow the press-slide-release gesture used by character popups. */
    fun onAnchorTouch(popup: PopupWindow, event: MotionEvent): Boolean {
        if (!popup.isShowing) return false
        val items = listOf(R.id.cupertino_clipboard, R.id.cupertino_menu_hide)
            .map { popup.contentView.findViewById<View>(it) }
        val hit = items.firstOrNull { item ->
            val position = IntArray(2)
            item.getLocationOnScreen(position)
            event.rawX >= position[0] && event.rawX < position[0] + item.width &&
                event.rawY >= position[1] && event.rawY < position[1] + item.height
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> items.forEach { it.isPressed = it === hit }
            MotionEvent.ACTION_UP -> {
                items.forEach { it.isPressed = false }
                if (hit != null) { hit.performClick(); return true }
            }
            MotionEvent.ACTION_CANCEL -> { items.forEach { it.isPressed = false }; popup.dismiss() }
        }
        return false
    }

    fun show(anchor: View, clipboard: () -> Unit, hide: () -> Unit): PopupWindow {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val colors = Settings.getValues().mColors
        val foreground = colors.get(ColorType.KEY_TEXT)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
        }
        val popup = PopupWindow(row, dp(204), dp(88), false).apply {
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            elevation = dp(8).toFloat()
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(ColorUtils.setAlphaComponent(colors.get(ColorType.MAIN_BACKGROUND), 255))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), ColorUtils.setAlphaComponent(foreground, 35))
            })
        }
        fun action(id: Int, label: Int, icon: Int, callback: () -> Unit) {
            row.addView(LinearLayout(context).apply {
                this.id = id
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                contentDescription = context.getString(label)
                isClickable = true
                isFocusable = true
                val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                background = attributes.getDrawable(0)
                attributes.recycle()
                addView(ImageView(context).apply {
                    setImageResource(icon)
                    imageTintList = ColorStateList.valueOf(foreground)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(26), dp(26)))
                addView(TextView(context).apply {
                    setText(label)
                    setTextColor(foreground)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
                setOnClickListener { popup.dismiss(); callback() }
            }, LinearLayout.LayoutParams(dp(96), dp(76)))
        }
        action(R.id.cupertino_clipboard, R.string.cupertino_action_clipboard, R.drawable.sym_keyboard_clipboard_rounded, clipboard)
        action(R.id.cupertino_menu_hide, R.string.cupertino_action_hide, R.drawable.cupertino_hide, hide)
        popup.showAsDropDown(anchor, anchor.width - popup.width, -anchor.height - popup.height - dp(8), Gravity.LEFT)
        return popup
    }
}
