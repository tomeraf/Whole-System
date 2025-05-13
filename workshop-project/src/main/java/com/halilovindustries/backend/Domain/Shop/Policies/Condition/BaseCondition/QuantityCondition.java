package Domain.Shop.Policies.Condition.BaseCondition;

import java.util.HashMap;

import Domain.Shop.Category;
import Domain.Shop.Item;

public class QuantityCondition extends BaseCondition {
    private int quantity;

    public QuantityCondition(int itemId,Category category, int quantity) {
        super(itemId,category);
        this.quantity = quantity;
    }
    public QuantityCondition(int itemId, int quantity) {
        super(itemId);
        this.quantity = quantity;
    }

    public QuantityCondition(Category category, int quantity) {
        super(category);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean checkItemCondition(HashMap<Item, Integer> allItems) {
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                return allItems.get(item) >= quantity;
            }
        }
        return false;
    }
    @Override
    public boolean checkCategoryCondition(HashMap<Item, Integer> allItems) {
        int totalQuantity = 0;
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                totalQuantity += allItems.get(item);
            }
        }
        return totalQuantity >= quantity;
    }
    @Override
    public boolean checkShopCondition(HashMap<Item, Integer> allItems) {
        int totalQuantity = 0;
        for (Item item : allItems.keySet()) {
            totalQuantity += allItems.get(item);
        }
        return totalQuantity >= quantity;
    }

    public String toString() {
        return String.format("%s, Minimum quantity required: %d", super.toString(), quantity);
    }

}
