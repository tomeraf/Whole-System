package com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;

public abstract class CompositeCondition extends Condition {
    private Condition condition1;
    private Condition condition2;

    public CompositeCondition(Condition condition1, Condition condition2) {
        super();
        this.condition1 = condition1;
        this.condition2 = condition2;
    }

    public Condition getCondition1() {
        return condition1;
    }

    public Condition getCondition2() {
        return condition2;
    }
    @Override
    public int getItemId() { return condition1.getItemId(); }
    @Override
    public int getItemId2() { return condition2.getItemId(); }
    @Override
    public Category getCategory() { return condition1.getCategory(); }
    @Override
    public Category getCategory2() { return condition2.getCategory(); }
    @Override
    public ConditionLimits getConditionLimits() { return condition1.getConditionLimits(); }
    @Override
    public ConditionLimits getConditionLimits2() { return condition2.getConditionLimits(); }
    @Override
    public int getMinPrice() { return condition1.getMinPrice(); }
    @Override
    public int getMaxPrice() { return condition1.getMaxPrice(); }
    @Override
    public int getMinQuantity() { return condition1.getMinQuantity(); }
    @Override
    public int getMaxQuantity() { return condition1.getMaxQuantity(); }
    @Override
    public int getMinPrice2() { return condition2.getMinPrice(); }
    @Override
    public int getMaxPrice2() { return condition2.getMaxPrice(); }
    @Override
    public int getMinQuantity2() { return condition2.getMinQuantity(); }
    @Override
    public int getMaxQuantity2() { return condition2.getMaxQuantity(); }
    


}
