package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.HashMap;

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
        for (Discount discount : getDiscounts().values()) {
            totalDiscount += discount.calculateDiscount(allItems);
        }
        return totalDiscount;
    }
}
