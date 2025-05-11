package com.halilovindustries.backend.Domain.Rules;

import java.util.function.Predicate;

import com.halilovindustries.backend.Domain.Guest;
import com.halilovindustries.backend.Domain.Item;

public class PredicateRule implements Rule {
    private final Rule rule;
    private final int shopID;

    private PredicateRule(Rule rule, int shopID) {
        this.rule = rule;
        this.shopID = shopID;
    }

    @Override
    public boolean evaluate(Guest guest, Item item, int quantity) {
        return rule.evaluate(guest, item, quantity);
    }

    @Override
    public int getShopID() {
        return shopID;
    }

    // Factory for rules that only depend on the Guest
    public static PredicateRule fromUser(Predicate<Guest> userPredicate, int shopID) {
        return new PredicateRule((guest, ignored1, ignored2) -> userPredicate.test(guest), shopID);
    }

    // Factory for rules that only depend on the Item
    public static PredicateRule fromItem(Predicate<Item> itemPredicate, int shopID) {
        return new PredicateRule((ignored1, item, ignored2) -> itemPredicate.test(item), shopID);
    }

    // Factory for rules that only depend on Quantity
    public static PredicateRule fromQuantity(java.util.function.IntPredicate quantityPredicate, int shopID) {
        return new PredicateRule((ignored1, ignored2, quantity) -> quantityPredicate.test(quantity), shopID);
    }

    // Full custom rule
    public static PredicateRule from(Rule rule, int shopID) {
        return new PredicateRule(rule, shopID);
    }
}

