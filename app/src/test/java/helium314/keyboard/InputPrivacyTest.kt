// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.app.Application
import android.text.InputType
import android.view.inputmethod.EditorInfo
import helium314.keyboard.latin.InputAttributes
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29, 36])
class InputPrivacyTest {
    private fun editor(options: Int = 0) = EditorInfo().apply {
        inputType = InputType.TYPE_CLASS_TEXT
        packageName = "dev.cupertinokeys.test"
        imeOptions = options
    }

    @Test fun privacyFlagChangeReloadsSettingsInBothDirections() {
        val ordinary = editor()
        val private = editor(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        val ordinaryAttributes = InputAttributes(ordinary, false, "test")
        val privateAttributes = InputAttributes(private, false, "test")
        assertFalse(ordinaryAttributes.isSameInputType(private))
        assertFalse(privateAttributes.isSameInputType(ordinary))
        assertTrue(privateAttributes.mNoLearning)
        assertTrue(privateAttributes.isSameInputType(private))
    }

    @Test fun reusedEditorInfoCannotHidePrivacyFlagChange() {
        val editor = editor()
        val previous = InputAttributes(editor, false, "test")
        editor.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        assertFalse(previous.isSameInputType(editor))
    }
}
