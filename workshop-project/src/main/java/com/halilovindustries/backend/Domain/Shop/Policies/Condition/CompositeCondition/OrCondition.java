package com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OR")
public class OrCondition extends CompositeCondition {
    // Default constructor for JPA
    public OrCondition() {
    }
    public OrCondition(Condition condition1, Condition condition2) {
        super(condition1, condition2);
    }

    @Override
    public boolean checkCondition(HashMap<Item, Integer> allItems) {
        return getCondition1().checkCondition(allItems) || getCondition2().checkCondition(allItems);
    }

    public String toString() {
        return String.format("%s OR %s", getCondition1().toString(), getCondition2().toString());
    }
    @Override
    public ConditionType getConditionType() {
        return ConditionType.OR;
    }

}
