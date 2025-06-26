package com.halilovindustries.backend.Domain.DomainServices;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.BidPurchase;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.User.*;
import com.halilovindustries.backend.Infrastructure.MemoryShopRepository;

public class ManagementService {
    private static ManagementService instance = null;
    private final ConcurrencyHandler concurrencyHandler;
    // Spring יזריק את ה־ConcurrencyHandler singleton
    private ManagementService(ConcurrencyHandler concurrencyHandler) {
        this.concurrencyHandler = concurrencyHandler;
        // מאתחלים את השדה ה־static
        instance = this;
    }
    public static ManagementService getInstance(ConcurrencyHandler concurrencyHandler) {
        if (instance == null) {
            instance = new ManagementService(concurrencyHandler);
        }
        return instance;
    }
    
    public void addOwner(Registered appointer, Shop shop, Registered appointee) {
        ReentrantLock lock = concurrencyHandler.getShopUserLock(shop.getId(), appointee.getUsername());
        try {
            lock.lockInterruptibly();
            Owner owner = new Owner(appointer.getUserID(),shop.getId());
            if (shop.getOwnerIDs().contains(appointee.getUserID())) {
                throw new IllegalArgumentException("User is already an owner of the shop");
            }
            if (shop.getManagerIDs().contains(appointee.getUserID())) {
                throw new IllegalArgumentException("User is already a manager of the shop");
            }
            appointer.addOwner(shop.getId(), (int)appointee.getUserID(), owner);
            appointee.setRoleToShop(shop.getId(), owner);
            shop.addOwner(appointee.getUserID());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // handle interruption
        } finally {
            lock.unlock();
        }
    }

    public void removeAppointment(Registered appointer, Shop shop, Registered userToRemove) {
        List<Integer> idsToRemove = appointer.removeAppointment(shop.getId(), userToRemove.getUserID());
        shop.removeAppointment(idsToRemove);
        }

    public void addManager(Registered appointer,int shopId, Registered appointee, Set<Permission> permission, Function<Integer,Shop> shopFetcher) {
       ReentrantLock lock = concurrencyHandler.getShopUserLock(shopId, appointee.getUsername());
        try {
          lock.lockInterruptibly();
            Shop shop = shopFetcher.apply(shopId); // Fetch the shop using the provided function
            Manager manager = new Manager(appointer.getUserID(),shop.getId(), permission);
            if (shop.getManagerIDs().contains(appointee.getUserID())) {
                throw new IllegalArgumentException("User is already a manager of the shop");
            }
            if (shop.getOwnerIDs().contains(appointee.getUserID())) {
                throw new IllegalArgumentException("User is already an owner of the shop");
            }
            appointer.addManager(shop.getId(), appointee.getUserID(), manager);
            appointee.setRoleToShop(shop.getId(), manager);
            shop.addManager(appointee.getUserID());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // handle interruption
        } 
        finally {
            lock.unlock();
        }
    }

    public void addPermission(Registered appointer, Shop shop, Registered appointee, Permission permission) {
        if(appointer.hasPermission(shop.getId(), Permission.UPDATE_PERMISSIONS)) {
            appointee.addPermission(shop.getId(), permission);
        } else {
            throw new IllegalArgumentException("You don't have permission to add permissions");
        }
    }
    public void removePermission(Registered appointer, Shop shop, Registered appointee, Permission permission) {
        if(appointer.hasPermission(shop.getId(), Permission.UPDATE_PERMISSIONS)) {
            appointee.removePermission(shop.getId(), permission);
        } else {
            throw new IllegalArgumentException("You don't have permission to remove permissions");
        }
    }
    
    public Item addItemToShop(Registered supplyManager, Shop shop, String name, Category category, double price, String description) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_SUPPLY)){
            return shop.addItem(name, category, price, description);
        }
        else {
            throw new IllegalArgumentException("You don't have permission to add items to the shop");
        }
    }
    public void removeItemFromShop(Registered supplyManager, Shop shop, int itemID) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemID);
        try {
            itemLock.lockInterruptibly();
            if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_SUPPLY)) {
                shop.removeItem(itemID);
            } else {
                throw new IllegalArgumentException("You don't have permission to remove items from the shop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            itemLock.unlock();
        }
    }
    public void updateItemName(Registered supplyManager, Shop shop, int itemID, String name) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemID);
        try {
            itemLock.lockInterruptibly();
            if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
                shop.updateItemName(itemID, name);
            } else {
                throw new IllegalArgumentException("You don't have permission to update item name");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            itemLock.unlock();
        }
    }
    public void updateItemPrice(Registered supplyManager, Shop shop, int itemID, double price) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemID);

        try {
            itemLock.lockInterruptibly();
            if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_PRICE)) {
                shop.updateItemPrice(itemID, price);
            } else {
                throw new IllegalArgumentException("You don't have permission to update item price");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            itemLock.unlock();
        }
    }
    public void updateItemQuantity(Registered supplyManager, Shop shop, int itemID, int quantity) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemID);
        try {
            itemLock.lockInterruptibly();
            if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_QUANTITY)) {
                shop.updateItemQuantity(itemID, quantity);
            } else {
                throw new IllegalArgumentException("You don't have permission to update item quantity");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            itemLock.unlock();
        }
    }
    // public void updateItemRating(Registered supplyManager, Shop shop, int itemID, double rating) {
    //     if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_RATING)) {
    //         shop.updateItemRating(supplyManager.getUserID(), itemID, rating);
    //     } else {
    //         throw new IllegalArgumentException("You don't have permission to update item rating");
    //     }
    // }
    public void updateItemCategory(Registered supplyManager, Shop shop, int itemID, Category category) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
            shop.updateItemCategory(itemID, category);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item category");
        }
    }
    //Not to forget purchase and sale policy
    public void updateItemDescription(Registered supplyManager, Shop shop, int itemID, String description) {
        ReentrantLock itemLock = concurrencyHandler.getItemLock(shop.getId(), itemID);
        try {
            itemLock.lockInterruptibly();
            if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
                shop.updateItemDescription(itemID, description);
            } else {
                throw new IllegalArgumentException("You don't have permission to update item description");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            itemLock.unlock();
        }
    }
    
    public void updatePurchaseType(Registered supplyManager, Shop shop, PurchaseType purchaseType) {
        if( supplyManager.hasPermission(shop.getId(), Permission.UPDATE_PURCHASE_POLICY)) {
            shop.updatePurchaseType(purchaseType);
        } else {
            throw new IllegalArgumentException("You don't have permission to update purchase type");
        }
    }

    public void updateDiscountType(Registered supplyManager, Shop shop, DiscountType discountType) {
        if( supplyManager.hasPermission(shop.getId(), Permission.UPDATE_DISCOUNT_POLICY)) {
            shop.updateDiscountType(discountType);
        } else {
            throw new IllegalArgumentException("You don't have permission to update discount type");
        }
    }

    public void closeShop(Registered supplyManager, Shop shop,Registered founder) {
        Lock shopWrite = concurrencyHandler.getShopWriteLock(shop.getId());
        shopWrite.lock();
        try {
            if(supplyManager.isSystemManager()){
                closeShopAsSystemManager(supplyManager, shop,founder);     
            } else if (supplyManager.getAppointer(shop.getId())==-1) {
                shop.closeShop();
            } else {
                throw new IllegalArgumentException("You don't have permission to close the shop");
            }
        } finally {
            shopWrite.unlock();
        }
    }
    private void closeShopAsSystemManager(Registered systemManager, Shop shop,Registered founder) {
        shop.closeShop();
        shop.clearRoles();
        founder.getRoleInShop(shop.getId()).removeAllAppointments();
        founder.removeRoleFromShop(shop.getId());
    }

    public List<Permission> getMembersPermissions(Registered supplyManager, Shop shop,Registered member) {

        if(supplyManager.hasPermission(shop.getId(), Permission.VIEW)){
            return member.getPermissions(shop.getId());
        }
        else {
            throw new IllegalArgumentException("You don't have permission to view members permissions");
        }
    }
	public Pair<Integer,String> answerBid(Registered user, Shop shop, int bidID, boolean accept,List<Integer> members) {//Pair<Integer,String> is used to return the id of the buyer the bid and the message
        ReentrantLock bidLock = concurrencyHandler.getBidLock(shop.getId(), bidID);
        try {
            bidLock.lockInterruptibly();
            if (user.hasPermission(shop.getId(), Permission.ANSWER_BID)) {
                shop.addBidDecision(user.getUserID(),bidID, accept, members);
                BidPurchase bidPurchase = shop.getBidPurchase(bidID);
                return notifyBid(bidPurchase);
            } else {
                throw new IllegalArgumentException("You don't have permission to answer bids");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            bidLock.unlock();
        }
    }
    private Pair<Integer,String> notifyBid(BidPurchase bidPurchase) {
        Pair<Integer,String> notificationPair= null;
        if(bidPurchase.isAccepted()==1){
            notificationPair= new Pair(bidPurchase.getBuyerId() ,"Bid " + bidPurchase.getId() + " has been accepted by all members");
        } else if(bidPurchase.isAccepted()==-1){
            notificationPair =new Pair(bidPurchase.getId(), "Bid " + bidPurchase.getId() + " has been rejected by " + bidPurchase.getRejecterId());
        }
        return notificationPair;
    }
    public void submitCounterBid(Registered user, Shop shop, int bidID, double offerAmount) {
        ReentrantLock bidLock = concurrencyHandler.getBidLock(shop.getId(), bidID);
        try {
            bidLock.lockInterruptibly();
            if (user.hasPermission(shop.getId(), Permission.ANSWER_BID)) {
                shop.submitCounterBid(user.getUserID(), bidID, offerAmount);
            } else {
                throw new IllegalArgumentException("You don't have permission to submit counter bids");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            bidLock.unlock();
        }
    }
    public void openAuction(Registered user, Shop shop, int itemID, double startingPrice, LocalDateTime startDate,
            LocalDateTime endDate) {
        if (user.hasPermission(shop.getId(), Permission.OPEN_AUCTION)) {
            shop.openAuction(itemID, startingPrice, startDate, endDate);
        } else {
            throw new IllegalArgumentException("You don't have permission to open an auction");
        }
    }
        public void addDiscount(Registered user, Shop shop, DiscountDTO discountDetails) {
        if (user.hasPermission(shop.getId(), Permission.UPDATE_DISCOUNT_POLICY)) {
            shop.addDiscount(discountDetails);
        } else {
            throw new IllegalArgumentException("You don't have permission to add discounts");
        }
    }
    public void removeDiscount(Registered user, Shop shop, String discountID) {
        if (user.hasPermission(shop.getId(), Permission.UPDATE_DISCOUNT_POLICY)) {
            shop.removeDiscount(discountID);
        } else {
            throw new IllegalArgumentException("You don't have permission to remove discounts");
        }
    }
    public void addPurchaseCondition(Registered user, Shop shop, ConditionDTO condition) {
        if (user.hasPermission(shop.getId(), Permission.UPDATE_PURCHASE_POLICY)) {
            shop.addPurchaseCondition(condition);
        } else {
            throw new IllegalArgumentException("You don't have permission to add purchase conditions");
        }
    }
    public void removePurchaseCondition(Registered user, Shop shop, String conditionID) {
        if (user.hasPermission(shop.getId(), Permission.UPDATE_PURCHASE_POLICY)) {
            shop.removePurchaseCondition(conditionID);
        } else {
            throw new IllegalArgumentException("You don't have permission to remove purchase conditions");
        }
    }
    public List<ConditionDTO> getPurchaseConditions(Registered user, Shop shop) {
        if( user.hasPermission(shop.getId(), Permission.VIEW)) {
            return shop.getPurchaseConditions();
        } else {
            throw new IllegalArgumentException("You don't have permission to view purchase conditions");
        }
    }
    public List<DiscountDTO> getDiscounts(Registered user, Shop shop) {
        if( user.hasPermission(shop.getId(), Permission.VIEW)) {
            return shop.getDiscounts();
        } else {
            throw new IllegalArgumentException("You don't have permission to view discounts");
        }
    }
    public List<BidDTO> getBids(Registered user, Shop shop) {
        if( user.hasPermission(shop.getId(), Permission.ANSWER_BID)) {
            return shop.getBids().stream().filter(bid -> bid.getCounterBidId() == -1).toList();
        } else {
            throw new IllegalArgumentException("You don't have permission to view bids");
        }
    }

//    public void reOpenShop(Registered supplyManager, Shop shop,Registered founder) {
//        Lock shopWrite = concurrencyHandler.getShopWriteLock(shop.getId());
//        shopWrite.lock();
//        try {
//            if(supplyManager.isSystemManager()){
//                openShopAsSystemManager(supplyManager, shop,founder);
//            } else if (supplyManager.getAppointer(shop.getId())==-1) {
//                shop.openShop();
//            } else {
//                throw new IllegalArgumentException("You don't have permission to close the shop");
//            }
//        } finally {
//            shopWrite.unlock();
//        }
//
//
//
//    }
}
