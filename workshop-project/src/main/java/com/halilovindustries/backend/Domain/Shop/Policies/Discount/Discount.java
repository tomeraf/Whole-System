package com.halilovindustries.backend.Domain.Shop.Policies.Discount;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int discountId;
    public Discount() {//Default constructor for JPA
        
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
