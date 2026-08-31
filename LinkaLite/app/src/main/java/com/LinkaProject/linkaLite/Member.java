package com.LinkaProject.linkaLite;

import android.widget.ImageView;

public class Member {
    private String username;
    private ImageView avatarImageView;

    public Member(String username, ImageView avatarImageView) {
        this.username = username;
        this.avatarImageView = avatarImageView;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ImageView getAvatarImageView() {
        return avatarImageView;
    }

    public void setAvatarImageView(ImageView avatarImageView) {
        this.avatarImageView = avatarImageView;
    }
}