package Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Domain.Item;
import Domain.Category;

import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {
    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item("Test Item",Category.FOOD, 10.0, 5); // Create a new item with name, price, and quantity
    }

    @Test
    void testRatingupdate() {
        item.setNumOfOrders(1);
        item.updateRating(4.5); // Update the rating with a new value no orders yet
        assertEquals(4.5, item.getRating(), 0.01); // Check if the rating is updated correctly
    }
    @Test
    void testBuyItem() {
        item.setQuantity(10); // Set initial quantity
        item.buyItem(5); // Buy 5 items
        assertEquals(5, item.getQuantity()); // Check if the quantity is updated correctly
        assertEquals(1, item.getNumOfOrders()); // Check if the number of orders is updated correctly
    }
    @Test
    void testBuyItemNotEnoughStock() {
        item.setQuantity(2); // Set initial quantity
        item.buyItem(5); // Try to buy 5 items
        assertEquals(2, item.getQuantity()); // Check if the quantity remains the same
        assertEquals(0, item.getNumOfOrders()); // Check if the number of orders remains the same
    }
}
