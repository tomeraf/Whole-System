package com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
@Entity
@DiscriminatorValue("AND")
public class AndCondition extends CompositeCondition {
    // Default constructor for JPA
    public AndCondition() {
    }
    public AndCondition(Condition condition1, Condition condition2) {
        super(condition1, condition2);
    }

    @Override
    public boolean checkCondition(HashMap<Item, Integer> allItems) {
        return getCondition1().checkCondition(allItems) && getCondition2().checkCondition(allItems);
    }

    @Override
    public String toString() {
        // exactly “<left> AND <right>”
        return getCondition1().toString() + " AND " + getCondition2().toString();
    }
    
    @Override
    public ConditionType getConditionType() {
        return ConditionType.AND;
    }
}
