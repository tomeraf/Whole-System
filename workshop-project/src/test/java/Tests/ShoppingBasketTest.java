package Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.User.ShoppingBasket;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class ShoppingBasketTest {

    private ShoppingBasket basket;

    @BeforeEach
    void setUp() {
        basket = new ShoppingBasket(42);
    }

    @Test
    void testConstructorAndGetShopID() {
        assertEquals(42, basket.getShopID());
    }

    @Test
    void testAddItem_NewItem() {
        basket.addItem(1, 3);
        Map<Integer, Integer> items = basket.getItems();
        assertEquals(1, items.size());
        assertEquals(3, items.get(1));
    }

    @Test
    void testAddItem_ExistingItemIncreasesQuantity() {
        basket.addItem(1, 2);
        basket.addItem(1, 5);
        assertEquals(7, basket.getItems().get(1));
    }

    @Test
    void testAddItem_ZeroQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> basket.addItem(2, 0));
    }

    @Test
    void testAddItem_NegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> basket.addItem(2, -1));
    }

    @Test
    void testRemoveItem_ItemExists() {
        basket.addItem(1, 2);
        boolean removed = basket.removeItem(1);
        assertTrue(removed);
        assertFalse(basket.getItems().containsKey(1));
    }

    @Test
    void testRemoveItem_ItemDoesNotExist() {
        boolean removed = basket.removeItem(99);
        assertFalse(removed);
    }

    @Test
    void testClearBasket() {
        basket.addItem(1, 2);
        basket.addItem(2, 3);
        basket.clearBasket();
        assertTrue(basket.getItems().isEmpty());
    }
}