// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import helium314.keyboard.latin.BuildConfig;
import java.util.Locale;

/** App-private replacement for Android's shared, potentially synced user dictionary. */
public final class LocalUserDictionary {
    private LocalUserDictionary() {}

    public static final class Words implements BaseColumns {
        private Words() {}
        public static final Uri CONTENT_URI = Uri.parse(
                "content://" + BuildConfig.APPLICATION_ID + ".dictionary/words");
        public static final String WORD = "word";
        public static final String SHORTCUT = "shortcut";
        public static final String FREQUENCY = "frequency";
        public static final String LOCALE = "locale";

        public static void addWord(Context context, String word, int frequency,
                String shortcut, Locale locale) {
            if (word == null || word.trim().isEmpty()) return;
            ContentValues values = new ContentValues();
            values.put(WORD, word);
            values.put(FREQUENCY, Math.max(0, Math.min(255, frequency)));
            values.put(SHORTCUT, shortcut == null || shortcut.trim().isEmpty() ? null : shortcut);
            values.put(LOCALE, locale == null ? null : locale.toString());
            context.getContentResolver().insert(CONTENT_URI, values);
        }
    }
}
