package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;

public abstract class CompositeDiscount extends Discount {
    private HashMap<Integer,Discount> discounts;
    
    public CompositeDiscount() {
        this.discounts = new HashMap<>();
    }
    public CompositeDiscount(Discount discount1, Discount discount2) {
        this.discounts = new HashMap<>();
        discounts.put(discount1.getDiscountId(), discount1);
        discounts.put(discount2.getDiscountId(), discount2);
        
    }
    public HashMap<Integer, Discount> getDiscounts() {
        return discounts;
    }
    public List<Discount> getDiscountsList() {
        return new ArrayList<>(discounts.values());
    }
    public void addDiscount(Discount discount) {
        if(discounts.containsKey(discount.getDiscountId())){
            throw new IllegalArgumentException("Discount already exists");
        }
        discounts.put(discount.getDiscountId(), discount);
    }
    public void removeDiscount(int discountId) {
        if(discounts.containsKey(discountId)){
            discounts.remove(discountId);
        }
        else{
            throw new IllegalArgumentException("Discount not found");
        }
    }

    public List<Integer> getIds() {
        return new ArrayList<>(discounts.keySet());
    }

}
