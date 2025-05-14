package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.halilovindustries.backend.Domain.DTOs.Pair;

public class BidPurchase extends Purchase {
    private int submitterId;
    private List<Integer> AcceptingMembers;
    private int rejecterId=-1;
    private int isAccepted = 0; // 0 = not accepted, 1 = accepted, -1 = rejected
    private int CounterBidID=-1;
    boolean done=false;

    public BidPurchase(int id,double bidAmount, int itemId, int buyerID,int submitterID) {
        super(id, bidAmount, itemId, buyerID);
        this.submitterId = submitterID;
        this.AcceptingMembers = new ArrayList<>();
        this.AcceptingMembers.add(submitterID);
    }
    public List<Integer> getAcceptingMembers() {
        return AcceptingMembers;
    }
    public int getRejecterId() {
        return rejecterId;
    }
    public boolean isAccepted() {
        return isAccepted==1;
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
    public void addAcceptingMember(int memberId) {
        if (!AcceptingMembers.contains(memberId)) {
            AcceptingMembers.add(memberId);
        }
    }
    public void receiveDecision(int memberId, boolean answer) {
        if(isAccepted==-1)
        {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        if (answer) {
            addAcceptingMember(memberId);
        } else {
            reject(memberId);
        }
    }
    public BidPurchase submitCounterBid(int submitterId,double offerAmount,int counterID) {
        if (isAccepted == 1) {
            throw new IllegalStateException("Bid Purchase has already been accepted.");
        }
        if (isAccepted == -1) {
            throw new IllegalStateException("Bid Purchase has already been rejected by " + rejecterId);
        }
        BidPurchase counterBid = new BidPurchase(counterID, offerAmount, getItemId(),getBuyerId(), submitterId);
        counterBid.setCounterBidID(counterID);
        return counterBid;
    }
    /* getMembersWithPermission() is a placeholder for the actual implementation of getting members with permission.
    public String showStatus(){
        List<Integer> allMembers = getMembersWithPermission();
        allMembers.add(buyerId);
        StringBuilder status = new StringBuilder("Bid Purchase Status:\n");
        status.append("Bid Amount: ").append(bidAmount).append("\n");
        status.append("Item ID: ").append(itemId).append("\n");
        status.append("Buyer ID: ").append(buyerId).append("\n");
        status.append("Submitter ID: ").append(submitterId).append("\n");
        for (int memberId : allMembers) {
            if(memberId == rejecterID) {
                status.append("Rejecting Member ID: ").append(memberId).append("\n");
            } else if (AcceptingMembers.contains(memberId)) {
                status.append("Accepting Member ID: ").append(memberId).append("\n");
            }
            else {
                status.append("Need to Decide Member ID: ").append(memberId).append("\n");
            }
        }
    }
    */
    private void setCounterBidID(int counterID) {
        this.CounterBidID = counterID;
    }
    public Pair<Integer,Double> purchaseBidItem(int userID, List<Integer> memberIds) {
        memberIds.add(userID); // Add the user ID to the set of member IDs
        if(!AcceptingMembers.containsAll(memberIds)){
            throw new IllegalArgumentException("Error: not all members accepted the bid.");
        }
        if(getBuyerId()!=userID){
            throw new IllegalArgumentException("Error: user is not the buyer of the bid.");
        }
        isAccepted = 1;
        done = true; // Mark the bid as done
        return new Pair<>(getItemId(), getAmount()); // Return the item ID and bid amount as a pair
    }
    public void answerOnCounterBid(int userID, boolean accept) {
        if(getBuyerId()!=userID)
        {
            throw new IllegalArgumentException("Error: user is not the buyer of the bid.");
        }
        receiveDecision(userID, accept);
    }


}
