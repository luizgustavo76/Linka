package com.LinkaProject.linkaLite;

public class Message {
    private int id;
    private String message;
    private String sender;

    public Message(int id, String message, String sender) {
        this.id = id;
        this.message = message;
        this.sender = sender;
    }

    public int getId() { return id; }
    public String getMessage() { return message; }
    public String getSender() { return sender; }
}