package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.Shop.ShopKey;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@IdClass(ShopKey.class)
public class AuctionPurchase {

    @Id
    private int id;
    @Id
    private int shopId;

    private double startingBid;
    private int buyerId = -1;
    private int itemId;
    private double highestBid = 0;

    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

    private boolean done = false;
    private boolean notified=false;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> bidders = new ArrayList<>();

    public AuctionPurchase() {
        // Required by JPA
    }

    public AuctionPurchase(int id,int shopId,double startingBid, int itemId, LocalDateTime auctionStartTime, LocalDateTime auctionEndTime) {
        this.id = id;
        this.shopId=shopId;
        if (startingBid <= 0) {
            throw new IllegalArgumentException("Starting bid must be greater than 0.");
        }
        this.startingBid = startingBid;
        this.itemId = itemId;
        validateAuctionTimes(auctionStartTime, auctionEndTime);
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
    }

    private void validateAuctionTimes(LocalDateTime auctionStartTime, LocalDateTime auctionEndTime) {
        if (auctionStartTime.isAfter(auctionEndTime)) {
            throw new IllegalArgumentException("Auction start time must be before end time.");
        }
        if (auctionStartTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction start time must be in the future.");
        }
        if (auctionEndTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction end time must be in the future.");
        }
    }

    public void placeOffer(double bidAmount, int buyerId) {
        if (bidAmount > highestBid && bidAmount >= startingBid && isAuctionActive()) {
            this.highestBid = bidAmount;
            this.buyerId = buyerId;
            if (!bidders.contains(buyerId)) {
                bidders.add(buyerId);
            }
        } else {
            throw new IllegalArgumentException("Bid amount must be higher than the current bid and starting bid.");
        }
    }

    public Pair<Integer, Double> purchaseAuctionItem(int userID) {
        if (!isAuctionEnded()) {
            throw new IllegalStateException("Auction has not ended yet.");
        }
        if (buyerId != userID) {
            throw new IllegalArgumentException("Error: user is not the highest bidder.");
        }
        done = true;
        return new Pair<>(itemId, highestBid);
    }

    public int getId() {
        return id;
    }

    public int getShopId(){
        return shopId;
    }
    public double getStartingBid() {
        return startingBid;
    }

    public int getItemId() {
        return itemId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public double getHighestBid() {
        return highestBid;
    }
    public boolean isNotified() {
        return notified;
    }
    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public LocalDateTime getAuctionStartTime() {
        return auctionStartTime;
    }

    public LocalDateTime getAuctionEndTime() {
        return auctionEndTime;
    }

    public boolean isAuctionActive() {
        return auctionStartTime.isBefore(LocalDateTime.now()) && auctionEndTime.isAfter(LocalDateTime.now());
    }

    public boolean isAuctionEnded() {
        return auctionEndTime.isBefore(LocalDateTime.now());
    }

    public boolean isAuctionStarted() {
        return auctionStartTime.isBefore(LocalDateTime.now());
    }

    public boolean isDone() {
        return done;
    }

    public List<Integer> getBidders() {
        return bidders;
    }
}
