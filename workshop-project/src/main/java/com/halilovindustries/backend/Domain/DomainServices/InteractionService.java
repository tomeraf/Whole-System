package com.halilovindustries.backend.Domain.DomainServices;

import java.util.Date;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Permission;
import com.halilovindustries.backend.Domain.Registered;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IMessage;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IMessageListener;

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
    public IMessage createMessage(int id, int senderId, String senderName, int receiverId, String title, String content) {
        IMessage message = new Message(id, senderId, senderName, receiverId, new Date(), title, content);
        return message;
    }

    public void sendMessage(IMessageListener receiver, IMessage message) {
        if(receiver == null || message == null) {
            throw new IllegalArgumentException("Receiver and message cannot be null");
        }
        if(!message.canSend()){
            throw new IllegalStateException("Message cannot be sent");
        }
        message.send(receiver);
    }

    public void respondToMessage(Registered sender, IMessage parentMessage, IMessage responseMessage) {
        if(parentMessage == null || responseMessage == null) {
            throw new IllegalArgumentException("Parent message and response message cannot be null");
        }
        if(!responseMessage.canSend()){
            throw new IllegalStateException("Response message cannot be sent");
        }
        if(sender.hasPermission(responseMessage.getReceiverId(), Permission.OWNER)){
            responseMessage.respond(parentMessage);
        }
        else {
            throw new IllegalStateException("Non owner does not have permission to respond to this message");
        }
    }





}
