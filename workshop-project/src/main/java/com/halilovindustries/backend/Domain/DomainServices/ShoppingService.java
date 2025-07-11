package com.halilovindustries.backend.Domain.DomainServices;

import java.util.List;

import com.halilovindustries.backend.Domain.Shop.Shop;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;

public class ShoppingService {

    public boolean RateItem(Shop shop, int raterId, int itemId, List<Order> allUserOrders, int rating) {
        for (Order order : allUserOrders) {
            for (ItemDTO orderItem : order.getItems()) {
                if (orderItem.getItemID() == itemId && orderItem.getShopId() == shop.getId()) {
                    shop.updateItemRating(raterId, itemId, rating);
                    return true; // Item found and rating updated
                }
            }
        }
        throw new IllegalArgumentException("Item not found in user's orders.");
    }

    public boolean RateShop(Shop shop, List<Order> allUserOrders, int raterId, int rating) {
        int shopId = shop.getId();
        for (Order order : allUserOrders) {
            for (ItemDTO orderItem : order.getItems()) {
                if (orderItem.getShopId() == shopId) {
                    shop.updateRating(raterId, rating);
                    return true; // Shop found and rating updated
                }
            }
        }
        throw new IllegalArgumentException("Shop not found in user's orders.");
    }

    // public List<Order> userPurchaseHistory() method is not needed here
    // it should be in UserService (by doing IOrderRepository.getOrdersByCustomerId(userId))

    public List<Order> shopPurchaseHistory(int ownerId, List<Order> allShopOrders, Shop shop) {
        if (shop.getOwnerIDs().contains(ownerId)) {
            // The user is the owner of the shop, proceed to display the purchase history
            return allShopOrders;
        }
        throw new IllegalArgumentException("You are not the owner of this shop.");
    }
}
