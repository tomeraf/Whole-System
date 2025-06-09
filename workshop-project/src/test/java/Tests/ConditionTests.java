package Tests;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.lang.IllegalArgumentException;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
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
        Item i = new Item(0, "natania", Category.AUTOMOTIVE, price, 42, "Test Item");
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

    @Test
    void compositeCondition_Getters_DelegatesToSubconditions() {
        // Setup two base conditions with distinct limits
        PriceCondition p = new PriceCondition(10, 20);
        QuantityCondition q = new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 5);
        AndCondition c = new AndCondition(p, q);

        // Subcondition references
        assertSame(p, c.getCondition1(), "getCondition1 should return the first condition instance");
        assertSame(q, c.getCondition2(), "getCondition2 should return the second condition instance");

        // ID delegation
        assertEquals(p.getItemId(),   c.getItemId(),   "getItemId should delegate to first condition");
        assertEquals(q.getItemId(),   c.getItemId2(),  "getItemId2 should delegate to second condition");

        // Category delegation
        assertEquals(p.getCategory(), c.getCategory(),  "getCategory should delegate to first condition");
        assertEquals(q.getCategory(), c.getCategory2(), "getCategory2 should delegate to second condition");

        // Limits delegation
        assertEquals(p.getConditionLimits(),  c.getConditionLimits(),  "getConditionLimits should delegate to first condition");
        assertEquals(q.getConditionLimits(),  c.getConditionLimits2(), "getConditionLimits2 should delegate to second condition");

        // Price range delegation
        assertEquals(p.getMinPrice(),   c.getMinPrice(),   "getMinPrice should delegate to first condition");
        assertEquals(p.getMaxPrice(),   c.getMaxPrice(),   "getMaxPrice should delegate to first condition");
        assertEquals(q.getMinPrice(),   c.getMinPrice2(),  "getMinPrice2 should delegate to second condition");
        assertEquals(q.getMaxPrice(),   c.getMaxPrice2(),  "getMaxPrice2 should delegate to second condition");

        // Quantity range delegation for first (PriceCondition has default 0/0)
        assertEquals(p.getMinQuantity(), c.getMinQuantity(), "getMinQuantity should delegate to first condition");
        assertEquals(p.getMaxQuantity(), c.getMaxQuantity(), "getMaxQuantity should delegate to first condition");

        // Quantity range delegation for second
        assertEquals(q.getMinQuantity(),  c.getMinQuantity2(), "getMinQuantity2 should delegate to second condition");
        assertEquals(q.getMaxQuantity(),  c.getMaxQuantity2(), "getMaxQuantity2 should delegate to second condition");
    }

    @Test
    void testAndCondition_ToStringAndType() {
        PriceCondition p = new PriceCondition(5, 15);
        QuantityCondition q = new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 3);
        AndCondition c = new AndCondition(p, q);
        String expected = p.toString() + " AND " + q.toString();
        assertEquals(expected, c.toString(), "toString should concatenate sub-conditions with AND");
        assertEquals(ConditionType.AND, c.getConditionType(), "getConditionType should return AND");
    }

    @Test
    void testOrCondition_ToStringAndType() {
        PriceCondition p = new PriceCondition(5, 15);
        QuantityCondition q = new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 3);
        OrCondition c = new OrCondition(p, q);
        String expected = p.toString() + " OR " + q.toString();
        assertEquals(expected, c.toString(), "toString should concatenate sub-conditions with OR");
        assertEquals(ConditionType.OR, c.getConditionType(), "getConditionType should return OR");
    }

    @Test
    void testXorCondition_ToStringAndType() {
        PriceCondition p = new PriceCondition(5, 15);
        QuantityCondition q = new QuantityCondition(-1, Category.AUTOMOTIVE, -1, 3);
        XorCondition c = new XorCondition(p, q);
        String expected = p.toString() + " OR " + q.toString() + ",but not both";
        assertEquals(expected, c.toString(), "toString should describe XOR condition");
        assertEquals(ConditionType.XOR, c.getConditionType(), "getConditionType should return XOR");
    }

    // --- PriceCondition item- and category-level tests ---
    @Test
    void testPriceCondition_CheckItemCondition_ItemLevel() {
        // itemId = 7, allow total price between 30 and 100 for that item only
        PriceCondition c = new PriceCondition(7, null, 30, 100);
        Item matching = new Item(7, "X", Category.AUTOMOTIVE, 20, 0, "desc");
        Item other    = new Item(8, "Y", Category.AUTOMOTIVE, 50, 0, "desc");
        HashMap<Item,Integer> basket = new HashMap<>();
        basket.put(matching, 2); // total = 40
        basket.put(other,    3);
        assertTrue(c.checkItemCondition(basket), "Should allow when matching item total in range");
        basket.put(matching, 1); // total = 20
        assertFalse(c.checkItemCondition(basket), "Should block when matching item total out of range");
    }

    @Test
    void testPriceCondition_CheckCategoryCondition_CategoryLevel() {
        // category-level: only items in AUTOMOTIVE count
        PriceCondition c = new PriceCondition(-1, Category.AUTOMOTIVE, 30, 60);
        Item cat1 = new Item(1, "A", Category.AUTOMOTIVE, 10, 0, "d");
        Item cat2 = new Item(2, "B", Category.CLOTHING, 100, 0, "d");
        HashMap<Item,Integer> basket = new HashMap<>();
        basket.put(cat1, 3); // total automotive = 30
        basket.put(cat2, 1);
        assertTrue(c.checkCategoryCondition(basket), "Automotive subtotal in range");
        basket.put(cat1, 2); // subtotal 20
        assertFalse(c.checkCategoryCondition(basket), "Automotive subtotal below min should block");
    }

    @Test
    void testPriceCondition_InvalidConstructorRanges() {
        assertThrows(IllegalArgumentException.class, () -> new PriceCondition(-1, -1),
            "Both -1 should throw");
        assertThrows(IllegalArgumentException.class, () -> new PriceCondition(-5, 10),
            "Negative minPrice should throw");
        assertThrows(IllegalArgumentException.class, () -> new PriceCondition(5, -5),
            "Negative maxPrice should throw");
        assertThrows(IllegalArgumentException.class, () -> new PriceCondition(100, 50),
            "maxPrice < minPrice should throw");
    }

    // --- QuantityCondition item- and category-level tests ---
    @Test
    void testQuantityCondition_CheckItemCondition_ItemLevel() {
        // itemId = 5, allow quantity between 2 and 4 for that item only
        QuantityCondition c = new QuantityCondition(5, null, 2, 4);
        Item match = new Item(5, "M", Category.AUTOMOTIVE, 1, 0, "");
        Item other = new Item(6, "O", Category.AUTOMOTIVE, 1, 0, "");
        HashMap<Item,Integer> basket = new HashMap<>();
        basket.put(match, 3);
        basket.put(other, 1);
        assertTrue(c.checkItemCondition(basket), "Matching item quantity in range");
        basket.put(match, 1);
        assertFalse(c.checkItemCondition(basket), "Matching item quantity out of range");
    }

    @Test
    void testQuantityCondition_CheckCategoryCondition_CategoryLevel() {
        QuantityCondition c = new QuantityCondition(-1, Category.AUTOMOTIVE, 2, 5);
        Item cat = new Item(1, "C", Category.AUTOMOTIVE, 1, 0, "");
        Item other = new Item(2, "X", Category.CLOTHING, 1, 0, "");
        HashMap<Item,Integer> basket = new HashMap<>();
        basket.put(cat, 4);
        basket.put(other, 10);
        assertTrue(c.checkCategoryCondition(basket), "Automotive total quantity in range");
        basket.put(cat, 1);
        assertFalse(c.checkCategoryCondition(basket), "Automotive total quantity below min blocks");
    }

    @Test
    void testQuantityCondition_InvalidConstructorRanges() {
        assertThrows(IllegalArgumentException.class, () -> new QuantityCondition(-1, Category.AUTOMOTIVE, -1, -1),
            "Both -1 should throw");
        assertThrows(IllegalArgumentException.class, () -> new QuantityCondition(1, Category.AUTOMOTIVE, -2, 3),
            "Negative minQuantity should throw");
        assertThrows(IllegalArgumentException.class, () -> new QuantityCondition(1, Category.AUTOMOTIVE, 3, -2),
            "Negative maxQuantity should throw");
        assertThrows(IllegalArgumentException.class, () -> new QuantityCondition(1, Category.AUTOMOTIVE, 5, 2),
            "maxQuantity < minQuantity should throw");
    }

    @Test
    void testQuantityCondition_ToStringFormat() {
        QuantityCondition c = new QuantityCondition(-1, Category.AUTOMOTIVE, 2, 7);
        String s = c.toString();
        assertTrue(s.startsWith("QuantityCondition"), "Should start with class name");
        assertTrue(s.contains("minQuantity=2"), "Should include minQuantity");
        assertTrue(s.contains("maxQuantity=7"), "Should include maxQuantity");
    }
}