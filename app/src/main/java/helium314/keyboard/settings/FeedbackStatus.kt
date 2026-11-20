// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.latin.R
import helium314.keyboard.settings.preferences.Preference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Explain why enabled feedback can be silent; never alter the phone's sound policy. */
@Composable
fun FeedbackStatus() {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var reason by remember { mutableIntStateOf(0) }
    LaunchedEffect(context) {
        while (isActive) {
            reason = when {
                android.provider.Settings.Global.getInt(context.contentResolver, "zen_mode", 0) != 0 ->
                    R.string.cupertino_feedback_dnd
                audio.ringerMode != AudioManager.RINGER_MODE_NORMAL -> R.string.cupertino_feedback_silent
                else -> 0
            }
            delay(1000)
        }
    }
    if (reason != 0) Preference(
        name = stringResource(reason),
        description = stringResource(R.string.cupertino_feedback_sound_settings),
        onClick = {
            val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
            if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
        }
    )
}
