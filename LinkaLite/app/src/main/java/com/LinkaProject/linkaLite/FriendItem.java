package com.LinkaProject.linkaLite;

public class FriendItem {
    private int id;
    private String username;
    private String groupName;
    private String permissions;
    private int type;

    public FriendItem(String username) {
        this.username = username;
        this.type = FriendsAdapter.TYPE_FRIEND;
    }

    public FriendItem(int id, String groupName, String permissions) {
        this.id = id;
        this.groupName = groupName;
        this.permissions = permissions;
        this.type = FriendsAdapter.TYPE_GROUP;
    }

    public FriendItem(String headerTitle, boolean isHeader) {
        this.groupName = headerTitle;
        this.type = FriendsAdapter.TYPE_HEADER;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getGroupName() { return groupName; }
    public String getPermissions() { return permissions; }
    public int getType() { return type; }
}