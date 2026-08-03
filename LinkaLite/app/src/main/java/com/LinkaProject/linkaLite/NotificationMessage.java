package com.LinkaProject.linkaLite;

public class NotificationMessage {
    private int id;
    private String content;
    private String datetime;
    private String fromUser;
    private String receiver;
    private int read;
    private String type;

    public NotificationMessage(int id, String content, String datetime, String fromUser, String receiver, int read, String type) {
        this.id = id;
        this.content = content;
        this.datetime = datetime;
        this.fromUser = fromUser;
        this.receiver = receiver;
        this.read = read;
        this.type = type;
    }

    public int getId() { return id; }
    public String getContent() { return content; }
    public String getDatetime() { return datetime; }
    public String getFromUser() { return fromUser; }
    public String getReceiver() { return receiver; }
    public int getRead() { return read; }
    public String getType() { return type; }
}