package com.LinkaProject.linkaLite;
public class InboxItem {
    private String username;
    private String message;
    public InboxItem(String username, String message) {
        this.username = username;
        this.message = message;
    }
    public String getUsername() { 
        return username; 
    }
    public String getMessage() { 
        return message; 
    }
}