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
    // Aumentamos a versão para forçar a atualização da estrutura
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_TEXT = "text_post";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_RELATIVE_DATETIME = "relative_datetime";
    public static final String COLUMN_COMMENT_COUNT = "comment_count";
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
                + COLUMN_RELATIVE_DATETIME + " TEXT, "
                + COLUMN_COMMENT_COUNT + " INTEGER DEFAULT 0, "
                + COLUMN_MEDIA_URL + " TEXT, "
                + COLUMN_WIDTH + " TEXT, "
                + COLUMN_HEIGHT + " TEXT)";
        db.execSQL(CREATE_POSTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Se a versão mudou, recria a tabela com as novas colunas
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
        return hasPosts;
    }

    // Insere ou Atualiza posts existente mantendo os contadores em dia
    public void insertPostsBatch(JSONArray postsArray) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject obj = postsArray.getJSONObject(i);
                int pId = obj.optInt("id", 0);
                if (pId <= 0) continue;

                String uName = obj.optString("username", "");
                String txt = obj.optString("text_post", "");
                String dt = obj.optString("datetime", "");
                String relDt = obj.optString("relative_datetime", "");
                int comments = obj.optInt("comment_count", obj.optInt("comments_count", 0));
                
                String mUrl = "";
                String w = "";
                String h = "";

                JSONArray mediaList = obj.optJSONArray("media_list");
                if (mediaList != null && mediaList.length() > 0) {
                    JSONObject mObj = mediaList.optJSONObject(0);
                    if (mObj != null) {
                        mUrl = mObj.optString("media_url", "");
                        w = mObj.optString("width", "");
                        h = mObj.optString("heigth", "");
                    }
                } else {
                    JSONObject singleMedia = obj.optJSONObject("media");
                    if (singleMedia != null) {
                        mUrl = singleMedia.optString("media_url", "");
                        w = singleMedia.optString("width", "");
                        h = singleMedia.optString("heigth", "");
                    }
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_ID, pId);
                values.put(COLUMN_USERNAME, uName);
                values.put(COLUMN_TEXT, txt);
                values.put(COLUMN_DATETIME, dt);
                values.put(COLUMN_RELATIVE_DATETIME, relDt);
                values.put(COLUMN_COMMENT_COUNT, comments);
                values.put(COLUMN_MEDIA_URL, mUrl);
                values.put(COLUMN_WIDTH, w);
                values.put(COLUMN_HEIGHT, h);

                // CONFLICT_REPLACE garante que, se o ID já existir no SQLite,
                // ele atualiza o comentário e os dados sem duplicar nem dar erro!
                db.insertWithOnConflict(TABLE_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public int getLastPostId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_ID + ") FROM " + TABLE_POSTS, null);
        int lastId = 0;
        if (cursor.moveToFirst()) {
            lastId = cursor.getInt(0);
        }
        cursor.close();
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
                    post.put("relative_datetime", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RELATIVE_DATETIME)));
                    post.put("comment_count", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_COUNT)));

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
        return list;
    }
}