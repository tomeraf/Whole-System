package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.BaseDiscount;

public class MaxDiscount extends CompositeDiscount {
    
    public MaxDiscount(BaseDiscount discount1, BaseDiscount discount2) {
        super(discount1, discount2);
    }


    @Override
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        double maxDiscount = 0;
        for (Discount discount : getDiscounts().values()) {
            maxDiscount=Math.max(maxDiscount,discount.calculateDiscount(allItems));
        }
        return maxDiscount;
    }

}
