package com.halilovindustries.backend.Domain.DTOs;

public class AuctionDTO {
    private int id;
    private double startingBid;
    private int itemId;
    private double highestBid;
    private String auctionStartTime; 
    private String auctionEndTime; 
    private boolean done;
    private boolean notified;

    
    public AuctionDTO(int id,double startingBid, int itemId, double highestBid, String auctionStartTime, String auctionEndTime, boolean done, boolean notified) {
        this.id = id;
        this.startingBid = startingBid;
        this.itemId = itemId;
        this.highestBid = highestBid;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
        this.done = done;
        this.notified = notified;
    }
    public int getId() {
        return id;
    }
    public boolean isNotified() {
        return notified;
    }
    public double getStartingBid() {
        return startingBid;
    }
    public int getItemId() {
        return itemId;
    }
    public double getHighestBid() {
        return highestBid;
    }
    public String getAuctionStartTime() {
        return auctionStartTime;
    }
    public String getAuctionEndTime() {
        return auctionEndTime;
    }
    public boolean isDone() {
        return done;
    }
}
