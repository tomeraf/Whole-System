package com.halilovindustries.backend.Domain.User;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
public class ShoppingBasket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int shopID;

    @ElementCollection
    @MapKeyColumn(name = "item_id")
    @Column(name = "quantity")
    @CollectionTable(name = "basket_items", joinColumns = @JoinColumn(name = "basket_id"))
    private Map<Integer,Integer> items= new HashMap<>(); //<itemID, Quantity> 

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id")
    private ShoppingCart cart;

    // Default constructor required by JPA
    protected ShoppingBasket() {

    }
    public ShoppingBasket(int shopID) {
        this.shopID = shopID;
        this.items = new HashMap<Integer,Integer>();
    }

    public int getShopID() {
        return shopID;
    }

    public Map<Integer,Integer> getItems() {
        return items;
    }

    public void addItem(int itemID, int quantity) {
        if(quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        items.merge(itemID, quantity, Integer::sum);
    }

    public boolean removeItem(int itemID) {
        // Check if the itemID is in the list of items
        if (items.containsKey(itemID)) {
            items.remove(itemID); // Remove the item from the basket
            return true; // Item removed successfully
        }
        return false; // Item not found, nothing removed
    }

    public void clearBasket() {
        items.clear(); // Clear all items from the basket
    }
    public void setCart(ShoppingCart cart) {
        this.cart = cart;
    }
    public ShoppingCart getCart() {
        return cart;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}
