// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.SettingsValues
import java.util.Locale

/** Compact Google recognition, with external voice IME fallback if no public service exists. */
object GoogleVoiceInput {
    private val googlePackages = setOf("com.google.android.tts", "com.google.android.googlequicksearchbox")

    @JvmStatic fun isAllowed(settings: SettingsValues): Boolean =
        settings.mShowsVoiceInputKey && !settings.mIncognitoModeEnabled

    @JvmStatic fun enabledIme(context: Context): InputMethodInfo? {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return manager.enabledInputMethodList.firstOrNull { info ->
            info.packageName in googlePackages &&
                (0 until info.subtypeCount).any { info.getSubtypeAt(it).mode == "voice" }
        }
    }

    @JvmStatic fun start(ime: LatinIME) {
        if (!isAllowed(Settings.getValues())) return
        if (CupertinoVoiceInput.start(ime)) return
        val target = enabledIme(ime)
        if (target == null) {
            Toast.makeText(ime, R.string.cupertino_enable_google_voice, Toast.LENGTH_LONG).show()
            return
        }
        val language = RichInputMethodManager.getInstance().currentSubtype.locale.language
        val subtype = (0 until target.subtypeCount).map { target.getSubtypeAt(it) }.firstOrNull {
            @Suppress("DEPRECATION")
            it.mode == "voice" && Locale.forLanguageTag(it.locale.replace('_', '-')).language == language
        }
        try {
            if (Build.VERSION.SDK_INT >= 28 && subtype != null) ime.switchInputMethod(target.id, subtype)
            else ime.switchInputMethod(target.id)
        } catch (_: IllegalArgumentException) {
            Toast.makeText(ime, R.string.cupertino_enable_google_voice, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(ime, R.string.cupertino_voice_not_permitted, Toast.LENGTH_LONG).show()
        }
    }
}
