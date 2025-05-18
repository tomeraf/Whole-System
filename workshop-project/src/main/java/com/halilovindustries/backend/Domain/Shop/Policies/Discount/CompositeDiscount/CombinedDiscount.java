package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;

public class CombinedDiscount extends CompositeDiscount {

    public CombinedDiscount() {
        super();
    }
    public CombinedDiscount(Discount discount1, Discount discount2) {
        super(discount1, discount2);
    }

    @Override
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        double totalDiscount = 0;
        HashMap<Item,Double> totalDiscounts = getPercentagePerItem(allItems);
        for (Item item : allItems.keySet()) {
            if (totalDiscounts.containsKey(item)) {
                totalDiscount += allItems.get(item) * ((1-totalDiscounts.get(item)))*item.getPrice();
            }
        }
        return totalDiscount;
    }
    @Override
    public HashMap<Item, Double> getPercentagePerItem(HashMap<Item, Integer> allItems) {
        HashMap<Item, Double> totalDiscounts = new HashMap<>();
        List<Discount> discounts = getDiscountsList();
        for (Discount discount : discounts){
            HashMap<Item, Double> currentDiscounts = discount.getPercentagePerItem(allItems);
            for (Item item : currentDiscounts.keySet()) {
                totalDiscounts.put(item, (totalDiscounts.getOrDefault(item, 1.0))*currentDiscounts.get(item));
            }
        }
        return totalDiscounts;
    }
}
