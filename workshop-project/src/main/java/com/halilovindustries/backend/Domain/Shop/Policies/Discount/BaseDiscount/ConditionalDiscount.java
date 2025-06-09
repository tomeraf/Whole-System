package com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("CONDITIONAL")
public class ConditionalDiscount extends BaseDiscount {
    @OneToOne(cascade = CascadeType.ALL)
    private Condition condition;
    public ConditionalDiscount() {// Default constructor for JPA
    }
    
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
    @Override
    public HashMap<Item, Double> getPercentagePerItem(HashMap<Item, Integer> allItems) {
        if (condition.checkCondition(allItems)) {
            return super.getPercentagePerItem(allItems);
        }
        return new HashMap<>();
    }
    @Override
    public DiscountKind getDiscountKind() {
        return DiscountKind.CONDITIONAL;
    }
    @Override
    public DiscountType getDiscountType() {
        return DiscountType.CONDITIONAL;
    }
    @Override
    public ConditionDTO getCondition() {
        return new ConditionDTO(condition.getConditionType(),condition.getItemId(),condition.getCategory(),condition.getConditionLimits(),condition.getMinPrice(),condition.getMaxPrice(),condition.getMinQuantity(),condition.getMaxQuantity(),condition.getConditionLimits2(),condition.getItemId2(),condition.getMinPrice2(),condition.getMaxPrice2(),condition.getMinQuantity2(),condition.getMaxQuantity2(),condition.getCategory2());
    }

}
