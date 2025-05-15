package com.halilovindustries.backend.Domain.Shop.Policies.Condition;

import java.util.HashMap;
import com.halilovindustries.backend.Domain.Shop.Item;

public abstract class Condition {
    private static int idCounter = 0;
    private int id;
    public Condition() {
        this.id = idCounter++;
    }
    public int getId() {
        return id;
    }
    public abstract boolean checkCondition(HashMap<Item, Integer> allItems);
}
