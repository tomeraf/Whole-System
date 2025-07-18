package com.halilovindustries.backend.Domain.DomainServices;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IExternalSystems;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;

import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.AuctionPurchase;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.BidPurchase;
import com.halilovindustries.backend.Domain.User.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

public class PurchaseService {
    private static volatile PurchaseService instance;

    // Dependency for concurrency control (assumed to be provided externally)
    private final ConcurrencyHandler concurrencyHandler;

    private PurchaseService(ConcurrencyHandler concurrencyHandler) {
        this.concurrencyHandler = concurrencyHandler;
    }

    /**
     * Returns the singleton instance, creating it if necessary.
     * Uses double-checked locking for thread safety.
     */
    public static PurchaseService getInstance(ConcurrencyHandler concurrencyHandler) {
        if (instance == null) {
            synchronized (PurchaseService.class) {
                if (instance == null) {
                    instance = new PurchaseService(concurrencyHandler);
                }
            }
        }
        return instance;
    }

    // Use case #2.3: Add item to cart
    public void addItemsToCart(Guest user, Shop shop,int itemId, int quantity) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemId);
        itemLock.lock();
        try {
            ShoppingCart cart = user.getCart();
            int currentQuantity = cart.getItems().get(shop.getId()) == null ? 0 :
                                cart.getItems().get(shop.getId()).get(itemId) == null ? 0 :
                                cart.getItems().get(shop.getId()).get(itemId);
            if(shop.canAddItemToBasket(itemId, currentQuantity + quantity))
                cart.addItem(shop.getId(),itemId, quantity);
            else
                throw new IllegalArgumentException("Error: item cannot be added to cart.");
        } finally {
            itemLock.unlock();
        }
    }
    
    // use case 2.5
    @Transactional
    public Order buyCartContent(Guest user, List<Shop> shops, IShipment ship, IPayment pay, int orderID,
                                    PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails,
                                    IExternalSystems monitor, Function<Integer, Shop> shopFetcher) {
        if (shops.isEmpty()) {
            throw new IllegalArgumentException("Error: no shops to purchase from.");    
        }
        if (!monitor.handshake()) {
            throw new IllegalStateException("External services are unavailable. Try again later.");
        }
        ShoppingCart cart = user.getCart();
        Map<Integer, Map<Integer, Integer>> items = cart.getItems();            //<shopId,<itemId,quantity>>
        HashMap<Shop, HashMap<Integer, Integer>> itemsToBuy = new HashMap<>();
        for(Shop shop : shops) {
            Map<Integer,Integer> itemsMap = items.get(shop.getId());
            if(itemsMap!=null) {
                itemsToBuy.put(shop, new HashMap<>(itemsMap));
            }
        }
        if(!(ship.validateShipmentDetails(shipmentDetails) && pay.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
        // Acquire fine-grained locks per item
        List<Lock> acquiredLocks = new ArrayList<>();
        try {
            // Build list of locks (shopId, itemId) pairs
            List<Pair<Integer, Integer>> locksToAcquire = new ArrayList<>();
            for (Shop shop : itemsToBuy.keySet()) {
                int shopId = shop.getId();
                for (Integer itemId : itemsToBuy.get(shop).keySet()) {
                    locksToAcquire.add(new Pair<>(shopId, itemId));
                }
            }
            // Sort to avoid deadlocks
            locksToAcquire.sort((a, b) -> {
                int cmp = a.getKey().compareTo(b.getKey());
                return (cmp != 0) ? cmp : a.getValue().compareTo(b.getValue());
            });
            
            // Lock each item
            for (Pair<Integer, Integer> p : locksToAcquire) {
                ReentrantLock itemLock = concurrencyHandler.getItemLock(p.getKey(), p.getValue());
                itemLock.lockInterruptibly();
                acquiredLocks.add(itemLock);
            }
            for(Shop shop : itemsToBuy.keySet()) {
                // Fetch the latest shop instance to ensure we have the most up-to-date state *after* aquiring item locks
                shop = shopFetcher.apply(shop.getId());    // Ensure we have the latest shop instance
                itemsToBuy.put(shop, itemsToBuy.get(shop)); // Update the items to buy with the latest shop instance
                HashMap<Integer, Integer> itemsMap = itemsToBuy.get(shop);
                if(!shop.canPurchaseBasket(itemsMap)) {
                    throw new IllegalArgumentException("Error: cant purchase items.");
                }
            }
            HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
            for(Shop shop : itemsToBuy.keySet()) {
                itemsToShip.put(shop.getId(),checkCartContent(user, List.of(shop)).getKey());
            }
            
            // 1. Calculate the total price without reducing inventory
            Double total = 0.0;
            HashMap<Shop, HashMap<Integer, Integer>> validatedItems = new HashMap<>();
            for(Shop shop : itemsToBuy.keySet()) {
                HashMap<Integer, Integer> itemsMap = itemsToBuy.get(shop);
                // Just calculate the price without reducing quantity
                total += shop.calculateBasketPrice(itemsMap);
                validatedItems.put(shop, itemsMap);
            }

            // 2. Process payment first
            Integer paymentId = pay.processPayment(total, paymentDetails);
            if (paymentId == null) {
                throw new IllegalArgumentException("Error: payment processing failed.");
            }

            // 3. Process shipment
            Integer shipmentId = ship.processShipment(shipmentDetails);
            if (shipmentId == null) {
                throw new IllegalArgumentException("Error: shipment processing failed.");
            }

            // 4. Only now updating inventory
            for(Shop shop : validatedItems.keySet()) {
                HashMap<Integer, Integer> itemsMap = validatedItems.get(shop);
                shop.updateInventory(itemsMap);
            }

            cart.clearCart();
            Order order = new Order(orderID, user.getUserID(), total, itemsToShip.values().stream().flatMap(List::stream).toList(),paymentId, shipmentId);
            return order;
        } 
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while locking items", ie);
        } finally {
            // Release locks in reverse order
            Collections.reverse(acquiredLocks);
            for (Lock lock : acquiredLocks) {
                lock.unlock();
            }
        }
    }


    public Pair<List<ItemDTO>,Boolean> checkCartContent(Guest user,List<Shop> shops)
    {
        List<ItemDTO> cart = new ArrayList<>();
        user.getCart().update();
        Map<Integer,Map<Integer,Integer>> items = user.getCart().getItems();
        boolean isBadItem = false;
        for(Shop shop : shops) {
            if (items.get(shop.getId()) == null){
                continue;
            }
            Map<Integer,Integer> itemsMap = items.get(shop.getId());    
            Pair<HashMap<Item,Double>,List<Integer>> discountedPricesPair= shop.getDiscountedPrices(new HashMap<>(itemsMap));//returns updated prices and removes items that cannot be purchased(due to quantity))
            List<Integer> badItems = discountedPricesPair.getValue();
            for(Integer itemId : itemsMap.keySet()) {
                if(discountedPricesPair.getKey().keySet().stream().filter(item -> item.getId() == itemId).toList().size() == 0 && !discountedPricesPair.getValue().contains(itemId)) {
                    isBadItem = true;
                    user.getCart().deleteItem(shop.getId(), itemId);
                }
            }
            for(Integer badItem : badItems) {
                isBadItem=true;
                user.getCart().deleteItem(shop.getId(), badItem);
            }
            HashMap<Item,Double> discountedPrices = discountedPricesPair.getKey();
            List<ItemDTO> itemsList = new ArrayList<>();
            for(Item item : discountedPrices.keySet()) {
                int quantity = itemsMap.get(item.getId());
                itemsList.add(new ItemDTO(item.getName(), item.getCategory(), discountedPrices.get(item), shop.getId(), item.getId(), itemsMap.get(item.getId()), item.getRating(), item.getDescription(), item.getNumOfOrders()));
            }
            cart.addAll(itemsList);
        }

        return new Pair<>(cart, isBadItem);
    }

    public void submitBidOffer(Guest user,Shop shop ,int itemId, double offer)
    {
        ReentrantLock lock = concurrencyHandler.getItemLock(shop.getId(), itemId);
        try {
            lock.lockInterruptibly();
            if(user instanceof Registered) {
                shop.addBidPurchase(itemId, offer,user.getUserID());
            }
            else {
                throw new IllegalArgumentException("Error: guest cannot submit bid.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // handle interruption…
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Order purchaseBidItem(Guest guest, Shop shop, int bidId, int orderID, IPayment pay, IShipment ship, 
                            PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails, 
                            IExternalSystems monitor) {
        if(!(ship.validateShipmentDetails(shipmentDetails) && pay.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
        if (!monitor.handshake()) {
            throw new IllegalStateException("External services are unavailable. Try again later.");
        }
        
        // Get bid details without updating inventory
        BidPurchase bidPurchase = shop.getBidPurchase(bidId);
        int itemId = bidPurchase.getItemId();
        
        // VALIDATE ITEM AVAILABILITY FIRST
        if (!shop.getItem(itemId).quantityCheck(1)) {
            throw new IllegalArgumentException("Error: item is out of stock.");
        }
        double price = shop.calculateBidPrice(bidId);
        
        // Process payment first
        Integer paymentId = pay.processPayment(price, paymentDetails);
        if (paymentId == null) {
            throw new IllegalArgumentException("Error: payment processing failed.");
        }
        
        // Process shipment
        Integer shipmentId = ship.processShipment(shipmentDetails);
        if (shipmentId == null) {
            throw new IllegalArgumentException("Error: shipment processing failed.");
        }
        
        // Only update inventory after successful payment and shipment
        shop.updateBidInventory(bidId);
        Pair<Integer,Double> offer = bidPurchase.purchaseBidItem(guest.getUserID());
        
        // Create order
        HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
        List<ItemDTO> itemsList = new ArrayList<>();
        Item item = shop.getItem(itemId);
        itemsList.add(new ItemDTO(item.getName(), item.getCategory(), price, 
                                shop.getId(), itemId, 1, item.getRating(), 
                                item.getDescription(), item.getNumOfOrders()));
        itemsToShip.put(shop.getId(), itemsList);
        
        Order order = new Order(orderID, guest.getUserID(), price, 
                            itemsToShip.values().stream().flatMap(List::stream).toList(),
                            paymentId, shipmentId);
        return order;
    }

    public void submitAuctionOffer(Registered user, Shop shop, int auctionID, double offerPrice) {
        shop.submitAuctionOffer(auctionID, offerPrice, user.getUserID());
    }

    @Transactional
public Order purchaseAuctionItem(Registered user, Shop shop, int auctionID, int orderID, 
                           IPayment payment, IShipment shipment, 
                           PaymentDetailsDTO paymentDetails, ShipmentDetailsDTO shipmentDetails, 
                           IExternalSystems monitor) {

        if(!(shipment.validateShipmentDetails(shipmentDetails) & payment.validatePaymentDetails(paymentDetails))){
            throw new IllegalArgumentException("Error: cant validate payment or shipment details.");
        }
        if (!monitor.handshake()) {
            throw new IllegalStateException("External services are unavailable. Try again later.");
        }
        
        // Get auction details first - do this only once
        AuctionPurchase auctionPurchase = shop.getAuctionPurchase(auctionID);
        int itemId = auctionPurchase.getItemId();
        
        // VALIDATE ITEM AVAILABILITY FIRST
        if (!shop.getItem(itemId).quantityCheck(1)) {
            throw new IllegalArgumentException("Error: item is out of stock.");
        }

        // Calculate price (without duplicating the auction lookup)
        double price = shop.calculateAuctionPrice(auctionID);
        
        // Process payment first
        Integer paymentId = payment.processPayment(price, paymentDetails);
        if (paymentId == null) {
            throw new IllegalArgumentException("Error: payment processing failed.");
        }
        
        // Process shipment
        Integer shipmentId = shipment.processShipment(shipmentDetails);
        if (shipmentId == null) {
            throw new IllegalArgumentException("Error: shipment processing failed.");
        }
        
        // Only update inventory after successful payment and shipment
        shop.updateAuctionInventory(auctionID);
        Pair<Integer,Double> offer = auctionPurchase.purchaseAuctionItem(user.getUserID());
        
        // Create order
        HashMap<Integer, List<ItemDTO>> itemsToShip = new HashMap<>();
        List<ItemDTO> itemsList = new ArrayList<>();
        Item item = shop.getItem(itemId);
        itemsList.add(new ItemDTO(item.getName(), item.getCategory(), price, 
                                shop.getId(), itemId, 1, item.getRating(), 
                                item.getDescription(), item.getNumOfOrders()));
        itemsToShip.put(shop.getId(), itemsList);
        
        Order order = new Order(orderID, user.getUserID(), price, 
                            itemsToShip.values().stream().flatMap(List::stream).toList(),
                            paymentId, shipmentId);
        return order;
    }

    public void answerOnCounterBid(Registered user, Shop shop, int bidId, boolean accept) {
        shop.answerOnCounterBid(bidId, accept, user.getUserID());
        // Set<Integer> managersId = shop.getManagerIDs();
        // managersId.stream().filter()




        // return notifyBid(shop.getBidPurchase(bidId));
    }
    // private Pair<List<Integer>,String> notifyBid(BidPurchase bidPurchase) {
    //     Pair<List<Integer>,String> notificationPair= null;
    //     if(bidPurchase.isAccepted()==1){
    //         notificationPair= new Pair(bidPurchase.getBuyerId() ,"Bid " + bidPurchase.getId() + " has been accepted by " + bidPurchase.getSubmitterId());
    //     } else if(bidPurchase.isAccepted()==-1){
    //         notificationPair =new Pair(bidPurchase.getBuyerId(), "Bid " + bidPurchase.getId() + " has been rejected by " + bidPurchase.getSubmitterId());
    //     }
    //     return notificationPair;
    // }
}
