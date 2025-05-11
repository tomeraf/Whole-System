package Domain.Shop.Policies.Condition.BaseCondition;

import java.util.HashMap;
import Domain.Shop.Category;

import Domain.Shop.Item;

public class PriceCondition extends BaseCondition {
    private double price;

    public PriceCondition(double price) {
        super();
        this.price = price;
    }
    public PriceCondition(int itemID,Category category, double price) {
        super(itemID, category);
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public boolean checkItemCondition(HashMap<Item, Integer> allItems) {
        for (Item item : allItems.keySet()) {
            if (item.getId() == getItemId()) {
                return item.getPrice()*allItems.get(item) >= price;
            }
        }
        return false;
    }
    @Override
    public boolean checkCategoryCondition(HashMap<Item, Integer> allItems) {
        double totalPrice = 0;
        for (Item item : allItems.keySet()) {
            if (item.getCategory().equals(getCategory())) {
                totalPrice += item.getPrice()*allItems.get(item);
            }
        }
        return totalPrice >= price;
    }
    @Override
    public boolean checkShopCondition(HashMap<Item, Integer> allItems) {
        double totalPrice = 0;
        for (Item item : allItems.keySet()) {
            totalPrice += item.getPrice()*allItems.get(item);
        }
        return totalPrice >= price;
    }

    public String toString() {
        return String.format("%s, Minimum purchase amount required: %.2f", super.toString(), price);
    }

}
