package Domain.Shop.Discount.BaseDiscount;

import java.util.HashMap;

import Domain.Shop.Discount.Discount;
import Domain.DTOs.ConditionDTO;
import Domain.Shop.*;

import Domain.Shop.Condition.*;

public class ConditionalDiscount extends BaseDiscount {
    private Condition condition;
    
    public ConditionalDiscount(int id,Condition condition,int percentage,int itemID,Category category) {
        super(id,percentage, category,itemID);
        this.condition = condition;
    }
    public ConditionalDiscount(int id,Condition condition,int percentage,int itemID) {
        super(id,percentage, itemID);
        this.condition = condition;
        
    }
    public ConditionalDiscount(int id,Condition condition, int percentage,Category category) {
        super(id,percentage, category);
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

}
