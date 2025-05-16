package Tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.User.ShoppingBasket;
import com.halilovindustries.backend.Domain.User.ShoppingCart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CartTest {
    
    private ShoppingCart cart;
    private ShoppingBasket basket1;
    private ShoppingBasket basket2;
    private ItemDTO item1, item2, item3, item4, item5, item6;
    
    @BeforeEach
    public void setUp() {
        // Create ItemDTO objects
        item1 = new ItemDTO("Item 1", Category.BEAUTY, 10.0, -1, 101, 5, 4.5, "Description 1",1);
        item2 = new ItemDTO("Item 2", Category.BEAUTY, 10.0, -1, 102, 5, 4.5, "Description 2",1);
        item3 = new ItemDTO("Item 3", Category.BEAUTY, 10.0, -1, 103, 5, 4.5,   "Description 3",1);
        item4 = new ItemDTO("Item 4", Category.BEAUTY, 10.0, -2, 201, 5, 4.5,   "Description 4",1);
        item5 = new ItemDTO("Item 5", Category.BEAUTY, 10.0, -2, 202, 5, 4.5,  "Description 5",1);
        item6 = new ItemDTO("Item 6", Category.BEAUTY, 10.0, -2, 203, 5, 4.5, "Description 6",1);
        
        // Create ShoppingBasket objects with shopIDs
        // and initialize them with empty item lists
        basket1 = new ShoppingBasket(-1);
        basket1.addItem(item1);
        basket1.addItem(item2);
        basket1.addItem(item3);
        basket2 = new ShoppingBasket(-2);
        basket2.addItem(item4);
        basket2.addItem(item5);
        basket2.addItem(item6);

        // Create a ShoppingCart object with a cartID and a list of baskets
        cart = new ShoppingCart(Arrays.asList(basket1, basket2), -1);
    }
    

    @Test
    public void testAddItemsSuccess() {
        basket1 = new ShoppingBasket(-1);
        basket2 = new ShoppingBasket(-2);
        cart = new ShoppingCart(Arrays.asList(basket1, basket2), -1);

        List<ItemDTO> itemsToAdd = Arrays.asList(item1, item2, item3, item4, item5, item6);
        
        assertTrue(cart.addItems(itemsToAdd), "Should return true when adding items to the cart successfully");
        
        // Verify items in each basket after adding
        assertTrue(basket1.getItems().containsAll(Arrays.asList(item1, item2, item3)), "Basket 1 should contain items 1, 2, and 3");
        assertTrue(basket2.getItems().containsAll(Arrays.asList(item4, item5, item6)), "Basket 2 should contain items 4, 5, and 6");
    }

    @Test
    public void testGetCartID() {
        assertEquals(-1, cart.getCartID(), "Cart ID should be -1");
    }
    
    @Test
    public void testGetItems() {
        List<ItemDTO> expectedItems = new ArrayList<>();
        expectedItems.add(item1);
        expectedItems.add(item2);
        expectedItems.add(item3);
        expectedItems.add(item4);
        expectedItems.add(item5);
        expectedItems.add(item6);


        
        assertEquals(expectedItems, cart.getItems(), "Items in cart should match expected mapping of ItemDTO objects to shop IDs");
    }
    
    @Test
public void testDeleteItemsSuccess() {
    HashMap<Integer, List<Integer>> itemsToDelete = new HashMap<>();
    // Create list of item IDs for shop -1
    List<Integer> shop1Items = new ArrayList<>();
    shop1Items.add(item1.getItemID());
    shop1Items.add(item2.getItemID());
    
    // Add the list to the map with shop ID as key
    itemsToDelete.put(-1, shop1Items);
    
    assertTrue(cart.deleteItems(itemsToDelete), "Should return true when successfully deleting items");
    
    // Verify items after deletion
    List<ItemDTO> expectedRemainingItems = new ArrayList<>();
    expectedRemainingItems.add(item3);
    expectedRemainingItems.add(item4);
    expectedRemainingItems.add(item5);
    expectedRemainingItems.add(item6);
    
    assertEquals(expectedRemainingItems, cart.getItems(), "Cart should contain only remaining items after deletion");
}

    @Test
    public void testDeleteItemsNotInCart() {
        // Create a test for the exception scenario
        HashMap<Integer, List<Integer>> itemsToDelete = new HashMap<>();
        List<Integer> shop1Items = new ArrayList<>();
        shop1Items.add(999); // Item ID that doesn't exist
        itemsToDelete.put(-1, shop1Items);
        
        assertThrows(IllegalArgumentException.class, () -> {
            cart.deleteItems(itemsToDelete);
        }, "Should throw IllegalArgumentException when trying to delete items not in cart");
    }

    @Test
    public void testDeleteNonExistentShop() {
        // Create a test for non-existent shop
        HashMap<Integer, List<Integer>> itemsToDelete = new HashMap<>();
        List<Integer> nonExistentShopItems = new ArrayList<>();
        nonExistentShopItems.add(item1.getItemID());
        itemsToDelete.put(999, nonExistentShopItems); // Shop ID that doesn't exist
        
        assertThrows(IllegalArgumentException.class, () -> {
            cart.deleteItems(itemsToDelete);
        }, "Should throw IllegalArgumentException when trying to delete items from non-existent shop");
    }
}

