package com.LinkaProject.linkaLite;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "linka_feeds.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE feeds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL);");
        db.execSQL("CREATE TABLE feed_sources (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "feed_id INTEGER, " +
                "network TEXT NOT NULL, " +
                "source_identifier TEXT NOT NULL, " +
                "FOREIGN KEY(feed_id) REFERENCES feeds(id) ON DELETE CASCADE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS feed_sources");
        db.execSQL("DROP TABLE IF EXISTS feeds");
        onCreate(db);
    }
}