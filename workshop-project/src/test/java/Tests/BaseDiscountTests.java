package Tests;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.BaseDiscount;
import com.halilovindustries.backend.Domain.Shop.Category;

public class BaseDiscountTests {

    // helper to build a simple item→quantity map
    private HashMap<Item, Integer> basket(int id, double price, int qty) {
        Item i = new Item(id, "natania", Category.AUTOMOTIVE, price, 42, "Test Item");
        HashMap<Item, Integer> items = new HashMap<>();
        items.put(i, qty);
        return items;
    }

    @Test
    void shopLevelDiscount_CalculatesCorrectly_andMetadata() {
        BaseDiscount d = new BaseDiscount(10);
        HashMap<Item,Integer> items = basket(1, 50.0, 2); // total 100
        // 10% of 100 = 10
        assertEquals(10.0, d.calculateDiscount(items), 1e-6);
        assertEquals("Shop", d.getType());
        assertEquals(DiscountKind.BASE, d.getDiscountKind());
        assertEquals(DiscountType.BASE, d.getDiscountType());
    }

    @Test
    void itemLevelDiscount_AppliesOnlyToMatchingItem() {
        BaseDiscount d = new BaseDiscount(25, 1);
        HashMap<Item,Integer> items = new HashMap<>();
        items.put(new Item(1, "X", Category.ELECTRONICS, 80.0, 42, "desc"), 1);
        items.put(new Item(2, "Y", Category.FURNITURE, 100.0, 42, "desc"), 2);
        // only item 1: 25% of 80 = 20
        assertEquals(20.0, d.calculateDiscount(items), 1e-6);

        // getPercentagePerItem: only entry for id=1 with multiplier 0.75
        HashMap<Item,Double> pct = d.getPercentagePerItem(items);
        assertEquals(1, pct.size());
        assertEquals(0.75, pct.get(items.keySet().stream().filter(i -> i.getId()==1).findFirst().get()), 1e-6);
        assertEquals("Item", d.getType());
    }

    // @Test
    // void categoryLevelDiscount_AppliesOnlyToThatCategory() {
    //     BaseDiscount d = new BaseDiscount(50, Category.AUTOMOTIVE);
    //     HashMap<Item,Integer> items = basket(1, 120.0, 1);
    //     assertEquals("Category", d.getType());
    //     assertEquals(60.0, d.calculateDiscount(items), 1e-6);
    // }

    @Test
    void getPercentagePerItem_AllItemsForShopLevel() {
        BaseDiscount d = new BaseDiscount(20);
        HashMap<Item,Integer> items = basket(1, 40.0, 3);
        HashMap<Item,Double> pct = d.getPercentagePerItem(items);
        // 20% discount => multiplier 0.8 for each
        for (double v : pct.values()) {
            assertEquals(0.8, v, 1e-6);
        }
    }

    @Test
    void invalidConstructor_bothItemAndCategory_Throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BaseDiscount(10, Category.BEAUTY, 1);
        });
    }

    @Test
    void itemLevelDiscount_NoMatchingItem_ReturnsZero() {
        // BaseDiscount for itemId=99, but cart contains only itemId=1
        BaseDiscount d = new BaseDiscount(30, 99);
        HashMap<Item,Integer> items = basket(1, 100.0, 2);
        assertEquals(0.0, d.calculateDiscount(items), 1e-6, 
                     "Should return zero when no matching item present");
        assertTrue(d.getPercentagePerItem(items).isEmpty(),
                   "getPercentagePerItem should be empty for non-matching item");
    }

    @Test
    void categoryLevelDiscount_NoMatchingCategory_ReturnsZero() {
        // BaseDiscount for Category.ELECTRONICS, but cart items are OTHER
        BaseDiscount d = new BaseDiscount(50, Category.ELECTRONICS);
        HashMap<Item,Integer> items = basket(1, 80.0, 1);
        assertEquals(0.0, d.calculateDiscount(items), 1e-6, 
                     "Should return zero when cart items do not match category");
        assertTrue(d.getPercentagePerItem(items).isEmpty(),
                   "getPercentagePerItem should be empty when category does not match");
    }
}