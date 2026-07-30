package com.LinkaProject.linkaLite;

public class InboxItem {
    private String id;
    private String username;
    private String avatarUrl;

    public InboxItem(String id, String username, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
}