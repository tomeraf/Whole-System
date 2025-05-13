package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;


public class ConditionDTO {
    private ConditionType condition_type;
    private int itemId;
    private Category category;
    private ConditionLimits conditionLimits;
    private int price;
    private int quantity;
    private ConditionLimits conditionLimits2;
    private int itemId2;
    private int quantity2;
    private int price2;
    private Category category2;

    public ConditionDTO(ConditionType condition_type, int itemId, Category category, int price, int quantity, int itemId2, int quantity2, int price2, Category category2,ConditionLimits conditionLimits, ConditionLimits conditionLimits2) {
        this.condition_type = condition_type;
        this.itemId = itemId;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.itemId2 = itemId2;
        this.quantity2 = quantity2;
        this.price2 = price2;
        this.category2 = category2;
        this.conditionLimits = conditionLimits;
        this.conditionLimits2 = conditionLimits2;
    }
    public ConditionDTO(int itemId, Category category, int price, int quantity, ConditionLimits conditionLimits) {
        this.condition_type = ConditionType.BASE;
        this.itemId = itemId;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.conditionLimits = conditionLimits;
        this.itemId2 = -1;
        this.quantity2 = -1;
        this.price2 = -1;
        this.category2 = null;
        this.conditionLimits2 = null;
    }

    public ConditionType getConditionType() {
        return condition_type;
    }

    public int getItemId() {
        return itemId;
    }

    public Category getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getItemId2() {
        return itemId2;
    }

    public int getQuantity2() {
        return quantity2;
    }

    public int getPrice2() {
        return price2;
    }

    public Category getCategory2() {
        return category2;
    }
    public ConditionLimits getConditionLimits() {
        return conditionLimits;
    }
    public ConditionLimits getConditionLimits2() {
        return conditionLimits2;
    }
    


}
