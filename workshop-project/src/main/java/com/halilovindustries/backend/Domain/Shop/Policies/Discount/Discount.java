package com.halilovindustries.backend.Domain.Shop.Policies.Discount;

import java.util.HashMap;

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


}
