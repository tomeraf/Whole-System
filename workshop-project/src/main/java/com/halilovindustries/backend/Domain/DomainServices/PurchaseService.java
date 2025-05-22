package com.halilovindustries.backend.Domain.DomainServices;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;

import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.BidPurchase;
import com.halilovindustries.backend.Domain.User.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

public class PurchaseService {
    // Use case #2.3: Add item to cart
    public void addItemsToCart(Guest user, Shop shop,int itemId, int quantity) {
        ShoppingCart cart = user.getCart();
        if(shop.canAddItemToBasket(itemId, quantity))
            cart.addItem(shop.getId(),itemId, quantity);
        else
            throw new IllegalArgumentException("Error: item cannot be added to cart.");
    }
    
    // use case 2.5
    public Order buyCartContent(Guest user, List<Shop> shops, IShipment ship, IPayment pay, int orderID, PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails) {
        if (shops.isEmpty()) {
            throw new IllegalArgumentException("Error: no shops to purchase from.");    
        }
        ShoppingCart cart = user.getCart();
        HashMap<Integer,HashMap<Integer,Integer>> items = cart.getItems();
        HashMap<Shop, HashMap<Integer, Integer>> itemsToBuy = new HashMap<>();
        for(Shop shop : shops) {
            HashMap<Integer,Integer> itemsMap = items.get(shop.getId());
            if(itemsMap!=null) {
                itemsToBuy.put(shop, itemsMap);
            }
        }
        if(!(ship.validateShipmentDetails(shipmentDetails) && pay.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
        for(Shop shop : itemsToBuy.keySet()) {
            HashMap<Integer, Integer> itemsMap = itemsToBuy.get(shop);
            if(!shop.canPurchaseBasket(itemsMap)) {
                throw new IllegalArgumentException("Error: cant purchase items.");
            }
        }
        HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
        for(Shop shop : itemsToBuy.keySet()) {
            itemsToShip.put(shop.getId(),checkCartContent(user, List.of(shop)));
        }
        Double total = 0.0;
        for(Shop shop : itemsToBuy.keySet()) {
            HashMap<Integer, Integer> itemsMap = itemsToBuy.get(shop);
            total=+shop.purchaseBasket(itemsMap);
        }
        ship.processShipment(total*0.1, shipmentDetails);
        pay.processPayment(total, paymentDetails);
        cart.clearCart();
        Order order = new Order(orderID, user.getUserID(), total, itemsToShip);
        return order;
    }


    public List<ItemDTO> checkCartContent(Guest user,List<Shop> shops)
    {
        List<ItemDTO> cart = new ArrayList<>();
        HashMap<Integer,HashMap<Integer,Integer>> items = user.getCart().getItems();
        for(Shop shop : shops) {
            HashMap<Integer,Integer> itemsMap = items.get(shop.getId());
            HashMap<Item,Double> discountedPrices= shop.getDiscountedPrices(itemsMap);//returns updated prices and removes items that cannot be purchased(due to quantity))
            List<ItemDTO> itemsList = new ArrayList<>();
            for(Item item : discountedPrices.keySet()) {
                int quantity = itemsMap.get(item.getId());
                itemsList.add(new ItemDTO(item.getName(), item.getCategory(), discountedPrices.get(item), shop.getId(), item.getId(), itemsMap.get(item.getId()), item.getRating(), item.getDescription(), item.getNumOfOrders()));
            }
            cart.addAll(itemsList);
        }
        return cart;
    }

    // public void directPurchase(Guest user, int itemId)
    // {
    //     // something with immediate purchase


    // }

    public void submitBidOffer(Guest user,Shop shop ,int itemId, double offer)
    {
        if(user instanceof Registered) {
            shop.addBidPurchase(itemId, offer,user.getUserID());
        }
        else {
            throw new IllegalArgumentException("Error: guest cannot submit bid.");
        }
    }

	public Order purchaseBidItem(Guest guest, Shop shop, int bidId,int orderID,IPayment pay,IShipment ship, PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails) {
        if(!(ship.validateShipmentDetails(shipmentDetails) && pay.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
		Pair<Integer,Double> offer = shop.purchaseBidItem(bidId, guest.getUserID());
        HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
        List<ItemDTO> itemsList = new ArrayList<>();
        Item item = shop.getItem(offer.getKey());
        itemsList.add(new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(), offer.getKey(), 1, item.getRating(), item.getDescription(), item.getNumOfOrders()));
        itemsToShip.put(shop.getId(), itemsList);
        pay.processPayment(offer.getValue(), paymentDetails);
        ship.processShipment(offer.getValue()*0.1, shipmentDetails);
        Order order= new Order(orderID, guest.getUserID(), offer.getKey(), itemsToShip);
        return order;
    }

    public void submitAuctionOffer(Registered user, Shop shop, int auctionID, double offerPrice) {
        shop.submitAuctionOffer(auctionID, offerPrice, user.getUserID());
    }

    public Order purchaseAuctionItem(Registered user, Shop shop, int auctionID, int orderID, IPayment payment,
            IShipment shipment, PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails) {
        
        if(!(shipment.validateShipmentDetails(shipmentDetails)& payment.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
        Pair<Integer,Double> offer = shop.purchaseAuctionItem(auctionID, user.getUserID());
        HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
        List<ItemDTO> itemsList = new ArrayList<>();
        Item item = shop.getItem(offer.getKey());
        itemsList.add(new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), shop.getId(), offer.getKey(), 1, item.getRating(), item.getDescription(), item.getNumOfOrders()));
        itemsToShip.put(shop.getId(), itemsList);
        payment.processPayment(offer.getValue(), paymentDetails);
        shipment.processShipment(offer.getValue()*0.1, shipmentDetails);
        shop.updateItemQuantity(item.getId(), item.getQuantity()-1);
        Order order= new Order(offer.getKey(), user.getUserID(), offer.getValue(), itemsToShip);
        return order;
    }

    public Pair<Integer,String> answerOnCounterBid(Registered user, Shop shop, int bidId, boolean accept,List<Integer> members) {
        shop.answerOnCounterBid(bidId, accept, user.getUserID(),members);
        return notifyBid(shop.getBidPurchase(bidId));
    }
    private Pair<Integer,String> notifyBid(BidPurchase bidPurchase) {
        Pair<Integer,String> notificationPair= null;
        if(bidPurchase.isAccepted()==1){
            notificationPair= new Pair(bidPurchase.getBuyerId() ,"Bid " + bidPurchase.getId() + " has been accepted by all members");
        } else if(bidPurchase.isAccepted()==-1){
            notificationPair =new Pair(bidPurchase.getBuyerId(), "Bid " + bidPurchase.getId() + " has been rejected");
        }
        return notificationPair;
    }
}
