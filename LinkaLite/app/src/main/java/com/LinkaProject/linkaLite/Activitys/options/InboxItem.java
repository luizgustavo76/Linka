package com.LinkaProject.linkaLite;

public class InboxItem {
    public static final int TYPE_REQUEST = 0;
    public static final int TYPE_NOTIFICATION = 1;

    private int id;
    private String username;
    private String message;
    private String notifType;
    private int type;

    public InboxItem(String username, String message) {
        this(-1, username, message, "REQUEST", TYPE_REQUEST);
    }

    public InboxItem(int id, String username, String message, String notifType, int type) {
        this.id = id;
        this.username = username;
        this.message = message;
        this.notifType = notifType;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public String getNotifType() {
        return notifType;
    }

    public int getType() {
        return type;
    }
}