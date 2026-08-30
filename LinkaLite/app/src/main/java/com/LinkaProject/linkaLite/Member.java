package com.linka.lite.model;

public class Member {
    private String username;
    private int avatarResourceId;

    public Member(String username, int avatarResourceId) {
        this.username = username;
        this.avatarResourceId = avatarResourceId;
    }

    public String getUsername() {
        return username;
    }

    public int getAvatarResourceId() {
        return avatarResourceId;
    }
}