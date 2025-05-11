package com.halilovindustries.backend.Domain.Discount;

import java.time.LocalDate;

import com.halilovindustries.backend.Domain.Guest;
import com.halilovindustries.backend.Domain.Item;
import com.halilovindustries.backend.Domain.Rules.Rule;

public class conditionalDiscount extends OpenDiscount {
    private Rule rule; // The rule that must be satisfied for the discount to apply

    public conditionalDiscount(int id, int percentage, String startDate, String endDate, Rule rule) {
        super(id, percentage, startDate, endDate);
        this.rule = rule;
    }
    public int applyDiscount(int price,Guest user,Item item,int quantity) {
        if (inTime(LocalDate.now()) && isApplicable(item.getId()) && rule.evaluate(user,item,quantity)) { // Check if the rule is satisfied
            return price - (price * getPercentage() / 100);
        }
        return price; // No discount applied
    }

}
