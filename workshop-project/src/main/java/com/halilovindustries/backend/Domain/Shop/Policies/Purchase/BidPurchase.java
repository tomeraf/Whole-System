package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import com.halilovindustries.backend.Domain.DTOs.Pair;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bid_purchases")
public class BidPurchase extends Purchase {

    private int submitterId;

    @ElementCollection
    @CollectionTable(name = "bid_accepting_members", joinColumns = @JoinColumn(name = "bid_id"))
    @Column(name = "member_id")
    private List<Integer> AcceptingMembers = new ArrayList<>();

    private int rejecterId = -1;

    private int isAccepted = 0; // 0 = not accepted , 1 = accepted , -1 = rejected

    private int CounterBidID = -1;

    private boolean done = false;

    public BidPurchase() {}

    public BidPurchase(int id, double bidAmount, int itemId, int buyerID, int submitterID) {
        super(id, bidAmount, itemId, buyerID);
        this.submitterId = submitterID;
        this.AcceptingMembers.add(submitterID);
    }

    public List<Integer> getAcceptingMembers() {
        return AcceptingMembers;
    }

    public int getRejecterId() {
        return rejecterId;
    }

    public int isAccepted() {
        return isAccepted;
    }

    public void reject(int rejecterID) {
        if (isAccepted == 1) {
            throw new IllegalStateException("Bid Purchase has already been accepted.");
        }
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        this.rejecterId = rejecterID;
        isAccepted = -1;
    }

    public void addAcceptingMember(int memberId, List<Integer> members) {
        if (!AcceptingMembers.contains(memberId)) {
            AcceptingMembers.add(memberId);
        }
        if (AcceptingMembers.containsAll(members)) {
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

    public BidPurchase submitCounterBid(int submitterId, double offerAmount, int counterID) {
        if (isAccepted == 1) {
            throw new IllegalStateException("Bid Purchase has already been accepted.");
        }
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        BidPurchase counterBid = new BidPurchase(counterID, offerAmount, getItemId(), getBuyerId(), submitterId);
        counterBid.setCounterBidID(counterID);
        return counterBid;
    }

    private void setCounterBidID(int counterID) {
        this.CounterBidID = counterID;
    }

    public Pair<Integer, Double> purchaseBidItem(int userID) {
        if (isAccepted == 0) {
            throw new IllegalArgumentException("Error: not all members accepted the bid.");
        }
        if (getBuyerId() != userID) {
            throw new IllegalArgumentException("Error: user is not the buyer of the bid.");
        }
        done = true; // Mark the bid as done
        return new Pair<>(getItemId(), getAmount()); // Return the item ID and bid amount as a pair
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
            isAccepted = -1;
        }
    }

    public int getSubmitterId() {
        return submitterId;
    }

    public int getCounterBidID() {
        return CounterBidID;
    }

    public boolean isDone() {
        return done;
    }
}
