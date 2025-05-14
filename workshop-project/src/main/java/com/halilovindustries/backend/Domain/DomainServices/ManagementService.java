package com.halilovindustries.backend.Domain.DomainServices;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.User.*;

public class ManagementService {
    private static ManagementService instance = null;
    private ManagementService() {
        // private constructor to prevent instantiation
    }
    public static ManagementService getInstance() {
        if (instance == null) {
            instance = new ManagementService();
        }
        return instance;
    }

    public Shop createShop(int shopId, Registered user, String name, String description) {
        Shop shop = new Shop(shopId,user.getUserID(), name, description);
        user.setRoleToShop(shopId, new Founder(shopId));
        return shop;
    }
    
    public void addOwner(Registered appointer, Shop shop, Registered appointee) {
        Owner owner = new Owner(appointer.getUserID(),shop.getId());
        if (shop.getOwnerIDs().contains(appointee.getUserID())) {
            throw new IllegalArgumentException("User is already an owner of the shop");
        }
        appointer.addOwner(shop.getId(), (int)appointee.getUserID(), owner);
        appointee.setRoleToShop(shop.getId(), owner);
        shop.addOwner(appointee.getUserID());
    }

    public void removeAppointment(Registered appointer, Shop shop, Registered userToRemove) {
        List<Integer> idsToRemove = appointer.removeAppointment(shop.getId(), userToRemove.getUserID());
        shop.removeAppointment(idsToRemove);
    }

    public void addManager(Registered appointer, Shop shop, Registered appointee, Set<Permission> permission) {
        Manager manager = new Manager(appointer.getUserID(),shop.getId(), permission);
        if (shop.getManagerIDs().contains(appointee.getUserID())) {
            throw new IllegalArgumentException("User is already a manager of the shop");
        }
        appointer.addManager(shop.getId(), appointee.getUserID(), manager);
        appointee.setRoleToShop(shop.getId(), manager);   
        shop.addManager(appointee.getUserID());
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
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_SUPPLY)) {
            shop.removeItem(itemID);
        } else {
            throw new IllegalArgumentException("You don't have permission to remove items from the shop");
        }
    }
    public void updateItemName(Registered supplyManager, Shop shop, int itemID, String name) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
            shop.updateItemName(itemID, name);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item name");
        }
    }
    public void updateItemPrice(Registered supplyManager, Shop shop, int itemID, double price) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_PRICE)) {
            shop.updateItemPrice(itemID, price);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item price");
        }
    }
    public void updateItemQuantity(Registered supplyManager, Shop shop, int itemID, int quantity) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_QUANTITY)) {
            shop.updateItemQuantity(itemID, quantity);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item quantity");
        }
    }
    public void updateItemRating(Registered supplyManager, Shop shop, int itemID, double rating) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_RATING)) {
            shop.updateItemRating(itemID, rating);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item rating");
        }
    }
    public void updateItemCategory(Registered supplyManager, Shop shop, int itemID, Category category) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
            shop.updateItemCategory(itemID, category);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item category");
        }
    }
    //Not to forget purchase and sale policy
    public void updateItemDescription(Registered supplyManager, Shop shop, int itemID, String description) {
        if (supplyManager.hasPermission(shop.getId(), Permission.UPDATE_ITEM_DESCRIPTION)) {
            shop.updateItemDescription(itemID, description);
        } else {
            throw new IllegalArgumentException("You don't have permission to update item description");
        }
    }
    
    public void updatePurchaseType(Registered supplyManager, Shop shop, String purchaseType) {
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

    public void closeShop(Registered supplyManager, Shop shop) {
        if (supplyManager.isSystemManager()|| supplyManager.hasPermission(shop.getId(), Permission.CLOSE_SHOP)) {
            shop.closeShop();
        } else {
            throw new IllegalArgumentException("You don't have permission to close the shop");
        }
    }

    public List<Integer> getMembersPermissions(Registered supplyManager, Shop shop) {
        List<Integer> permissions = new ArrayList<>();
        if(supplyManager.hasPermission(shop.getId(), Permission.VIEW)){
            permissions.addAll(shop.getManagerIDs());
            permissions.addAll(shop.getOwnerIDs());
        }
        else {
            throw new IllegalArgumentException("You don't have permission to view members permissions");
        }
        return permissions;
    }
	public void answerBid(Registered user, Shop shop, int bidID, boolean accept) {
        if (user.hasPermission(shop.getId(), Permission.ANSWER_BID)) {
            shop.addBidDecision(user.getUserID(),bidID, accept);
        } else {
            throw new IllegalArgumentException("You don't have permission to answer bids");
        }
	}
    public void submitCounterBid(Registered user, Shop shop, int bidID, double offerAmount) {
        if (user.hasPermission(shop.getId(), Permission.ANSWER_BID)) {
            shop.submitCounterBid(user.getUserID(), bidID, offerAmount);
        } else {
            throw new IllegalArgumentException("You don't have permission to submit counter bids");
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
    public void removeDiscount(Registered user, Shop shop, int discountID) {
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
    public void removePurchaseCondition(Registered user, Shop shop, int conditionID) {
        if (user.hasPermission(shop.getId(), Permission.UPDATE_PURCHASE_POLICY)) {
            shop.removePurchaseCondition(conditionID);
        } else {
            throw new IllegalArgumentException("You don't have permission to remove purchase conditions");
        }
    }

}
