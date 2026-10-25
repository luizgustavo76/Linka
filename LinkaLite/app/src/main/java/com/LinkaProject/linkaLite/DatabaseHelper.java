package com.LinkaProject.linkaLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "linkalite_cache.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_TEXT = "text_post";
    public static final String COLUMN_DATETIME = "datetime";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_POSTS_TABLE = "CREATE TABLE " + TABLE_POSTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_USERNAME + " TEXT, "
                + COLUMN_TEXT + " TEXT, "
                + COLUMN_DATETIME + " TEXT)";
        db.execSQL(CREATE_POSTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    // Verifica se o ficheiro do banco de dados física no disco existe
    public static boolean doesDatabaseExist(Context context) {
        java.io.File dbFile = context.getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
    }

    // Verifica se existem posts salvos na tabela de cache
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

    // Inserir ou atualizar post no Cache
    public void insertPost(int id, String username, String textPost, String datetime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_TEXT, textPost);
        values.put(COLUMN_DATETIME, datetime);

        db.insertWithOnConflict(TABLE_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Obter o ID do post mais recente salvo (retorna 0 se estiver vazio)
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
}