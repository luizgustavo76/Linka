package com.LinkaProject.linkaLite;

import android.graphics.Bitmap;

public class FriendItem {
    private int type;
    private String username;
    private String headerTitle;
    private int avatarResId;
    private Bitmap avatarBitmap;
    private int groupId;
    private String permissions;
    public FriendItem(String username) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
    }
    public FriendItem(int groupId, String groupName, String permissions) {
        this.type = FriendsAdapter.TYPE_GROUP;
        this.groupId = groupId;
        this.username = groupName;
        this.permissions = permissions;
    }

    public FriendItem(String username, Bitmap avatarBitmap) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
        this.avatarBitmap = avatarBitmap;
    }
    public FriendItem(String username, int avatarResId) {
        this.type = FriendsAdapter.TYPE_FRIEND;
        this.username = username;
        this.avatarResId = avatarResId;
    }
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
    public int getGroupId() { return groupId; }
    public String getPermissions() { return permissions; }
}