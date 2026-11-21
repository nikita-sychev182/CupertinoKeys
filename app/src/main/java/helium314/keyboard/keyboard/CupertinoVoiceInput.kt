// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings

/** Google recognition with our own compact UI. No keyboard switch or stored audio. */
object CupertinoVoiceInput {
    private var active: Session? = null

    internal fun googleService(context: Context): ComponentName? =
        context.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
            .mapNotNull { it.serviceInfo }
            .firstOrNull { it.exported && it.packageName in setOf("com.google.android.tts", "com.google.android.googlequicksearchbox") }
            ?.let { ComponentName(it.packageName, it.name) }

    /** False means no supported recognizer: the caller may offer the external voice IME. */
    fun start(ime: LatinIME): Boolean {
        if (!GoogleVoiceInput.isAllowed(Settings.getValues())) return true
        active?.let { it.stop(); return true }
        val service = googleService(ime) ?: return false
        if (Build.VERSION.SDK_INT >= 23 && ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            try {
                ime.startActivity(Intent(ime, VoicePermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: SecurityException) {
                Toast.makeText(ime, R.string.cupertino_voice_not_permitted, Toast.LENGTH_LONG).show()
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(ime, R.string.cupertino_voice_unavailable, Toast.LENGTH_LONG).show()
            }
            return true
        }
        val root = ime.window?.window?.decorView ?: return true
        val anchor = root.findViewById<View>(R.id.cupertino_voice)?.takeIf { it.isShown }
            ?: root.findViewById<View>(R.id.main_keyboard_frame) ?: return true
        CupertinoNavigationBar.dismissMenu()
        val session = Session(ime, anchor)
        active = session
        session.start(service)
        return true
    }

    @JvmStatic fun cancel() { active?.close() }
    internal fun panel(): View? = active?.popup?.contentView

    private class Session(private val ime: LatinIME, private val anchor: View) : RecognitionListener {
        private val connection = ime.currentInputConnection
        private fun dp(value: Int) = (value * anchor.resources.displayMetrics.density).toInt()
        private val palette = Settings.getValues().mColors
        private val ink = palette.get(ColorType.KEY_TEXT)
        private var recognizer: SpeechRecognizer? = null
        private var closed = false
        private var finishing = false
        private val timeout = Runnable { stop() }
        private val dismiss = Runnable { close() }
        private val status = TextView(anchor.context).apply {
            textSize = 16f
            setTextColor(ink)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setText(R.string.cupertino_voice_connecting)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val popup: PopupWindow

        init {
            val row = LinearLayout(anchor.context).apply {
                id = R.id.cupertino_voice_panel
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            }
            fun button(symbol: String, label: Int, action: () -> Unit) = TextView(anchor.context).apply {
                text = symbol
                textSize = 27f
                gravity = Gravity.CENTER
                setTextColor(ink)
                contentDescription = ime.getString(label)
                isFocusable = true
                setOnClickListener { action() }
            }
            row.addView(button("×", android.R.string.cancel) { close() }, LinearLayout.LayoutParams(dp(48), dp(56)))
            row.addView(LinearLayout(anchor.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(anchor.context).apply {
                    text = "Google · ${RichInputMethodManager.getInstance().currentSubtype.locale.displayLanguage}"
                    textSize = 11f
                    setTextColor(ColorUtils.setAlphaComponent(ink, 170))
                })
                addView(status)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            row.addView(button("✓", R.string.cupertino_voice_done) { stop() }, LinearLayout.LayoutParams(dp(48), dp(56)))
            val width = minOf(dp(360), anchor.rootView.width - dp(24)).coerceAtLeast(dp(200))
            popup = PopupWindow(row, width, dp(80), false).apply {
                isOutsideTouchable = true
                inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
                elevation = dp(8).toFloat()
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(ColorUtils.setAlphaComponent(palette.get(ColorType.MAIN_BACKGROUND), 255))
                    cornerRadius = dp(20).toFloat()
                    setStroke(dp(1), ColorUtils.setAlphaComponent(ink, 35))
                })
                setOnDismissListener { close() }
            }
        }

        fun show() {
            popup.showAsDropDown(anchor, anchor.width - popup.width,
                -anchor.height - popup.height - dp(8), Gravity.LEFT)
        }

        fun start(service: ComponentName) {
            try {
                show()
                recognizer = SpeechRecognizer.createSpeechRecognizer(ime, service).also {
                    it.setRecognitionListener(this)
                    it.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, RichInputMethodManager.getInstance().currentSubtype.locale.toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    })
                }
                anchor.postDelayed(timeout, 30_000)
            } catch (_: RuntimeException) { onError(SpeechRecognizer.ERROR_CLIENT) }
        }

        fun stop() {
            if (closed) return
            if (finishing) { close(); return }
            finishing = true
            status.setText(R.string.cupertino_voice_processing)
            anchor.removeCallbacks(timeout)
            anchor.postDelayed(dismiss, 5_000)
            runCatching { recognizer?.stopListening() }
        }

        fun close() {
            if (closed) return
            closed = true
            if (active === this) active = null
            anchor.removeCallbacks(timeout)
            anchor.removeCallbacks(dismiss)
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
            recognizer = null
            popup.dismiss()
        }

        override fun onReadyForSpeech(params: Bundle?) { if (!closed) status.setText(R.string.cupertino_voice_listening) }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onEndOfSpeech() {
            if (closed) return
            finishing = true
            status.setText(R.string.cupertino_voice_processing)
            anchor.removeCallbacks(timeout)
            anchor.postDelayed(dismiss, 5_000)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            if (closed || finishing) return
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let { status.text = it }
        }
        override fun onResults(results: Bundle?) {
            if (closed) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
            val mayCommit = ime.isInputViewShown && connection != null && ime.currentInputConnection === connection &&
                GoogleVoiceInput.isAllowed(Settings.getValues())
            close()
            if (mayCommit && !text.isNullOrEmpty()) ime.onTextInput(text)
        }
        override fun onError(error: Int) {
            if (closed) return
            finishing = true
            anchor.removeCallbacks(timeout)
            runCatching { recognizer?.destroy() }
            recognizer = null
            status.setText(when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.cupertino_voice_no_speech
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.cupertino_voice_permission_required
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.cupertino_voice_network_error
                else -> R.string.cupertino_voice_unavailable
            })
            anchor.removeCallbacks(dismiss)
            anchor.postDelayed(dismiss, 3_000)
        }
    }
}
