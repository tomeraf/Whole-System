package Domain.User;

import java.util.List;

import Domain.DTOs.ItemDTO;

public class ShoppingBasket {
    private int shopID;
    private List<ItemDTO> items;


    public ShoppingBasket(int shopID, List<ItemDTO> items) {
        this.shopID = shopID;
        this.items = items;
    }


    public ShoppingBasket(int shopID) {
        this.shopID = shopID;
        this.items = new java.util.ArrayList<>();
    }


    public int getShopID() {
        return shopID;
    }


    public List<ItemDTO> getItems() {
        return items;
    }


    public boolean isItemIn(int itemID) {
        // Check if the itemID is in the list of items
        for (int i = 0; i < items.size(); i++) {
            int tempID = ((ItemDTO) items.get(i)).getItemID();
            if (tempID == itemID) {
                return true; // Item found
            }
        }
        return false; // Item not found
    }


    public boolean addItem(ItemDTO item) {
        // Check if the itemID is already in the list of items
        for (int i = 0; i < items.size(); i++) {
            int tempID = ((ItemDTO) items.get(i)).getItemID();
            if (tempID == item.getItemID()) {
                return false; // Item already exists, not added
            }
        }
        items.add(item); // Add the new item to the list
        return true; // Item added successfully
    }


    public boolean removeItem(int itemID) {
        // Check if the itemID is in the list of items
        for (int i = 0; i < items.size(); i++) {
            int tempID = ((ItemDTO) items.get(i)).getItemID();
            if (tempID == itemID) {
                items.remove(i);
                return true; // Item removed successfully
            }
        }
        return false; // Item not found, nothing removed
    }


    public void clearBasket() {
        items.clear(); // Clear all items from the basket
    }
}
