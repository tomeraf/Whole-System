package com.halilovindustries.backend.Domain;



import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String userName;
    private String shopName;
    private LocalDateTime dateTime;
    private String title;
    private String content;
    private int respondId;// id of the message that this message is responding to
    private boolean FromUser;

    
    public Message(String userName, String shopName, LocalDateTime dateTime, String title, String content, boolean FromUser) {
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