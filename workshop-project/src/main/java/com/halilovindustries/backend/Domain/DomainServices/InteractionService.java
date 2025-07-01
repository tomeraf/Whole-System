package com.halilovindustries.backend.Domain.DomainServices;

import java.time.LocalDateTime;
import java.util.Set;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.OfferMessage;
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
    public void sendMessage(Registered sender,Shop shop, String title,String content, int msgId) {
        if(title == null || content == null||title.isEmpty() || content.isEmpty()) {
            throw new IllegalArgumentException("Title and content cannot be null");
        }
        Message message = new Message(msgId, sender.getUsername(),shop.getName(),LocalDateTime.now(),title,content, true);
        shop.addMessage(message);
        sender.addMessage(message);
    }

    public Message respondToMessage(Registered member, Shop shop, int messageId, String title, String content, int sendedMsgID) {
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
        Message response = new Message(sendedMsgID, message.getUserName(),shop.getName(),LocalDateTime.now(),"(Re: " + message.getTitle() + "), " + title ,content, false);
        message.setRespondId(response.getId());
        shop.addMessage(response);
        return response;
    }

    public boolean answerOfferMessage(Shop shop, int msgId, boolean decision) {
        Message message = shop.getMessage(msgId);
        if (message == null) {
            throw new IllegalArgumentException("Message with ID " + msgId + " does not exist.");
        }
        if (!message.isOffer()) {
            throw new IllegalArgumentException("Message with ID " + msgId + " is not an offer.");
        }
        ((OfferMessage)message).setDecision(decision);
        return true;
    }

    public void offerMessage(Registered sender, Registered receiver, int msgId, Shop shop, String title, String content, boolean isManagerOffer, Set<Permission> offerDetails) {
        if(title == null || content == null||title.isEmpty() || content.isEmpty()) {
            throw new IllegalArgumentException("Title and content cannot be null");
        }
        if(!sender.hasPermission(shop.getId(), Permission.APPOINTMENT)) {
            throw new IllegalArgumentException("You do not have permission to send appointment offers");
        }
        Message message = new OfferMessage(msgId, sender.getUsername(), shop.getName(), LocalDateTime.now(), title, content, false);
        
        ((OfferMessage)message).setAppointerId(sender.getUserID());
        ((OfferMessage)message).setAppointeeId(receiver.getUserID());
        ((OfferMessage)message).setDecision(null); // Pending by default
        ((OfferMessage)message).setManagerOffer(isManagerOffer);
        ((OfferMessage)message).setOfferDetails(offerDetails);
        shop.addMessage(message);
        receiver.addMessage(message);
    }
}
