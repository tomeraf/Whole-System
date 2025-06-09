package Tests;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.PriceCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.QuantityCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition.AndCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition.OrCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition.XorCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.*;


class ConditionTests {

    // Helper to build a basket with one item of given price and quantity
    private HashMap<Item, Integer> basket(double price, int qty) {
        Item i = new Item("natania", Category.AUTOMOTIVE, price, 42, 0, "Test Item");
        HashMap<Item, Integer> items = new HashMap<>();
        items.put(i, qty);
        return items;
    }

    @Test
    void testPriceCondition_CheckShopCondition() {
        // allow prices between 50 and 100
        PriceCondition c = new PriceCondition(50, 100);
        assertTrue(c.checkShopCondition(basket(60,1)), "Should allow when total price >= 50 and <=100");
        assertFalse(c.checkShopCondition(basket(40,1)), "Should block when total price < 50");
    }

    @Test
    void testQuantityCondition_CheckShopCondition() {
        // allow up to 5 items
        QuantityCondition c = new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 5);
        assertTrue(c.checkShopCondition(basket(10,3)), "Should allow when quantity <=5");
        assertFalse(c.checkShopCondition(basket(10,6)), "Should block when quantity >5");
    }

    @Test
    void andCondition_FailsWhenOneSubConditionFails() {
        AndCondition c = new AndCondition(
            new PriceCondition(100, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        assertFalse(c.checkCondition(basket(150, 3)), "Should fail when quantity sub-condition fails");
    }

    @Test
    void orCondition_AllowsWhenOneSubConditionMet() {
        OrCondition c = new OrCondition(
            new PriceCondition(100, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 3)
        );
        assertTrue(c.checkCondition(basket(150, 1)), "Should allow when price and quantity sub-conditions met");
        assertTrue(c.checkCondition(basket(50, 3)),  "Should allow when quantity sub-condition met");
        assertTrue(c.checkCondition(basket(150, 4)), "Should block when neither sub-condition met");
        assertFalse(c.checkCondition(basket(10, 4)), "Should block when neither sub-condition met");
    }

    @Test
    void xorCondition_TrueWhenExactlyOne() {
        XorCondition c = new XorCondition(
            new PriceCondition(200, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 1)
        );
        assertTrue(c.checkCondition(basket(150, 1)),  "Should allow when exactly one sub-condition is met");
        assertTrue(c.checkCondition(basket(250, 2)),  "Should allow when exactly one sub-condition is met");
        assertFalse(c.checkCondition(basket(200, 1)), "Should block when both sub-conditions are met");
        assertFalse(c.checkCondition(basket(50, 2)),  "Should block when none sub-conditions are met");
    }
    // --- Additional AndCondition Tests ---
    @Test
    void testAndCondition_AllTrue_HappyAndInvariant() {
        AndCondition c = new AndCondition(
            new PriceCondition(10, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        HashMap<Item,Integer> basket = basket(20, 2);
        // Happy path: both sub-conditions true
        assertTrue(c.checkCondition(basket), "AND should allow when both sub-conditions are met");
        // Invariant: basket unchanged
        assertEquals(1, basket.size(), "Basket should remain unchanged after check");
        assertTrue(basket.containsValue(2), "Original quantity should remain");
    }

    @Test
    void testAndCondition_OneFalse_SadAndInvariant() {
        AndCondition c = new AndCondition(
            new PriceCondition(50, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        HashMap<Item,Integer> basket = basket(20, 3);
        // Sad path: quantity sub-condition fails
        assertFalse(c.checkCondition(basket), "AND should block when one sub-condition fails");
        // Invariant: basket unchanged
        assertEquals(1, basket.size(), "Basket should remain unchanged after failed check");
        assertTrue(basket.containsValue(3), "Original quantity should remain");
    }

    // --- Additional OrCondition Tests ---
    @Test
    void testOrCondition_NoTrue_SadAndInvariant() {
        OrCondition c = new OrCondition(
            new PriceCondition(50, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        HashMap<Item,Integer> basket = basket(10, 3);
        // Sad path: neither sub-condition true
        assertFalse(c.checkCondition(basket), "OR should block when no sub-condition is met");
        // Invariant
        assertEquals(1, basket.size());
    }

    @Test
    void testOrCondition_OneTrue_HappyAndInvariant() {
        OrCondition c = new OrCondition(
            new PriceCondition(50, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        HashMap<Item,Integer> basketPrice = basket(100, 3);
        assertTrue(c.checkCondition(basketPrice), "OR should allow when price sub-condition is met");
        assertEquals(1, basketPrice.size(), "Basket unchanged after check");
    }

    // --- Additional XorCondition Tests ---
    @Test
    void testXorCondition_BothTrue_SadAndInvariant() {
        XorCondition c = new XorCondition(
            new PriceCondition(10, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 5)
        );
        HashMap<Item,Integer> basketBoth = basket(20, 3);
        assertFalse(c.checkCondition(basketBoth), "XOR should block when both sub-conditions are met");
        assertEquals(1, basketBoth.size());
    }

    @Test
    void testXorCondition_NoneTrue_SadAndInvariant() {
        XorCondition c = new XorCondition(
            new PriceCondition(100, Integer.MAX_VALUE),
            new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 2)
        );
        HashMap<Item,Integer> basketNone = basket(10, 5);
        assertFalse(c.checkCondition(basketNone), "XOR should block when neither sub-condition is met");
        assertEquals(1, basketNone.size());
    }
}