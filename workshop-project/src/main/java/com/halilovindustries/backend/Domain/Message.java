package com.halilovindustries.backend.Domain;



import java.time.LocalDateTime;

public class Message {

    private int id;
    private String userName;
    private String shopName;
    private LocalDateTime dateTime;
    private String title;
    private String content;
    private int respondId;// id of the message that this message is responding to
    private boolean FromUser;

    
    public Message(int id, String userName, String shopName, LocalDateTime dateTime, String title, String content, boolean FromUser) {
        this.id = id;
        this.userName = userName;
        this.shopName = shopName;
        this.dateTime = dateTime;
        this.title = title;
        this.content = content;
        this.respondId = -1; // default value for respondId
        this.FromUser = FromUser;
    }
    public int getId() {
        return id;
    }
    
    public String getUserName() {
        return userName;
    }
    public String getShopName() {
        return shopName;
    }
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
    public int getRespondId() {
        return respondId;
    }
    public void setRespondId(int respondId) {
        this.respondId = respondId;
    }
    public boolean needResponse() {
        return respondId == -1;
    }
    public boolean isFromUser() {
        return FromUser;
    }
}