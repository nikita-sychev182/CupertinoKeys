// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.app.Application
import android.view.textservice.TextInfo
import helium314.keyboard.latin.spellcheck.AndroidSpellCheckerService
import helium314.keyboard.latin.spellcheck.AndroidSpellCheckerSession
import helium314.keyboard.latin.spellcheck.SentenceLevelAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class SpellCheckerSessionTest {
    private fun withSession(check: (AndroidSpellCheckerSession) -> Unit) {
        val service = Robolectric.buildService(AndroidSpellCheckerService::class.java).get()
        val session = AndroidSpellCheckerSession(service)
        // Android normally provides the locale through its internal binder session.
        // Supply just the splitter: these inputs must never reach dictionary lookup.
        AndroidSpellCheckerSession::class.java.getDeclaredField("mSentenceLevelAdapter").apply {
            isAccessible = true
            set(session, SentenceLevelAdapter(service.resources, Locale.US))
        }
        try { check(session) } finally { session.onClose() }
    }

    @Test fun punctuationOnlySentencesDoNotCrashCorrectionService() = withSession { session ->
        val result = session.onGetSentenceSuggestionsMultiple(arrayOf(TextInfo("."), TextInfo(","), TextInfo("...")), 3)
        assertEquals(3, result?.size)
        result!!.forEach { assertNull(it) }
    }

    @Test fun missingBatchEntryDoesNotCrashCorrectionService() = withSession { session ->
        val result = session.onGetSentenceSuggestionsMultiple(arrayOf(null, TextInfo("!")), 3)
        assertEquals(2, result?.size)
        result!!.forEach { assertNull(it) }
    }
}
