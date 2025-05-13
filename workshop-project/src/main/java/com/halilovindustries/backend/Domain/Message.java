package com.halilovindustries.backend.Domain;

import java.util.Date;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IMessage;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IMessageListener;

public class Message implements IMessage {

    private int id;
    private int senderId;
    private String senderName;
    private int receiverId;
    private Date dateTime;
    private String title;
    private String content;
    private IMessage previous;
    private IMessage next;

    
    public Message(int id, int senderId, String senderName, int receiverId, Date dateTime, String title, String content) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.dateTime = dateTime;
        this.content = content;
        this.title = title == null ? "" : title;
        this.previous = null;
        this.next = null;
    }
    public int getId() {
        return id;
    }
    public int getSenderId() {
        return senderId;
    }
    public String getSenderName() {
        return senderName;
    }
    public int getReceiverId() {
        return receiverId;
    }
    public Date getDateTime() {
        return dateTime;
    }
    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
    public IMessage getPrevious() {
        return previous;
    }
    public IMessage getNext() {
        return next;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setPrevious(IMessage previous) {
        this.previous = previous;
    }
    public void setNext(IMessage next) {
        this.next = next;
    }    
    
    @Override
    public void send(IMessageListener receiver) {
        receiver.acceptMessage(this);
    }
    
    @Override
    public void respond(IMessage respondTo) {
        //will need to send real time notification to the receiver
        respondTo.setNext(this);
        this.setPrevious(respondTo);
    }

    @Override
    public boolean canSend() {
        return senderId >= 0 && receiverId >= 0 && content != null;
    }
}