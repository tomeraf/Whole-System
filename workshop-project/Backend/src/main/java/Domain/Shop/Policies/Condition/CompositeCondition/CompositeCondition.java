package Domain.Shop.Policies.Condition.CompositeCondition;

import Domain.Shop.Policies.Condition.Condition;

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


}
