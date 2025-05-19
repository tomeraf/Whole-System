package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Category;

public abstract class CompositeDiscount extends Discount {
    private HashMap<Integer,Discount> discounts;
    private int firstId;
    private int secondId;
    
    public CompositeDiscount() {
        this.discounts = new HashMap<>();
    }
    public CompositeDiscount(Discount discount1, Discount discount2) {
        this.discounts = new HashMap<>();
        discounts.put(discount1.getDiscountId(), discount1);
        discounts.put(discount2.getDiscountId(), discount2);
        this.firstId = discount1.getDiscountId();
        this.secondId = discount2.getDiscountId();
        
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
    @Override
    public DiscountType getDiscountType() { return discounts.get(firstId).getDiscountType(); }
    @Override
    public int getItemId() { return discounts.get(firstId).getItemId(); }
    @Override
    public Category getCategory() { return discounts.get(firstId).getCategory(); }
    @Override
    public int getPercentage() { return discounts.get(firstId).getPercentage(); }
    @Override
    public ConditionDTO getCondition() { return discounts.get(firstId).getCondition(); }
    @Override
    public DiscountType getDiscountType2() { return discounts.get(secondId).getDiscountType(); }
    @Override
    public int getItemId2() { return discounts.get(secondId).getItemId(); }
    @Override
    public Category getCategory2() { return discounts.get(secondId).getCategory(); }
    @Override
    public int getPercentage2() { return discounts.get(secondId).getPercentage(); }
    @Override
    public ConditionDTO getCondition2() { return discounts.get(secondId).getCondition(); }
}
