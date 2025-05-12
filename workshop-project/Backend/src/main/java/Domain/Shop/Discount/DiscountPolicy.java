package Domain.Shop.Discount;

import java.util.ArrayList;
import java.util.List;

import Domain.Rules.Rule;

public class DiscountPolicy {
    private List<DiscountType> discountTypes;
    private List<Rule> discountRules;
    
    public DiscountPolicy(){
        this.discountTypes = new ArrayList<>();
        this.discountRules = new ArrayList<>();
    }
    public void addDiscountType(DiscountType discountType) {
        this.discountTypes.add(discountType);
    }
    public void adddiscountRule(Rule discountRule) {
        this.discountRules.add(discountRule);
    }
    public void removeDiscountType(DiscountType discountType) {
        this.discountTypes.remove(discountType);
    }
    public void removediscountRule(Rule discountRule) {
        this.discountRules.remove(discountRule);
    }

}
