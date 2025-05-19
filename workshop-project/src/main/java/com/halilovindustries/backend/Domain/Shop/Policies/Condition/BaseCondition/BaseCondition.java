package com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Category;



public abstract class BaseCondition extends Condition {
    private int itemId;
    private Category category;
    
    public BaseCondition(int itemId, Category category) {
        if(itemId != -1 && category != null){
            throw new IllegalArgumentException("Cannot have both itemId and category");
        }
        this.itemId = itemId;
        this.category = category;
    }
    public BaseCondition(int itemId) {
        this.itemId = itemId;
        this.category = null;
    }
    public BaseCondition(Category category) {
        this.itemId = -1;
        this.category = category;
    }
    public BaseCondition() {
        this.itemId = -1;
        this.category = null;
    }
    @Override
    public ConditionType getConditionType(){
        return ConditionType.BASE;
    }

    public int getItemId() {
        return itemId;
    }
    @Override
    public Category getCategory() {
        return category;
    }
    public String getType(){
        if (itemId != -1) {
            return "Item";
        } else if (category != null) {
            return "Category";
        } else {
            return "Shop";
        }
    }
    public boolean checkCondition(HashMap<Item, Integer> allItems){
        switch (getType()) {
            case "Item":
                return checkItemCondition(allItems);
            case "Category":
                return checkCategoryCondition(allItems);
            case "Shop":
                return checkShopCondition(allItems);
            default:
                return false;
        }
    }
    public abstract boolean checkItemCondition(HashMap<Item, Integer> allItems);
    public abstract boolean checkCategoryCondition(HashMap<Item, Integer> allItems);
    public abstract boolean checkShopCondition(HashMap<Item, Integer> allItems);

    public String toString() {
        switch (getType()) {
            case "Item":
                return "Item Condition: " + itemId+" ";
            case "Category":
                return "Category Condition: " + category.name()+" ";
            case "Shop":
                return "Shop Condition ";
            default:
                return "Unknown Condition";
        }
        
    }

}
