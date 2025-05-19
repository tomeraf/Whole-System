package com.halilovindustries.backend.Domain.Shop.Policies.Condition;

import java.util.HashMap;
import com.halilovindustries.backend.Domain.Shop.*;

public abstract class Condition {
    private static int idCounter = 0;
    private int id;
    public Condition() {
        this.id = idCounter++;
    }
    public int getId() {
        return id;
    }
    public abstract boolean checkCondition(HashMap<Item, Integer> allItems);
    public abstract ConditionType getConditionType();
    
    public int getItemId() { return -1; }
    public Category getCategory() { return null; }
    public ConditionLimits getConditionLimits() { return null; }
    public int getMinPrice() { return -1; }
    public int getMaxPrice() { return -1; }
    public int getMinQuantity() { return -1; }
    public int getMaxQuantity() { return -1; }
    public ConditionLimits getConditionLimits2() { return null; }
    public int getItemId2() { return -1; }
    public int getMinPrice2() { return -1; }
    public int getMaxPrice2() { return -1; }
    public int getMinQuantity2() { return -1; }
    public int getMaxQuantity2() { return -1; }
    public Category getCategory2() { return null; }
}
