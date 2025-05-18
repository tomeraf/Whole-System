package com.halilovindustries.backend.Domain.DomainServices;

import java.time.LocalDateTime;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.backend.Domain.Shop.Shop;

public class InteractionService {
    
    private static InteractionService instance = null;

    private InteractionService() {
        // private constructor to prevent instantiation
    }

    public static InteractionService getInstance() {
        if (instance == null) {
            instance = new InteractionService();
        }
        return instance;
    }
    public void sendMessage(Registered sender,Shop shop,String title,String content) {
        if(title == null || content == null||title.isEmpty() || content.isEmpty()) {
            throw new IllegalArgumentException("Title and content cannot be null");
        }
        Message message = new Message(shop.getNextMessageId(),sender.getUsername(),shop.getName(),LocalDateTime.now(),title,content);
        shop.addMessage(message);
        sender.addMessage(message);
    }

    public Message respondToMessage(Registered member, Shop shop, int messageId,String title ,String content) {
        if(content == null|| title == null||title.isEmpty() || content.isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if(!member.hasPermission(shop.getId(),Permission.ANSWER_MESSAGE)) {
            throw new IllegalArgumentException("You do not have permission to respond to messages");
        }
        if(!shop.hasMessage(messageId)) {
            throw new IllegalArgumentException("Message with ID " + messageId + " does not exist.");
        }
        Message message = shop.getMessage(messageId);
        if(!message.needResponse()) {
            throw new IllegalArgumentException("This message has already been responded to.");
        }
        Message response = new Message(shop.getNextMessageId(),message.getUserName(),shop.getName(),LocalDateTime.now(),"Re: " + title,content);
        message.setRespondId(response.getId());
        shop.addMessage(response);
        message.setRespondId(response.getId());
        return response;
    }





}
