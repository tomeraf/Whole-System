package com.halilovindustries.backend.Domain.Shop.Policies.Discount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.*;

public abstract class Discount {
    private static int idCounter = 0;
    private int discountId;
    public Discount() {
        this.discountId = idCounter++;
    }
    public int getDiscountId() {
        return discountId;
    }
    public abstract double calculateDiscount(HashMap<Item,Integer> allItems);
    public abstract HashMap<Item,Double> getPercentagePerItem(HashMap<Item,Integer> allItems);
    
    public DiscountKind getDiscountKind() { return null; }
    public DiscountType getDiscountType() { return null; }
    public int getItemId() { return -1; }
    public Category getCategory() { return null; }
    public int getPercentage() { return -1; }
    public ConditionDTO getCondition() { return null; }
    public DiscountType getDiscountType2() { return null; }
    public int getItemId2() { return -1; }
    public Category getCategory2() { return null; }
    public int getPercentage2() { return -1; }
    public ConditionDTO getCondition2() { return null; }
}
