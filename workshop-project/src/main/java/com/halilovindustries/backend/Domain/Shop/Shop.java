package com.halilovindustries.backend.Domain.Shop;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.*;
import com.halilovindustries.backend.Domain.User.Registered;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Shop {

    @Id
    private int id; 

    private String name;
    private String description;
    private int founderID;
    private boolean isOpen;
    private double rating;
    private int ratingCount;
    //ids counters
    private int itemIdCounter=0;
    private int bidPurchaseIdCounter=0;
    private int auctionPurchaseIdCounter=0;


    @ElementCollection
    private Set<Integer> ownerIDs = new HashSet<>();

    @ElementCollection
    private Set<Integer> managerIDs = new HashSet<>();

    @ElementCollection
    private Map<Integer, Double> ratedIds = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shop_id", referencedColumnName = "id")
    private List<Item> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shop_id", referencedColumnName = "id")
    private List<BidPurchase> bidPurchaseItems = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shop_id", referencedColumnName = "id")
    private List<AuctionPurchase> auctionPurchaseItems = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shop_id", referencedColumnName = "id")
    private List<Message> inbox = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private PurchasePolicy purchasePolicy;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private DiscountPolicy discountPolicy;

    public Shop() {
        // Required by JPA
    }
    public Shop(int id,int founderID,String name, String description) {
        this.id = id;
        this.founderID = founderID;
        ownerIDs.add(founderID); // Founder is also an owner
        this.name = name;
        this.description = description;
        this.isOpen = true; // Default to open
        this.rating = 0.0; // Default rating
        this.ratingCount = 0; // Default rating count
        this.purchasePolicy = new PurchasePolicy(id);
        this.discountPolicy = new DiscountPolicy(id);
        
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isOpen() { return isOpen; }
    public PurchasePolicy getPurchasePolicy() { return purchasePolicy; }
    public DiscountPolicy getDiscountPolicy() { return discountPolicy; }
    public List<Item> getItems() { 
        return items; 
    }
    public Set<Integer> getOwnerIDs() { return ownerIDs; }
    public Set<Integer> getManagerIDs() { return managerIDs; }
    public double getRating() { 
        if (ratedIds.isEmpty()) {
            return 0.0; // No ratings yet
        }
        double totalRating = 0;
        for (double rating : ratedIds.values()) {
            totalRating += rating;
        }
        this.rating = totalRating / ratedIds.size(); // Calculate the average rating
        return this.rating;
    }
    public int getRatingCount() { return ratingCount; }
    public int getFounderID() { return founderID; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPurchasePolicy(PurchasePolicy purchasePolicy) { this.purchasePolicy = purchasePolicy; }
    public void setDiscountPolicy(DiscountPolicy discountPolicy) { this.discountPolicy = discountPolicy; }
    public void setOpen(boolean isOpen) { this.isOpen = isOpen; }

    public Item addItem(String name, Category category, double price, String description){
        if (price < 0 || !validName(name)) {
            throw new IllegalArgumentException("Item name already exists or price cannot be negative.");
        } 
        else{
            Item item = new Item(itemIdCounter,name, category, price, this.id, description);
            itemIdCounter++;
            items.add(item); 
            return item;
        }
    }
    public boolean validName(String name) {
        for (Item item : items) {
            if (item.getName().equals(name)) {
                return false; // Name already exists
            }
        }
        return true; // Name is valid
    }
    public void removeItem(int itemId) throws IllegalArgumentException {
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        items.remove(item);
    }
    
    public void updateItemName(int itemId, String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty.");
        }
        if (!validName(name)) {
            throw new IllegalArgumentException("Item name already exists in the shop.");
        }
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.setName(name);
    }

    public void updateItemQuantity(int itemId, int quantity) {
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.updateQuantity(quantity);
    }

    public void updateItemPrice(int itemId, double price) {
        if(price<=0){
            throw new IllegalArgumentException("item price cannot be negative");
        }
        Item item = items.stream()
            .filter(i -> i.getId() == itemId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.setPrice(price);
    }

    public void updateItemRating(int raterId, int itemId, double rating) {
                Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.updateRating(raterId, rating);
    }

    public void updateItemCategory(int itemId, Category category) {
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.setCategory(category);
    }
    public void updateItemDescription(int itemId, String description) {
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
        item.setDescription(description);
    }

    public void updateRating(int raterId, double rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        }
        else {
            // if (!ratedIds.containsKey(raterId)) {
            //     ratingCount++;
            // }
            ratedIds.put(raterId, rating); // Mark the user as having rated the shop
            //this.rating = (rating + this.rating) / ratingCount; // Update the shop's rating based on the new rating
        }
    }
    
    public void openShop(){ //must fix later on using synchronized methods
        if (isOpen) {
            throw new RuntimeException("Shop is already open.");
        }
        this.isOpen = true;
    }

    public void closeShop(){//must fix later on using synchronized methods
        if (!isOpen) {
            throw new RuntimeException("Shop is already closed.");
        }
        this.isOpen = false;
    }

    public boolean canAddItemToBasket(int itemId, int quantity) {
        Item item = items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop.")); 
        if (item.quantityCheck(quantity)) {
            return true;
        }
        else {
            throw new IllegalArgumentException("Quantity exceeds available stock.");
        }
    }

    public boolean canPurchaseBasket(HashMap <Integer, Integer> itemsToPurchase){ //itemId -> quantity
        if (!isOpen) {
            throw new RuntimeException("Shop is closed. Cannot purchase items.");
        }
        if (itemsToPurchase.isEmpty()) {
            throw new IllegalArgumentException("Shopping basket is empty. Cannot purchase items.");
        }
        boolean result = true;
        HashMap<Item, Integer> allItems = new HashMap<>();
        for (Integer itemId : itemsToPurchase.keySet()) {
            allItems.put(getItem(itemId), itemsToPurchase.get(itemId));
        }
        purchasePolicy.checkPurchase(allItems); //check if the purchase policy allows the purchase
        for (Integer id : itemsToPurchase.keySet()) {
            Item item = items.stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
            result = result && getItem(id).quantityCheck(itemsToPurchase.get(id)); //assuming basket.get(item) returns the quantity of the item wanting to purchase
        }
        return result;
    }

    public double purchaseBasket(HashMap <Integer, Integer> itemsToPurchase){ //will need to be synchronized later on
        HashMap<Item,Integer> allItems = new HashMap<>(); //<Item,quantity>
        for(Integer itemId: itemsToPurchase.keySet()){
            allItems.put(getItem(id), itemsToPurchase.get(itemId)); 
        }
        double totalPrice =0;
        for(Item item: allItems.keySet()){
            item.buyItem(allItems.get(item));
            totalPrice = totalPrice + item.getPrice() * itemsToPurchase.get(item.getId()); 
        }
        double discount = discountPolicy.calculateDiscount(allItems);
        totalPrice = totalPrice - discount;
        return totalPrice; 
    }

    public void addBidPurchase(int itemId, double bidAmount, int buyerId) {  
        if (isItemInShop(itemId) && purchasePolicy.allowsPurchaseType(PurchaseType.BID)) {
            BidPurchase bidPurchase = new BidPurchase(bidPurchaseIdCounter,bidAmount, itemId, buyerId, buyerId);
            bidPurchaseIdCounter++;
            bidPurchaseItems.add(bidPurchase);
        } else {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void addOwner(int ownerID) {
        if (!ownerIDs.contains(ownerID)) {
            ownerIDs.add(ownerID);
        }
        else {
            throw new IllegalArgumentException("Owner ID already exists in the shop.");
        }
    }

    public void removeOwner(int ownerID) {
        if (ownerIDs.contains(ownerID)) {
            ownerIDs.remove(ownerID);
        } else {
            throw new IllegalArgumentException("Owner ID does not exist in the shop.");
        }
    }

    public void addManager(int managerID) {
        if (!managerIDs.contains(managerID)) {
            managerIDs.add(managerID);
        } else {
            throw new IllegalArgumentException("Manager ID already exists in the shop.");
        }
    }

    public void removeManager(int managerID) {
        if (managerIDs.contains(managerID)) {
            managerIDs.remove(managerID);
        } else {
            throw new IllegalArgumentException("Manager ID does not exist in the shop.");
        }
    }

    public void addBidDecision(int memberId, int bidId, boolean decision,List<Integer> members) {
        BidPurchase bidPurchase = bidPurchaseItems.stream()
            .filter(bid -> bid.getId() == bidId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bid ID does not exist in the shop."));
            bidPurchase.receiveDecision(memberId, decision,members);
    }


    public List<Item> filter(String name, String category, double minPrice, double maxPrice, int itemMinRating, double shopMinRating) {
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : items) {
            if ((name == null || item.getName().toLowerCase().contains(name.toLowerCase())) &&
                (category == null || item.getCategory().equalsIgnoreCase(category)) &&
                (minPrice <= 0 || item.getPrice() >= minPrice) &&
                (maxPrice <= 0 || item.getPrice() <= maxPrice)
                && (itemMinRating <= 0 || item.getRating() >= itemMinRating) &&
                (shopMinRating <= 0 || this.rating >= shopMinRating)) {
                filteredItems.add(item);
                System.out.println("Item added to filtered list: " + item.getName());
            }
        }
        return filteredItems;
    }

    public void updatePurchaseType(PurchaseType purchaseType) {
        purchasePolicy.updatePurchaseType(purchaseType);
    }

	public void submitCounterBid(int userID, int bidID, double offerAmount) {
        BidPurchase bidPurchase = bidPurchaseItems.stream()
            .filter(bid -> bid.getId() == bidID)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bid ID does not exist in the shop."));
        BidPurchase counter= bidPurchase.submitCounterBid(userID,offerAmount, bidPurchaseIdCounter);
        bidPurchaseIdCounter++;
        bidPurchaseItems.add(counter);
    }

    public void removeAppointment(List<Integer> idsToRemove) {
        for ( Integer id : idsToRemove) {
            if (ownerIDs.contains(id))
                ownerIDs.remove(id);
            if (managerIDs.contains(id))
                managerIDs.remove(id);
        }
    }
    public boolean isShopMember(int userId) {
        return ownerIDs.contains(userId) || managerIDs.contains(userId);
    }

    public boolean canAddItemsToBasket(HashMap<Integer,Integer> itemsMap) {
        for ( Integer itemId : itemsMap.keySet()) {
            if(canAddItemToBasket(itemId, itemsMap.get(itemId)) == false) {
                return false; // Item cannot be added to the basket
            }
        }
        return true; // All items can be added to the basket
    }

    public Item getItem(int itemId) {
        return items.stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item ID does not exist in the shop."));
    }

    public Pair<Integer, Double> purchaseBidItem(int bidId, int userID) {
        BidPurchase bidPurchase = bidPurchaseItems.stream()
                .filter(bid -> bid.getId() == bidId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bid ID does not exist in the shop."));
        try {
            if (!getItem(bidPurchase.getItemId()).quantityCheck(1)) {
                throw new IllegalArgumentException("Item is out of stock.");
            }
            return bidPurchase.purchaseBidItem(userID);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Bid purchase failed: " + e.getMessage());
        }
    }

    public void openAuction(int itemID, double startingPrice, LocalDateTime startDate, LocalDateTime endDate) {
            purchasePolicy.allowsPurchaseType(PurchaseType.AUCTION);
            getItem(itemID); // Check if item exists in the shop
            AuctionPurchase auctionPurchase = new AuctionPurchase(auctionPurchaseIdCounter,startingPrice, itemID, startDate, endDate);
            auctionPurchaseIdCounter++;
            auctionPurchaseItems.add(auctionPurchase);
    }

    public void submitAuctionOffer(int auctionID, double offerPrice, int userID) {
         AuctionPurchase auctionPurchase= auctionPurchaseItems.stream()
                .filter(auction -> auction.getId() == auctionID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Auction ID does not exist in the shop."));
        auctionPurchase.placeOffer(offerPrice, userID);
    }

    public Pair<Integer, Double> purchaseAuctionItem(int auctionID, int userID) {
        AuctionPurchase auctionPurchase = auctionPurchaseItems.stream()
                .filter(auction -> auction.getId() == auctionID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Auction ID does not exist in the shop."));
        try {
            if (!getItem(auctionPurchase.getItemId()).quantityCheck(1)) {
                throw new IllegalArgumentException("Item is out of stock.");
            }
            getItem(auctionPurchase.getItemId()).buyItem(1);
            return auctionPurchase.purchaseAuctionItem(userID);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Auction purchase failed: " + e.getMessage());
        }
    }

    public void updateDiscountType(DiscountType discountType) {
        discountPolicy.updateDiscountType(discountType);
    }

    public void addDiscount(DiscountDTO discountDetails) {
        discountPolicy.addDiscount(discountDetails);
    }
    public void removeDiscount(String discountId) {
        discountPolicy.removeDiscount(discountId);
    }

    public void addPurchaseCondition(ConditionDTO condition) {
        purchasePolicy.addCondition(condition);
    }

    public void removePurchaseCondition(String conditionID) {
        purchasePolicy.removeCondition(conditionID);
    }

    public void answerOnCounterBid(int bidId, boolean accept, int userID,List<Integer> members) {
        BidPurchase bidPurchase=bidPurchaseItems.stream()
            .filter(bid -> bid.getId() == bidId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bid ID does not exist in the shop."));
        bidPurchase.answerOnCounterBid(userID, accept, members);
    }

    public List<Integer> getMembersIDs() {
        List<Integer> membersIDs = new ArrayList<>();
        membersIDs.addAll(ownerIDs);
        membersIDs.addAll(managerIDs);
        return membersIDs;
    }
    public BidPurchase getBidPurchase(int bidId) {
        return bidPurchaseItems.stream()
            .filter(bid -> bid.getId() == bidId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bid ID does not exist in the shop."));
    }

    public void clearRoles() {
        ownerIDs.clear();
        managerIDs.clear();
    }
    public Message getMessage(int messageId) {
            return inbox.stream()
                .filter(message -> message.getId() == messageId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message ID does not exist in the inbox."));
    }


    
    public void addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        if (inbox.stream().anyMatch(m -> m.getId() == message.getId())) {
            throw new IllegalArgumentException("Message ID already exists in the inbox.");
        }
        inbox.add(message);
    }

	public boolean hasMessage(int messageId) {
		return inbox.stream()
            .anyMatch(message -> message.getId() == messageId);
	}

	public List<Message> getInbox() {
		return inbox.stream()
			.sorted((m1, m2) -> m1.getDateTime().compareTo(m2.getDateTime()))
			.toList();
	}

    public List<ConditionDTO> getPurchaseConditions() {
        return purchasePolicy.getConditions();
    }

    public List<DiscountDTO> getDiscounts() {
        return discountPolicy.getDiscounts();
    }

    public List<AuctionDTO> getActiveAuctions() {
        List<AuctionDTO> activeAuctions = new ArrayList<>();
        for (AuctionPurchase auction : auctionPurchaseItems) {
            if (auction.isAuctionActive()) {
                activeAuctions.add(new AuctionDTO(auction.getId(),auction.getStartingBid(), auction.getItemId(), auction.getHighestBid(), auction.getAuctionStartTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.getAuctionEndTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.isDone()));
            }
        }
        return activeAuctions;
    }

    public List<AuctionDTO> getFutureAuctions() {
        List<AuctionDTO> auctions = new ArrayList<>();
        for (AuctionPurchase auction : auctionPurchaseItems) {
            if(auction.getAuctionStartTime().isAfter(LocalDateTime.now())) {
                auctions.add(new AuctionDTO(auction.getId(), auction.getStartingBid(), auction.getItemId(), auction.getHighestBid(), auction.getAuctionStartTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.getAuctionEndTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.isDone()));
            }
        }
        return auctions;
    }

    public List<BidDTO> getBids() {
        List<BidDTO> bids = new ArrayList<>();
        for (BidPurchase bid : bidPurchaseItems) {
            bids.add(new BidDTO(bid.getId(), bid.getAmount(), bid.getItemId(), bid.getBuyerId(), bid.getSubmitterId(), bid.getAcceptingMembers(), bid.getRejecterId(), bid.isAccepted(), bid.getCounterBidID(), bid.isDone()));
        }
        return bids;
    }

    public List<AuctionDTO> getWonAuctions(int userId) {
        List<AuctionDTO> wonAuctions = new ArrayList<>();
        for (AuctionPurchase auction : auctionPurchaseItems) {
            if (auction.isAuctionEnded() && auction.getBuyerId() == userId && !auction.isDone()) {
                wonAuctions.add(new AuctionDTO(auction.getId(), auction.getStartingBid(), auction.getItemId(), auction.getHighestBid(), auction.getAuctionStartTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.getAuctionEndTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), auction.isDone()));
            }
        }
        return wonAuctions;
    }

    public List<PurchaseType> getPurchaseTypes() {
        return purchasePolicy.getPurchaseTypes();
    }

    public List<DiscountType> getDiscountTypes() { 
        return discountPolicy.getDiscountTypes();
    }

    public HashMap<Item, Double> getDiscountedPrices(HashMap<Integer,Integer> itemsMap) {
        HashMap<Item, Integer> allItems = new HashMap<>();
        for(Integer itemId : itemsMap.keySet()) {
            if(!isItemInShop(itemId))
            {
                throw new IllegalArgumentException("Item ID does not exist in the shop.");
            }
            Item item = getItem(itemId);
            if(item.getQuantity() < itemsMap.get(itemId)) {
                itemsMap.remove(itemId);
            }
            allItems.put(item, itemsMap.get(itemId));
        }

        return discountPolicy.getPricePerItem(allItems);
    }
    public boolean isItemInShop(int itemId) {
        return items.stream().anyMatch(item -> item.getId() == itemId);
    }
}


