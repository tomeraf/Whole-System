package Domain.Shop.Policies.Condition.CompositeCondition;

import java.util.HashMap;

import Domain.Shop.Item;
import Domain.Shop.Policies.Condition.Condition;
public class AndCondition extends CompositeCondition {
    public AndCondition(Condition condition1, Condition condition2) {
        super(condition1, condition2);
    }

    @Override
    public boolean checkCondition(HashMap<Item, Integer> allItems) {
        return getCondition1().checkCondition(allItems) && getCondition2().checkCondition(allItems);
    }

    public String toString() {
        return String.format("%s AND %s", getCondition1().toString(), getCondition2().toString());
    }
}
