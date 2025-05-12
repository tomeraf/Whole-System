package Domain.Shop;

import java.time.LocalDate;
import java.time.LocalDateTime;


import Domain.Adapters_and_Interfaces.IMessage;
import Domain.Adapters_and_Interfaces.IMessageListener;
import Domain.DTOs.Pair;
import Domain.DTOs.ShopDTO;

import Domain.Shop.Discount.*;
import Domain.Shop.Purchase.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Shop implements IMessageListener {

    private int id;
    private String name;
    private String description;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;
    private HashMap<Integer, Item> items; // itemId -> item
    private Set<Integer> ownerIDs;
    private Set<Integer> managerIDs;
    private boolean isOpen;
    private int counterItemId; // Counter for item IDs
    private double rating;
    private int ratingCount;
    private HashMap<Integer, BidPurchase> bidPurchaseItems; // <BidId, BidPurchase>
    private HashMap<Integer, AuctionPurchase> auctionPurchaseItems; // <AuctionId, AuctionPurchase>
    private int bidPurchaseCounter; // Counter for bid purchases
    private int auctionPurchaseCounter; // Counter for auction purchases
    private HashMap<Integer, IMessage> inbox; // 
    int messageIdCounter = 1; // Counter for message IDs

    public Shop(int id,int founderID, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.purchasePolicy = new PurchasePolicy();
        this.discountPolicy = new DiscountPolicy();
        this.items = new HashMap<>();
        this.ownerIDs = new HashSet<>();
        this.managerIDs = new HashSet<>();
        ownerIDs.add(founderID); // Add the founder as an owner
        this.isOpen = true;
        this.counterItemId = 0; // Initialize the item ID counter
        this.rating = 0.0;
        this.ratingCount = 0;
        this.bidPurchaseItems = new HashMap<>();
        this.auctionPurchaseItems = new HashMap<>();
        this.bidPurchaseCounter = 1; 
        this.auctionPurchaseCounter = 1; 
        this.inbox = new HashMap<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isOpen() { return isOpen; }
    public PurchasePolicy getPurchasePolicy() { return purchasePolicy; }
    public DiscountPolicy getDiscountPolicy() { return discountPolicy; }
    public HashMap<Integer, Item> getItems() { return items; }
    public Set<Integer> getOwnerIDs() { return ownerIDs; }
    public Set<Integer> getManagerIDs() { return managerIDs; }
    public double getRating() { return rating; }
    public int getRatingCount() { return ratingCount; }

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
            Item item = new Item(name, category, price, this.id, counterItemId, description);
            items.put(item.getId(), item);
            counterItemId++; // Increment the item ID counter for the next item
            return item;
        }
    }
    public boolean validName(String name) {
        for (Item item : items.values()) {
            if (item.getName().equals(name)) {
                return false; // Name already exists
            }
        }
        return true; // Name is valid
    }
    public void removeItem(int itemId) throws IllegalArgumentException {
        if (items.containsKey(itemId)) {
            items.remove(itemId);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }
    
    public void updateItemName(int itemId, String name) {
        if (items.containsKey(itemId)) {
            Item item = items.get(itemId);
            item.setName(name);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void updateItemQuantity(int itemId, int quantity) {
        if (items.containsKey(itemId)){
            Item item = items.get(itemId);
            item.updateQuantity(quantity);
        } 
        else {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void updateItemPrice(int itemId, double price) {
        if(price<=0){
            throw new IllegalArgumentException("item price cannot be negative");
        }
        if (items.containsKey(itemId)){
            Item item = items.get(itemId);
            item.setPrice(price);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void updateItemRating(int itemId, double rating) {
        if (items.containsKey(itemId)) {
            Item item = items.get(itemId);
            item.updateRating(rating);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void updateItemCategory(int itemId, Category category) {
        if (items.containsKey(itemId)) {
            Item item = items.get(itemId);
            item.setCategory(category);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }
        public void updateItemDescription(int itemId, String description) {
        if (items.containsKey(itemId)) {
            Item item = items.get(itemId);
            item.setDescription(description);
        } 
        else{
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void updateRating(double rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        }
        else {
            ratingCount++;
            this.rating = (rating + this.rating) / ratingCount; // Update the shop's rating based on the new rating
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
        if (items.containsKey(itemId) && items.get(itemId).quantityCheck(quantity)) {
            return true;
        }
        else if (!items.containsKey(itemId)) {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
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

        for (Integer id : itemsToPurchase.keySet()) {
            if (!items.containsKey(id)) {
                throw new IllegalArgumentException("Item ID does not exist in the shop.");
            }
            result = result && items.get(id).quantityCheck(itemsToPurchase.get(id)); //assuming basket.get(item) returns the quantity of the item wanting to purchase
        }
        return result;
    }

    public double purchaseBasket(HashMap <Integer, Integer> itemsToPurchase){ //will need to be synchronized later on
        List<Item> allItems = new ArrayList<Item>();
        for(Integer itemId: itemsToPurchase.keySet()){
            allItems.add(items.get(itemId)); 
        }
        double totalPrice =0;
        for(Item item: allItems){
            item.buyItem(itemsToPurchase.get(item.getId()));
            totalPrice = totalPrice + item.getPrice() * itemsToPurchase.get(item.getId()); 
        }
        return totalPrice; 
    }

    public void addBidPurchase(int itemId, double bidAmount, int buyerId) {  
        if (items.containsKey(itemId) && purchasePolicy.allowsPurchaseType(PurchaseType.BID)) {
            BidPurchase bidPurchase = new BidPurchase(bidPurchaseCounter, bidAmount, itemId, buyerId, buyerId);
            bidPurchaseCounter++;
            bidPurchaseItems.put(bidPurchase.getId(), bidPurchase);
        } else {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }
    public void addAuctionPurchase(int itemId, double startingAmount, LocalDateTime startTime, LocalDateTime endTime){
        if (items.containsKey(itemId) && purchasePolicy.allowsPurchaseType(PurchaseType.AUCTION)) {
            AuctionPurchase auctionPurchase = new AuctionPurchase(auctionPurchaseCounter, startingAmount, itemId, startTime, endTime);
            auctionPurchaseItems.put(auctionPurchase.getId(), auctionPurchase);
            auctionPurchaseCounter++;
        } 
        else {
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

    public void addBidDecision(int memberId, int bidId, boolean decision) {
        if(!bidPurchaseItems.containsKey(bidId)) {
            throw new IllegalArgumentException("Bid ID does not exist in the shop.");
        } 
        else {
            BidPurchase bidPurchase = bidPurchaseItems.get(bidId);
            bidPurchase.receiveDecision(memberId, decision);
        }
    }


    public List<Item> filter(String name, String category, double minPrice, double maxPrice, int itemMinRating, double shopMinRating) {
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : items.values()) {
            if ((name == null || item.getName().toLowerCase().contains(name.toLowerCase())) &&
                (category == null || item.getCategory().equalsIgnoreCase(category)) &&
                (minPrice <= 0 || item.getPrice() >= minPrice) &&
                (maxPrice <= 0 || item.getPrice() <= maxPrice)
                && (itemMinRating <= 0 || item.getRating() >= itemMinRating) &&
                (shopMinRating <= 0 || this.rating >= shopMinRating)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    public void updatePurchaseType(String purchaseType) {
        purchasePolicy.updatePurchaseType(purchaseType);
    }

	public void submitCounterBid(int userID, int bidID, double offerAmount) {
        if (bidPurchaseItems.containsKey(bidID)) {
            BidPurchase bidPurchase = bidPurchaseItems.get(bidID);
            BidPurchase counter= bidPurchase.submitCounterBid(userID,offerAmount,bidPurchaseCounter);
            bidPurchaseCounter++;
            bidPurchaseItems.put(counter.getId(), counter);
        } else {
            throw new IllegalArgumentException("Bid ID does not exist in the shop.");
        }
    }
 
    @Override
    public void acceptMessage(IMessage message) {
        inbox.put(message.getId(), message);
        //will need to update all owners.
    }

    public IMessage getMessage(int messageId) {
        if (inbox.containsKey(messageId)) {
            return inbox.get(messageId);
        }
        else {
            throw new IllegalArgumentException("Message ID does not exist in the inbox.");
        }
    }

    public HashMap<Integer, IMessage> getAllMessages() {
        return inbox;
    }

    public int getNextMessageId() {
        return messageIdCounter++;
    }

    public void removeAppointment(List<Integer> idsToRemove) {
        for (Integer id : idsToRemove) {
            if (ownerIDs.contains(id))
                ownerIDs.remove(id);
            if (managerIDs.contains(id))
                managerIDs.remove(id);
        }
    }

    public boolean canAddItemsToBasket(HashMap<Integer,Integer> itemsMap) {
        for (Integer itemId : itemsMap.keySet()) {
            if(canAddItemToBasket(itemId, itemsMap.get(itemId)) == false) {
                return false; // Item cannot be added to the basket
            }
        }
        return true; // All items can be added to the basket
    }

    public Item getItem(Integer itemId) {
        if (items.containsKey(itemId)) {
            return items.get(itemId);
        } else {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public Pair<Integer,Double> purchaseBidItem(int bidId, int userID) {
        if (bidPurchaseItems.containsKey(bidId)) {
            BidPurchase bidPurchase = bidPurchaseItems.get(bidId);
            try{
                if(!getItem(bidPurchase.getItemId()).quantityCheck(1)){
                    throw new IllegalArgumentException("Item is out of stock.");
                }
                return bidPurchase.purchaseBidItem(userID, ownerIDs);
            }
             catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Bid purchase failed: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Bid ID does not exist in the shop.");
        }
    }

    public void openAuction(int itemID, double startingPrice, LocalDateTime startDate, LocalDateTime endDate) {
        if (items.containsKey(itemID)) {
            AuctionPurchase auctionPurchase = new AuctionPurchase(auctionPurchaseCounter, startingPrice, itemID, startDate, endDate);
            auctionPurchaseCounter++;
            auctionPurchaseItems.put(auctionPurchase.getId(), auctionPurchase);
        } else {
            throw new IllegalArgumentException("Item ID does not exist in the shop.");
        }
    }

    public void submitAuctionOffer(int auctionID, double offerPrice, int userID) {
        if (auctionPurchaseItems.containsKey(auctionID)) {
            AuctionPurchase auctionPurchase = auctionPurchaseItems.get(auctionID);
            auctionPurchase.placeOffer(offerPrice, userID);
        } else {
            throw new IllegalArgumentException("Auction ID does not exist in the shop.");
        }
    }

	public Pair<Integer, Double> purchaseAuctionItem(int auctionID, int userID) {
		if(auctionPurchaseItems.containsKey(auctionID)) {
            AuctionPurchase auctionPurchase = auctionPurchaseItems.get(auctionID);
            try {
                if (!getItem(auctionPurchase.getItemId()).quantityCheck(1)) {
                    throw new IllegalArgumentException("Item is out of stock.");
                }
                return auctionPurchase.purchaseAuctionItem(userID);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Auction purchase failed: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Auction ID does not exist in the shop.");
        }
	}
}


