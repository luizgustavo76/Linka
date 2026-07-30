package com.LinkaProject.linkaLite;

import android.graphics.Bitmap;

public class FriendItem {
    private int type;
    private String username;
    private String headerTitle;
    private int avatarResId;
    private Bitmap avatarBitmap;

    // Construtor para AMIGO apenas com nome
    public FriendItem(String username) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
    }

    // Construtor com BITMAP
    public FriendItem(String username, Bitmap avatarBitmap) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
        this.avatarBitmap = avatarBitmap;
    }

    // Construtor com DRAWABLE RES ID
    public FriendItem(String username, int avatarResId) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
        this.avatarResId = avatarResId;
    }

    // Construtor para CABEÇALHO (ex: new FriendItem("ONLINE", true))
    public FriendItem(String headerTitle, boolean isHeader) {
        if (isHeader) {
            this.type = FriendsAdapter.TYPE_HEADER;
            this.headerTitle = headerTitle;
        } else {
            this.type = FriendsAdapter.TYPE_FRIEND;
            this.username = headerTitle;
        }
    }

    public int getType() { return type; }
    public String getUsername() { return username; }
    public String getHeaderTitle() { return headerTitle; }
    public int getAvatarResId() { return avatarResId; }
    public Bitmap getAvatarBitmap() { return avatarBitmap; }
}