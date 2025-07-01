package com.halilovindustries.backend.Domain.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
public class ShoppingCart {
    @Id
    @Column(name = "cart_id")
    private int cartID;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShoppingBasket> baskets;


    public ShoppingCart() {
        this.baskets = new ArrayList<>();
    }
    public ShoppingCart(List<ShoppingBasket> baskets, int cartID) {
        this.baskets = baskets;
        this.cartID = cartID;
    }


    public ShoppingCart(int cartID) {
        this.cartID = cartID;
        this.baskets = new ArrayList<>();
    }


    public int getCartID() {
        return cartID;
    }


    // Use case #2.3: Add item to cart
    public void addItem(int shopID, int itemID, int quantity) {
        ShoppingBasket basket = baskets.stream()
            .filter(b -> b.getShopID() == shopID)
            .findFirst()
            .orElseGet(() -> {
                ShoppingBasket newBasket = new ShoppingBasket(shopID);
                newBasket.setCart(this);
                baskets.add(newBasket);
                return newBasket;
            });
        basket.addItem(itemID, quantity);
    }


    // Use case #2.4.a: Check cart content
    // Use case #2.5: Buy items in cart - get all items in cart
    public Map<Integer, Map<Integer, Integer>> getItems() {
        Map<Integer, Map<Integer, Integer>> items = new HashMap<>();
        for (ShoppingBasket basket : baskets) {
            items.put(basket.getShopID(), basket.getItems());
        }
        return items;
    }


    // Use case #2.4.b: Change cart content
    public boolean deleteItem(int shopID, int itemID) {
        Iterator<ShoppingBasket> iterator = baskets.iterator();
        while (iterator.hasNext()) {
            ShoppingBasket basket = iterator.next();
            if (basket.getShopID() == shopID) {
                boolean removed = basket.removeItem(itemID);
                if (basket.getItems().isEmpty()) {
                    iterator.remove(); 
                }
                return removed;
            }
        }
        return false;
    }


    // Use case #2.5: Buy items in cart - after confirmation, the items are removed from the cart
    public void clearCart() {
        for (ShoppingBasket basket : baskets) {
            basket.clearBasket();
        }
        baskets.clear();
    }
    public List<ShoppingBasket> getBaskets() {
        return baskets;
    }
    public List<Integer> getShopIDs() {
        List<Integer> shopIDs = new ArrayList<>();
        for (ShoppingBasket basket : baskets) {
            shopIDs.add(basket.getShopID());
        }
        return shopIDs;
    }
    public void update() {
        baskets.removeIf(basket -> basket.getItems().isEmpty());
    }
}


