// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.app.Application
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29, 36])
class CupertinoDefaultsTest {
    @Test fun firstInstallEnablesRussianAndEnglish() {
        val context = RuntimeEnvironment.getApplication()
        context.prefs().edit().clear().commit()
        Settings.init(context)
        SubtypeSettings.init(context)
        val saved = context.prefs().getString(Settings.PREF_ENABLED_SUBTYPES, "")!!
        assertEquals(setOf("ru", "en-US"), SubtypeSettings.createSettingsSubtypes(saved).map { it.locale.toLanguageTag() }.toSet())
        assertEquals("SWITCH_LANGUAGE", Defaults.PREF_SPACE_HORIZONTAL_SWIPE)
    }

    @Test fun returningToSystemLanguagesIsNotOverwrittenOnRestart() {
        val context = RuntimeEnvironment.getApplication()
        context.prefs().edit().clear().commit()
        Settings.init(context)
        SubtypeSettings.init(context)
        context.prefs().edit().putString(Settings.PREF_ENABLED_SUBTYPES, "").commit()
        SubtypeSettings.init(context)
        assertEquals("", context.prefs().getString(Settings.PREF_ENABLED_SUBTYPES, null))
    }
}
