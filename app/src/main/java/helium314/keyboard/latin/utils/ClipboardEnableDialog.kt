// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.app.AlertDialog
import android.os.IBinder
import android.view.WindowManager
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings

fun createClipboardEnableDialog(ime: LatinIME, token: IBinder, onEnabled: Runnable): AlertDialog {
    val dialog = AlertDialog.Builder(getPlatformDialogThemeContext(ime))
        .setTitle(R.string.enable_clipboard_history)
        .setMessage(R.string.cupertino_clipboard_enable_message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.cupertino_enable) { _, _ ->
            ime.prefs().edit().putBoolean(Settings.PREF_ENABLE_CLIPBOARD_HISTORY, true).apply()
            onEnabled.run()
        }.create()
    dialog.window?.apply {
        attributes = attributes.apply {
            this.token = token
            type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        }
        addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }
    return dialog
}
