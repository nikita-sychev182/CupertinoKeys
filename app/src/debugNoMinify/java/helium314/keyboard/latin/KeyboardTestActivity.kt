// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.latin.dictionary.LocalUserDictionary.Words
import java.util.Locale

/** Disposable input fields for physical-device tests; absent from release builds. */
class KeyboardTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            if (android.os.Build.VERSION.SDK_INT >= 30) setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding)
                insets
            }
        }
        root.addView(TextView(this).apply {
            text = "Cupertino Keys · device test"
            textSize = 20f
        })
        val editor = EditText(this).apply {
            contentDescription = "test_editor"
            hint = "Disposable test text"
            textSize = 22f
            minLines = 2
            if (android.os.Build.VERSION.SDK_INT >= 26) setAutofillHints(null)
        }
        val result = TextView(this).apply { contentDescription = "test_result" }
        var password = false
        fun configure(type: Int, noLearning: Boolean = false) {
            password = type and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_PASSWORD
            editor.inputType = type
            editor.imeOptions = if (noLearning) EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING else EditorInfo.IME_ACTION_DONE
            editor.text.clear()
            editor.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.restartInput(editor)
            editor.post { imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT) }
        }
        fun row(vararg modes: Pair<String, () -> Unit>) {
            root.addView(LinearLayout(this).apply {
                modes.forEach { (label, action) ->
                    addView(Button(this@KeyboardTestActivity).apply {
                        text = label
                        textSize = 11f
                        isAllCaps = false
                        setOnClickListener { action() }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }
            })
        }
        row(
            "Text" to { configure(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE) },
            "Email" to { configure(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) },
            "Number" to { configure(InputType.TYPE_CLASS_NUMBER) }
        )
        row(
            "Password" to { configure(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) },
            "No learning" to { configure(InputType.TYPE_CLASS_TEXT, true) },
            "Clear" to { editor.text.clear() }
        )
        fun reportDictionary() {
            contentResolver.query(Words.CONTENT_URI, arrayOf(Words.WORD),
                "word=? AND locale=?", arrayOf("cupertinotestword", "en_US"), null).use {
                result.text = "Test dictionary word count: ${it?.count ?: -1}"
            }
        }
        row(
            "Add test word" to {
                Words.addWord(this, "cupertinotestword", 250, "ctw", Locale.US)
                reportDictionary()
            },
            "Check word" to { reportDictionary() },
            "Delete test word" to {
                contentResolver.delete(Words.CONTENT_URI, "word=? AND locale=?",
                    arrayOf("cupertinotestword", "en_US"))
                reportDictionary()
            }
        )
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                result.text = if (password) "Password: ${s?.length ?: 0} chars" else "Text: [$s]"
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        root.addView(editor)
        root.addView(result)
        setContentView(root)
        configure(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Only repair the initial launch. Restarting input after an IME-owned dialog
        // closes resets its selected panel and makes history/picker tests misleading.
        if (hasFocus && !initialFocusHandled) (currentFocus as? EditText)?.let {
            initialFocusHandled = true
            it.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.restartInput(it)
                imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }, 350)
        }
    }

    /** Deliberately high-frequency, non-private content to verify cross-window blur visually. */
    fun setBackdropProbe(warm: Boolean) {
        window.setBackgroundDrawable(object : android.graphics.drawable.Drawable() {
            val paint = android.graphics.Paint()
            override fun draw(canvas: android.graphics.Canvas) {
                val stripe = 8f * resources.displayMetrics.density
                var x = 0f
                var index = 0
                while (x < bounds.width()) {
                    paint.color = if (index++ % 2 == 0) {
                        if (warm) 0xFFE83868.toInt() else 0xFF2467E8.toInt()
                    } else {
                        if (warm) 0xFFFFCDD8.toInt() else 0xFFB8D5FF.toInt()
                    }
                    canvas.drawRect(x, 0f, x + stripe, bounds.height().toFloat(), paint)
                    x += stripe
                }
            }
            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(filter: android.graphics.ColorFilter?) { paint.colorFilter = filter }
            @Deprecated("Drawable API") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
        })
    }

    private var initialFocusHandled = false
}
