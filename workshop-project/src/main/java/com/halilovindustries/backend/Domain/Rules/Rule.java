package com.halilovindustries.backend.Domain.Rules;

import com.halilovindustries.backend.Domain.Guest;
import com.halilovindustries.backend.Domain.Item;

@FunctionalInterface
public interface Rule {
    boolean evaluate(Guest user, Item item, int quantity);

    // Add shopID getter
    default int getShopID() {
        return -1; // Default implementation (can be overridden)
    }
}

