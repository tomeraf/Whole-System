package com.halilovindustries.backend.Domain.Shop.Policies.Condition;

import java.util.HashMap;

import com.github.javaparser.ast.Generated;
import com.halilovindustries.backend.Domain.Shop.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    public Condition() {// Default constructor for JPA
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
