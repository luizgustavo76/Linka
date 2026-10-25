package com.LinkaProject.linkaLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "linkalite_cache.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_TEXT = "text_post";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_MEDIA_URL = "media_url";
    public static final String COLUMN_WIDTH = "width";
    public static final String COLUMN_HEIGHT = "height";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_POSTS_TABLE = "CREATE TABLE " + TABLE_POSTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_USERNAME + " TEXT, "
                + COLUMN_TEXT + " TEXT, "
                + COLUMN_DATETIME + " TEXT, "
                + COLUMN_MEDIA_URL + " TEXT, "
                + COLUMN_WIDTH + " TEXT, "
                + COLUMN_HEIGHT + " TEXT)";
        db.execSQL(CREATE_POSTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    public static boolean doesDatabaseExist(Context context) {
        java.io.File dbFile = context.getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
    }

    public boolean hasCachedPosts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_POSTS, null);
        boolean hasPosts = false;
        if (cursor.moveToFirst()) {
            hasPosts = cursor.getInt(0) > 0;
        }
        cursor.close();
        db.close();
        return hasPosts;
    }

    public void insertPost(int id, String username, String textPost, String datetime, String mediaUrl, String width, String height) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_TEXT, textPost);
        values.put(COLUMN_DATETIME, datetime);
        values.put(COLUMN_MEDIA_URL, mediaUrl);
        values.put(COLUMN_WIDTH, width);
        values.put(COLUMN_HEIGHT, height);

        db.insertWithOnConflict(TABLE_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public int getLastPostId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_ID + ") FROM " + TABLE_POSTS, null);
        int lastId = 0;
        if (cursor.moveToFirst()) {
            lastId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return lastId;
    }

    public ArrayList<JSONObject> getAllCachedPosts() {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_POSTS + " ORDER BY " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    JSONObject post = new JSONObject();
                    post.put("id", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                    post.put("username", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                    post.put("text_post", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                    post.put("datetime", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATETIME)));

                    String mUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_URL));
                    String w = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WIDTH));
                    String h = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT));

                    if (mUrl != null && mUrl.length() > 0) {
                        JSONObject mediaObj = new JSONObject();
                        mediaObj.put("media_url", mUrl);
                        mediaObj.put("media_type", "image");
                        mediaObj.put("width", w != null ? w : "");
                        mediaObj.put("heigth", h != null ? h : "");

                        post.put("media", mediaObj);

                        JSONArray mediaList = new JSONArray();
                        mediaList.put(mediaObj);
                        post.put("media_list", mediaList);
                        post.put("media_attachments", mediaList);
                    }

                    list.add(post);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }
}