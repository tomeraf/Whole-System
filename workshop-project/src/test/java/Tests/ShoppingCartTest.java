package Tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.User.ShoppingBasket;
import com.halilovindustries.backend.Domain.User.ShoppingCart;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.List;



class ShoppingCartTest {

    private ShoppingCart cart;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart(1);
    }

    @Test
    void testAddItem_NewBasketCreated() {
        cart.addItem(10, 100, 2);
        assertEquals(1, cart.getBaskets().size());
        assertTrue(cart.getItems().containsKey(10));
        assertEquals(2, cart.getItems().get(10).get(100));
    }

    @Test
    void testAddItem_ExistingBasket() {
        cart.addItem(10, 100, 2);
        cart.addItem(10, 101, 3);
        assertEquals(1, cart.getBaskets().size());
        assertEquals(2, cart.getItems().get(10).size());
        assertEquals(3, cart.getItems().get(10).get(101));
    }

    @Test
    void testGetCartID() {
        assertEquals(1, cart.getCartID());
    }

    @Test
    void testDeleteItem_ItemExists() {
        cart.addItem(10, 100, 2);
        boolean removed = cart.deleteItem(10, 100);
        assertTrue(removed);
        assertTrue(cart.getItems().get(10).isEmpty());
    }

    @Test
    void testDeleteItem_ItemDoesNotExist() {
        cart.addItem(10, 100, 2);
        boolean removed = cart.deleteItem(10, 101);
        assertFalse(removed);
    }

    @Test
    void testDeleteItem_BasketDoesNotExist() {
        boolean removed = cart.deleteItem(99, 100);
        assertFalse(removed);
    }

    @Test
    void testClearCart() {
        cart.addItem(10, 100, 2);
        cart.addItem(11, 200, 1);
        cart.clearCart();
        assertTrue(cart.getItems().get(10).isEmpty());
        assertTrue(cart.getItems().get(11).isEmpty());
    }

    @Test
    void testGetBaskets() {
        cart.addItem(10, 100, 2);
        cart.addItem(11, 200, 1);
        List<ShoppingBasket> baskets = cart.getBaskets();
        assertEquals(2, baskets.size());
        assertTrue(baskets.stream().anyMatch(b -> b.getShopID() == 10));
        assertTrue(baskets.stream().anyMatch(b -> b.getShopID() == 11));
    }

    @Test
    void testGetShopIDs() {
        cart.addItem(10, 100, 2);
        cart.addItem(11, 200, 1);
        List<Integer> shopIDs = cart.getShopIDs();
        assertTrue(shopIDs.contains(10));
        assertTrue(shopIDs.contains(11));
        assertEquals(2, shopIDs.size());
    }

    @Test
    void testGetItems_EmptyCart() {
        HashMap<Integer, HashMap<Integer, Integer>> items = cart.getItems();
        assertTrue(items.isEmpty());
    }

    @Test
    void testAddItem_MultipleQuantities() {
        cart.addItem(10, 100, 2);
        cart.addItem(10, 100, 3); // Should add to existing quantity
        assertEquals(5, cart.getItems().get(10).get(100));
    }
}