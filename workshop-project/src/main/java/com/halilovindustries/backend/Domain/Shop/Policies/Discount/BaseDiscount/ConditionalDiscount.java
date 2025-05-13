package com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;

public class ConditionalDiscount extends BaseDiscount {
    private Condition condition;
    
    public ConditionalDiscount(Condition condition,int percentage,int itemID,Category category) {
        super(percentage, category,itemID);
        this.condition = condition;
    }
    public ConditionalDiscount(int id,Condition condition,int percentage,int itemID) {
        super(percentage, itemID);
        this.condition = condition;
        
    }
    public ConditionalDiscount(int id,Condition condition, int percentage,Category category) {
        super(percentage, category);
        this.condition = condition;
    }
    public ConditionalDiscount(int id,Condition condition, int percentage) {
        super(id,percentage);
        this.condition = condition;
    }
    @Override
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        if (condition.checkCondition(allItems)) {
            return super.calculateDiscount(allItems);
        }
        return 0;
    }

}
