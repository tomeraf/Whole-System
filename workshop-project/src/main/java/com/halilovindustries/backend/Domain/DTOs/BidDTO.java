package com.halilovindustries.backend.Domain.DTOs;

import java.util.List;

public class BidDTO {
    private int id;
    private double amount;
    private int itemId;
    private int buyerId;
    private int submitterId;
    private int rejecterId;
    private List<Integer> acceptingMembers;
    private int isAccepted = 0; // 0 = not accepted , 1 = accepted , -1 = rejected
    private int counterBidId = -1;
    private boolean done;

    public BidDTO(int id, double amount, int itemId, int buyerId, int submitterId, List<Integer> acceptingMembers, int rejecterId, int isAccepted, int counterBidId, boolean done) {
        this.id = id;
        this.amount = amount;
        this.itemId = itemId;
        this.buyerId = buyerId;
        this.submitterId = submitterId;
        this.acceptingMembers = acceptingMembers;
        this.rejecterId = rejecterId;
        this.isAccepted = isAccepted;
        this.counterBidId = counterBidId;
        this.done = done;
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public int getItemId() {
        return itemId;
    }

    public int getBuyerId() {
        return buyerId;
    }
    public int getSubmitterId() {
        return submitterId;
    }
    public int getRejecterId() {
        return rejecterId;
    }
    public List<Integer> getAcceptingMembers() {
        return acceptingMembers;
    }
    public int getIsAccepted() {
        return isAccepted;
    }
    public int getCounterBidId() {
        return counterBidId;
    }
    public boolean isDone() {
        return done;
    }
    

}
