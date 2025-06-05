package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.Shop.ShopKey;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@IdClass(ShopKey.class)
public class BidPurchase {

    @Id
    private int id;
    @Id
    private int shopId;

    private double amount;
    private int itemId;
    private int buyerId;
    private int submitterId;
    private int rejecterId = -1;
    private int isAccepted = 0; // 0 = not accepted , 1 = accepted , -1 = rejected
    private int counterBidID = -1;
    private boolean done = false;

    @ElementCollection
    private List<Integer> acceptingMembers = new ArrayList<>();

    public BidPurchase() {
        // Required by JPA
    }

    public BidPurchase(int id,int shopId,double bidAmount, int itemId, int buyerID, int submitterID) {
        this.id = id;
        this.shopId=shopId;
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than 0.");
        }
        this.amount = bidAmount;
        this.itemId = itemId;
        this.buyerId = buyerID;
        this.submitterId = submitterID;
        this.acceptingMembers.add(submitterID);
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

    public List<Integer> getAcceptingMembers() {
        return acceptingMembers;
    }

    public int getRejecterId() {
        return rejecterId;
    }

    public int isAccepted() {
        return isAccepted;
    }

    public int getCounterBidID() {
        return counterBidID;
    }
    public int getShopId(){
        return shopId;
    }

    public boolean isDone() {
        return done;
    }

    public void reject(int rejecterID) {
        if (isAccepted == 1) {
            throw new IllegalStateException("Bid Purchase has already been accepted.");
        }
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + this.rejecterId);
        }
        this.rejecterId = rejecterID;
        isAccepted = -1;
    }

    public void addAcceptingMember(int memberId, List<Integer> members) {
        if (!acceptingMembers.contains(memberId)) {
            acceptingMembers.add(memberId);
        }
        if (acceptingMembers.containsAll(members)) {
            isAccepted = 1;
        }
    }

    public void receiveDecision(int memberId, boolean answer, List<Integer> members) {
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        if (answer) {
            addAcceptingMember(memberId, members);
        } else {
            reject(memberId);
        }
    }

    public BidPurchase submitCounterBid(int submitterId, double offerAmount,int counterId) {
        if (isAccepted == 1) {
            throw new IllegalStateException("Bid Purchase has already been accepted.");
        }
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        BidPurchase counterBid = new BidPurchase(counterId,shopId,offerAmount, getItemId(), getBuyerId(), submitterId);
        counterBid.setCounterBidID(counterBid.getId());
        return counterBid;
    }

    public void setCounterBidID(int counterID) {
        this.counterBidID = counterID;
    }

    public Pair<Integer, Double> purchaseBidItem(int userID) {
        if (isAccepted == 0) {
            throw new IllegalArgumentException("Error: not all members accepted the bid.");
        }
        if (getBuyerId() != userID) {
            throw new IllegalArgumentException("Error: user is not the buyer of the bid.");
        }
        done = true;
        return new Pair<>(getItemId(), getAmount());
    }

    public void answerOnCounterBid(int userID, boolean accept, List<Integer> members) {
        if (getBuyerId() != userID) {
            throw new IllegalArgumentException("Error: user is not the buyer of the bid.");
        }
        if (accept) {
            isAccepted = 1;
            addAcceptingMember(userID, members);
        } else {
            reject(userID);
        }
    }
}
