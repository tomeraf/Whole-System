package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.BaseDiscount;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MAX")
public class MaxDiscount extends CompositeDiscount {
    
    public MaxDiscount() {// Default constructor for JPA
        super();
    }
    public MaxDiscount(BaseDiscount discount1, BaseDiscount discount2) {
        super(discount1, discount2);
    }


    @Override
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        double maxDiscount = 0;
        for (Discount discount : getDiscounts()) {
            maxDiscount=Math.max(maxDiscount,discount.calculateDiscount(allItems));
        }
        return maxDiscount;
    }
    @Override
    public HashMap<Item, Double> getPercentagePerItem(HashMap<Item, Integer> allItems) {
        Discount maxDiscount = null;
        for(Discount discount : getDiscounts()) {
            if (maxDiscount == null || discount.calculateDiscount(allItems) > maxDiscount.calculateDiscount(allItems)) {
                maxDiscount = discount;
            }
        }
        return maxDiscount.getPercentagePerItem(allItems);
    }

    @Override
    public DiscountKind getDiscountKind() {
        return DiscountKind.MAX;
    }
}
