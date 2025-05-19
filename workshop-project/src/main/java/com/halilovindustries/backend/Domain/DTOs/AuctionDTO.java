package com.halilovindustries.backend.Domain.DTOs;

public class AuctionDTO {
    private int id;
    private double startingBid;
    private int itemId;
    private double highestBid;
    private String auctionStartTime; 
    private String auctionEndTime; 

    
    public AuctionDTO(int id,double startingBid, int itemId, double highestBid, String auctionStartTime, String auctionEndTime) {
        this.id = id;
        this.startingBid = startingBid;
        this.itemId = itemId;
        this.highestBid = highestBid;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
    }
    public int getId() {
        return id;
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
}
