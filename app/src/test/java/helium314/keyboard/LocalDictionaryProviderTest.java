// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import helium314.keyboard.latin.dictionary.LocalDictionaryProvider;
import helium314.keyboard.latin.dictionary.LocalUserDictionary.Words;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 36})
public class LocalDictionaryProviderTest {
    private LocalDictionaryProvider provider;
    private Context context;

    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        provider = Robolectric.buildContentProvider(LocalDictionaryProvider.class).create().get();
        provider.delete(Words.CONTENT_URI, null, null);
    }

    @After public void close() {
        provider.shutdown();
    }

    @Test public void wordsStaySeparateByLanguageAndNullLocale() {
        Words.addWord(context, "test", 250, null, Locale.US);
        Words.addWord(context, "test", 200, null, Locale.forLanguageTag("ru"));
        Words.addWord(context, "test", 100, null, null);
        assertEquals(3, count(null, null));
        assertEquals(1, count("locale=?", new String[] {"en_US"}));
        assertEquals(1, count("locale IS NULL", null));
        provider.delete(Words.CONTENT_URI, "word=? AND locale=?", new String[] {"test", "ru"});
        assertEquals(2, count(null, null));
    }

    @Test public void readdingSameWordUpdatesInsteadOfDuplicating() {
        Words.addWord(context, "hello", 40, null, null);
        Words.addWord(context, "hello", 90, "", null);
        assertEquals(1, count(null, null));
        try (Cursor c = provider.query(Words.CONTENT_URI, new String[] {Words.FREQUENCY}, null, null, null)) {
            assertTrue(c.moveToFirst());
            assertEquals(90, c.getInt(0));
        }
    }

    @Test public void blankWordsAreIgnoredAndFrequencyIsClamped() {
        Words.addWord(context, "  ", 250, null, Locale.US);
        assertEquals(0, count(null, null));
        Words.addWord(context, "hello", 1000, "hi", Locale.US);
        try (Cursor c = provider.query(Words.CONTENT_URI, new String[] {Words.FREQUENCY, Words.SHORTCUT}, null, null, null)) {
            assertTrue(c.moveToFirst());
            assertEquals(255, c.getInt(0));
            assertEquals("hi", c.getString(1));
        }
    }

    @Test public void dictionaryPersistsWhenProviderRestarts() {
        Words.addWord(context, "persistent", 250, null, Locale.US);
        provider.shutdown();
        provider = Robolectric.buildContentProvider(LocalDictionaryProvider.class).create().get();
        assertEquals(1, count("word=?", new String[] {"persistent"}));
        assertTrue(new java.io.File(provider.getContext().getNoBackupFilesDir(), "personal_dictionary.db").isFile());
    }

    @Test public void systemDictionaryUriIsRejected() {
        assertNotEquals("user_dictionary", Words.CONTENT_URI.getAuthority());
        assertThrows(IllegalArgumentException.class, () -> provider.query(
                Uri.parse("content://user_dictionary/words"), null, null, null, null));
    }

    private int count(String selection, String[] args) {
        try (Cursor c = provider.query(Words.CONTENT_URI, null, selection, args, null)) {
            return c.getCount();
        }
    }
}
