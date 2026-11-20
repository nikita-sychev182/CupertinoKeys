// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import java.io.File;

/** Not exported and never grants URI permissions. All consumers live in this app. */
public final class LocalDictionaryProvider extends ContentProvider {
    private SQLiteOpenHelper helper;
    private static final String TABLE = "words";

    @Override public boolean onCreate() {
        Context context = getContext();
        if (context == null) return false;
        // Also exclude the database independently of the manifest's backup policy.
        String path = new File(context.getNoBackupFilesDir(), "personal_dictionary.db").getPath();
        helper = new SQLiteOpenHelper(context, path, null, 1) {
            @Override public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE words (_id INTEGER PRIMARY KEY, word TEXT NOT NULL, "
                        + "frequency INTEGER NOT NULL DEFAULT 250, locale TEXT, shortcut TEXT)");
                db.execSQL("CREATE INDEX words_locale ON words(locale)");
            }
            @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                throw new IllegalStateException("Dictionary migration required");
            }
        };
        return true;
    }

    private void checkUri(Uri uri) {
        if (!LocalUserDictionary.Words.CONTENT_URI.equals(uri))
            throw new IllegalArgumentException("Unsupported dictionary URI");
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        checkUri(uri);
        Cursor cursor = helper.getReadableDatabase().query(
                TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        checkUri(uri);
        if (values == null || values.getAsString("word") == null
                || values.getAsString("word").trim().isEmpty())
            throw new IllegalArgumentException("A dictionary word is required");
        SQLiteDatabase db = helper.getWritableDatabase();
        long id;
        db.beginTransaction();
        try {
            // IS handles NULL locales (all languages) and NULL shortcuts correctly.
            db.delete(TABLE, "word=? AND locale IS ? AND shortcut IS ?", new String[] {
                    values.getAsString("word"), values.getAsString("locale"),
                    values.getAsString("shortcut")});
            id = db.insertOrThrow(TABLE, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        checkUri(uri);
        int count = helper.getWritableDatabase().delete(TABLE, selection, selectionArgs);
        if (count > 0) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkUri(uri);
        int count = helper.getWritableDatabase().update(TABLE, values, selection, selectionArgs);
        if (count > 0) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override public String getType(Uri uri) {
        checkUri(uri);
        return "vnd.android.cursor.dir/vnd.cupertinokeys.word";
    }

    @Override public void shutdown() {
        if (helper != null) helper.close();
        super.shutdown();
    }
}
