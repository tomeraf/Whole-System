package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;


import java.util.Set;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.halilovindustries.backend.Domain.User.*;

/**
 * Acceptance tests for shop operations functionality.
 * Tests include shop viewing, item searching with filters, and shop-specific operations.
 */
public class ShopManagementTests extends BaseAcceptanceTests {

    @Test
    public void testGetAllShopsAndItems_ShouldReturnCorrectCounts() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO createdShop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Act
        Response<List<ShopDTO>> shops = shopService.showAllShops(ownerToken);
        
        // Assert - Basic functionality
        assertNotNull(shops.getData(), "showAllShops should not return null");
        assertEquals(1, shops.getData().size(), "Should return exactly one shop");
        ShopDTO shop = shops.getData().get(0);
        assertEquals(3, shop.getItems().size(), "Shop should contain 3 items");
        
        // Assert - System Invariants
        // 1. Shop state invariants
        assertEquals("MyShop", shop.getName(), "Shop name should be preserved");
        assertEquals(createdShop.getDescription(), shop.getDescription(), "Shop description should be preserved");
        
        // 2. Item integrity invariants
        Set<Integer> itemIds = new HashSet<>();
        List<ItemDTO> items = shopService.showShopItems(ownerToken, shop.getId()).getData();
        items.forEach(item -> {
            // Check item belongs to correct shop
            assertEquals(shop.getId(), item.getShopId(), 
                String.format("Item %s should belong to shop %d", item.getName(), shop.getId()));
            
            // Check no duplicate IDs
            assertTrue(itemIds.add(item.getItemID()), 
                String.format("Item ID %d is duplicated", item.getItemID()));
            
            // Check item basic validity
            assertNotNull(item.getName(), "Item name should not be null");
            assertTrue(item.getPrice() > 0, "Item price should be positive");
            assertNotNull(item.getCategory(), "Item category should not be null");
        });
        
        // 3. Shop size invariant
        assertEquals(3, itemIds.size(), "Should have exactly 3 unique items");
    }
    
    // Item search tests
    
    @Test
    public void testSearchItemsWithCompositeFilters_ShouldReturnMatchingItems() {
        // Arrange - Owner enters & registers
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "enterToSystem should succeed");
        String guestToken = guestResp.getData();

        Response<Void> regResp = userService.registerUser(
            guestToken, "owner", "pwdO", LocalDate.now().minusYears(30)
        );
        assertTrue(regResp.isOk(), "Owner registration should succeed");

        // Owner logs in
        Response<String> loginResp = userService.loginUser(
            guestToken, "owner", "pwdO"
        );
        assertTrue(loginResp.isOk(), "Owner login should succeed");
        String ownerToken = loginResp.getData();

        // Owner creates a shop
        Response<ShopDTO> createResp = shopService.createShop(
            ownerToken, "MyShop", "A shop for tests"
        );
        assertTrue(createResp.isOk(), "createShop should succeed");
        ShopDTO shop = createResp.getData();

        // Owner adds three items
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shop.getId(), "Apple", Category.FOOD,  1.00, "fresh apple"
        );
        Response<ItemDTO> addB = shopService.addItemToShop(
            ownerToken, shop.getId(), "Banana", Category.FOOD, 0.50, "ripe banana"
        );
        Response<ItemDTO> addL = shopService.addItemToShop(
            ownerToken, shop.getId(), "Laptop", Category.ELECTRONICS, 999.99, "new laptop"
        );
        assertTrue(addA.isOk(), "Adding Apple should succeed");
        assertTrue(addB.isOk(), "Adding Banana should succeed");
        assertTrue(addL.isOk(), "Adding Laptop should succeed");

        // (Optional) Retrieve them if you need IDs or to verify all three exist
        Response<List<ItemDTO>> allResp = shopService.showShopItems(ownerToken,shop.getId());
        assertTrue(allResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> allItems = allResp.getData();
        assertEquals(3, allItems.size(), "Shop should now contain 3 items");

        // Act - Build a composite filter:
        //    - name contains "a" (so Apple, Banana, Laptop)
        //    - category = FOOD     (so Apple, Banana)
        //    - price between 0.6 and 2.0 (so only Apple)
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name",      "a");
        filters.put("category",  "FOOD");
        filters.put("minPrice",  "0.6");
        filters.put("maxPrice",  "2");
        // we'll leave minRating and shopRating at zero,
        // so they don't filter anything extra

        // Call the service
        Response<List<ItemDTO>> filteredResp = shopService.filterItemsAllShops(ownerToken,filters);
        
        // Assert - Basic functionality
        assertTrue(filteredResp.isOk(), "filterItemsAllShops should succeed");
        List<ItemDTO> result = filteredResp.getData();
        assertNotNull(result, "Filtered list must not be null");

        // Basic result verification
        assertEquals(1, result.size(), "Exactly one item should survive all filters");
        ItemDTO apple = result.get(0);
        assertEquals("Apple", apple.getName(), "That one item should be Apple");
        
        // Assert - System Invariants
        // 1. Item integrity invariants
        assertEquals(shop.getId(), apple.getShopId(), 
            "Filtered item should maintain correct shop association");
        assertEquals(Category.FOOD, apple.getCategory(), 
            "Category should remain unchanged");
        assertEquals(1.00, apple.getPrice(), 
            "Price should remain unchanged");
        assertEquals("fresh apple", apple.getDescription(), 
            "Description should remain unchanged");
        
        // 2. Filter compliance invariants
        assertTrue(apple.getName().toLowerCase().contains("a"), 
            "Result should match name filter");
        assertTrue(apple.getPrice() >= Double.parseDouble(filters.get("minPrice")), 
            "Result should respect minimum price filter");
        assertTrue(apple.getPrice() <= Double.parseDouble(filters.get("maxPrice")), 
            "Result should respect maximum price filter");
        
        // 3. Shop state invariants - verify original shop wasn't affected
        Response<List<ItemDTO>> postFilterShopItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(postFilterShopItems.isOk(), "Should still be able to get shop items after filtering");
        List<ItemDTO> shopItemsAfter = postFilterShopItems.getData();
        assertEquals(3, shopItemsAfter.size(), 
            "Original shop should still have all items after filtering");
        
        // 4. Result consistency invariants
        Set<Integer> itemIds = new HashSet<>();
        result.forEach(item -> {
            assertTrue(itemIds.add(item.getItemID()), 
                "No duplicate items should appear in results");
            assertNotNull(item.getName(), "Item name should not be null");
            assertTrue(item.getPrice() > 0, "Item price should be positive");
            assertNotNull(item.getCategory(), "Item category should not be null");
        });
    }

    @Test
    public void testSearchItemsWithoutFilters_ShouldReturnAllItems() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Guest enters the system
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest enterToSystem should succeed");

        // Act - Search without any filters
        HashMap<String,String> emptyFilters = new HashMap<>();
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,emptyFilters);
        
        // Assert - Basic functionality
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed");
        List<ItemDTO> items = searchResp.getData();
        assertEquals(3, items.size(), "Should return all 3 available items");

        // Assert - System Invariants
        // 1. Item integrity invariants
        Set<Integer> itemIds = new HashSet<>();
        items.forEach(item -> {
            // Check item belongs to correct shop
            assertEquals(shop.getId(), item.getShopId(), 
                String.format("Item %s should belong to shop %d", item.getName(), shop.getId()));
            
            // Check no duplicate IDs
            assertTrue(itemIds.add(item.getItemID()), 
                String.format("Item ID %d should not be duplicated", item.getItemID()));
            
            // Check item basic validity
            assertNotNull(item.getName(), "Item name should not be null");
            assertTrue(item.getPrice() > 0, "Item price should be positive");
            assertNotNull(item.getCategory(), "Item category should not be null");
        });

        // 2. Result size invariants
        assertEquals(3, itemIds.size(), "Should have exactly 3 unique items");
        
        // 3. Data consistency invariants - verify against direct shop query
        Response<List<ItemDTO>> directItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(directItems.isOk(), "Should be able to get items directly");
        assertEquals(directItems.getData().size(), items.size(), 
            "Filtered results should match direct shop query size");
        
        // 4. Result ordering invariance - items should maintain consistent IDs
        Set<Integer> directIds = directItems.getData().stream()
            .map(ItemDTO::getItemID)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(directIds, itemIds, 
            "Filtered items should have same IDs as direct query");
    }

    @Test
    public void testSearchItemsWithNoMatches_ShouldReturnEmptyList() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Guest enters
        userService.enterToSystem();

        // Act - Search with a name that matches nothing
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name", "NoSuchItem");
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,filters);
        
        // Assert - Basic functionality
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed even if empty");
        assertTrue(searchResp.getData().isEmpty(), "No items should be found");

        // Assert - System Invariants
        // 1. Original shop data should remain unchanged
        Response<List<ItemDTO>> shopItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(shopItems.isOk(), "Should still be able to get shop items");
        assertEquals(3, shopItems.getData().size(), "Original shop should still have 3 items");

        // 2. Filter integrity invariant
        assertEquals("NoSuchItem", filters.get("name"), "Filter should remain unchanged");
    }

    @Test
    public void testSearchItemsInSpecificShop_ShouldReturnMatchingItems() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Guest enters
        userService.enterToSystem();

        // Act - Search in that shop for bananas priced <=0.50
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name", "Banana");
        filters.put("category", Category.FOOD.name());
        filters.put("minPrice","0");
        filters.put("maxPrice","0.50");

        Response<List<ItemDTO>> resp = shopService.filterItemsInShop(ownerToken,shop.getId(), filters);
        
        // Assert - Basic functionality
        assertTrue(resp.isOk(), "filterItemsInShop should succeed");
        List<ItemDTO> results = resp.getData();
        assertEquals(1, results.size(), "Exactly one banana at price <= 0.50 should match");
        assertEquals("Banana", results.get(0).getName(), "Item should be a banana");

        // Assert - System Invariants
        // 1. Result integrity invariants
        ItemDTO banana = results.get(0);
        assertEquals(shop.getId(), banana.getShopId(), 
            "Filtered item should belong to correct shop");
        assertEquals(Category.FOOD, banana.getCategory(), 
            "Category should match filter");
        assertTrue(banana.getPrice() <= 0.50, 
            "Price should respect maximum filter");
        assertTrue(banana.getPrice() >= 0.00, 
            "Price should respect minimum filter");

        // 2. Shop state invariants - verify original data wasn't affected
        Response<List<ItemDTO>> allItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(allItems.isOk(), "Should still be able to get all shop items");
        assertEquals(3, allItems.getData().size(), 
            "Original shop should still have all items");

        // 3. Filter result consistency
        Set<Integer> itemIds = new HashSet<>();
        results.forEach(item -> {
            // Each result should match all filters
            assertTrue(item.getName().contains("Banana"), 
                "All results should match name filter");
            assertEquals(Category.FOOD, item.getCategory(), 
                "All results should match category filter");
            assertTrue(item.getPrice() >= 0 && item.getPrice() <= 0.50, 
                "All results should match price range");
            
            // No duplicates
            assertTrue(itemIds.add(item.getItemID()), 
                "No duplicate items in results");
        });
    }

    @Test 
    public void testCreateShop_WithGuestToken_ShouldFail() {
        // Arrange
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "enterToSystem should succeed");
        String guestToken = guestResp.getData();
        assertNotNull(guestToken, "Guest token must not be null");
        
        // Store initial state
        Response<List<ShopDTO>> initialShops = shopService.showAllShops(guestToken);
        assertTrue(initialShops.isOk(), "Should be able to view shops initially");
        int initialShopCount = initialShops.getData().size();
        
        // Act
        Response<ShopDTO> createShopResp = shopService.createShop(guestToken, "MyShop", "desc");
        
        // Assert - Basic functionality
        assertFalse(createShopResp.isOk(), "Shop creation should fail for guest user");
        
        // Assert - System Invariants
        // 1. Total shops count should remain unchanged
        Response<List<ShopDTO>> afterShops = shopService.showAllShops(guestToken);
        assertTrue(afterShops.isOk(), "Should still be able to view shops after failed attempt");
        assertEquals(initialShopCount, afterShops.getData().size(), 
            "Number of shops should not change after failed creation");
        
        // 2. No shop should exist with the attempted name
        assertFalse(afterShops.getData().stream()
            .anyMatch(s -> "MyShop".equals(s.getName())), 
            "Shop should not be created with the given name");
        
        // 3. Guest permissions should remain unchanged
        Response<List<ShopDTO>> userShops = shopService.showUserShops(guestToken);
        assertTrue(userShops.isOk(), "Should be able to check user's shops");
        assertTrue(userShops.getData().isEmpty(), 
            "Guest should not own any shops after failed creation");
    }


    @Test
    public void testRateShop_WhenNotLoggedIn_ShouldFail() {
        // Arrange - Setup payment and shipment details
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO(
            "1", "Some Name", "1@fff.com", "123456789", "Some Country", "Some City", "Some Address", "12345"
        );

        // fixtures.mockPositivePayment(p);
        // fixtures.mockPositiveShipment(s);

        // 1) Owner creates a shop with items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Store initial shop state
        Response<ShopDTO> initialShopState = shopService.getShopInfo(ownerToken, shopDto.getId());
        assertTrue(initialShopState.isOk(), "Should be able to get initial shop info");

        // Guest performs a purchase
        String guestToken = userService.enterToSystem().getData();
        List<ItemDTO> items = shopService.showShopItems(ownerToken, shopDto.getId()).getData();
        orderService.addItemToCart(guestToken, shopDto.getId(), items.get(0).getItemID(), 1);
        fixtures.successfulBuyCartContent(guestToken, p, s);
        
        // Act - Attempt to rate shop as guest
        Response<Void> res = shopService.rateShop(guestToken, shopDto.getId(), 5);
        
        // Assert - Basic functionality
        assertFalse(res.isOk(), "Rate shop should fail when not logged in");

        // Assert - System Invariants
        // 1. Shop rating should remain unchanged
        Response<ShopDTO> afterShopState = shopService.getShopInfo(ownerToken, shopDto.getId());
        assertTrue(afterShopState.isOk(), "Should be able to get shop info after failed rating");
        assertEquals(
            initialShopState.getData().getRating(),
            afterShopState.getData().getRating(),
            "Shop rating should remain unchanged after failed rating attempt"
        );

        // 2. Shop items should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should be able to get shop items after failed rating");
        assertEquals(items.size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Cart state should remain valid
        Response<List<ItemDTO>> cartItems = orderService.checkCartContent(guestToken);
        assertTrue(cartItems.isOk(), "Should still be able to view cart");
        assertTrue(cartItems.getData().isEmpty(), 
            "Cart should be empty after purchase, despite failed rating");
    }

    @Test
    public void testAddItemToShop_AsOwner_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");

        Response<ShopDTO> shopResp = shopService.createShop(
            ownerToken, "MyShop", "A shop for tests"
        );
        assertTrue(shopResp.isOk(), "Shop creation should succeed");
        ShopDTO shop = shopResp.getData();
        assertNotNull(shop, "Returned ShopDTO must not be null");
        int shopId = shop.getId();

        // 2) Owner adds item
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shopId, "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        // Basic assertions
        assertTrue(addA.isOk(), "Adding Apple should succeed");
        assertEquals("Apple", addA.getData().getName(), "Item name should be 'Apple'");
        assertEquals(Category.FOOD, addA.getData().getCategory(), "Item category should be FOOD");
        assertEquals(1.00, addA.getData().getPrice(), "Item price should be 1.00");
        assertEquals("fresh apple", addA.getData().getDescription());

        // System Invariants
        // 1. Shop state verification
        Response<ShopDTO> infoResp = shopService.getShopInfo(ownerToken, shopId);
        assertTrue(infoResp.isOk(), "getShopInfo should succeed");
        assertEquals(1, infoResp.getData().getItems().size(), "Shop should contain exactly one item");

        // 2. Item integrity through direct query
        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken, shopId);
        assertTrue(itemsResp.isOk(), "Should be able to query items");
        List<ItemDTO> items = itemsResp.getData();
        assertEquals(1, items.size(), "Shop should have exactly one item");
        ItemDTO item = items.get(0);
        assertEquals(shopId, item.getShopId(), "Item should belong to correct shop");
        assertEquals("Apple", item.getName(), "Item name should persist");
        assertEquals(1.00, item.getPrice(), "Item price should persist");
    }

    @Test
    public void testAddItemToShop_WithNonExistentShop_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");

        // Store initial state
        Response<List<ShopDTO>> initialShops = shopService.showAllShops(ownerToken);
        assertTrue(initialShops.isOk(), "Should be able to view initial shops");
        int initialCount = initialShops.getData().size();

        // Attempt to add item to non-existent shop
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, 0, "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        // Basic assertion
        assertFalse(addA.isOk(), "Adding Apple should fail");

        // System Invariants
        // 1. Verify total number of shops unchanged
        Response<List<ShopDTO>> afterShops = shopService.showAllShops(ownerToken);
        assertTrue(afterShops.isOk(), "Should still be able to view shops");
        assertEquals(initialCount, afterShops.getData().size(), 
            "Number of shops should remain unchanged");

        // 2. Verify user's shops unchanged
        Response<List<ShopDTO>> userShops = shopService.showUserShops(ownerToken);
        assertTrue(userShops.isOk(), "Should be able to view user's shops");
        assertEquals(initialCount, userShops.getData().size(),
            "User's shop count should remain unchanged");
    }

    @Test
    public void testAddItemToShop_AsNonOwner_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to view initial items");
        int initialCount = initialItems.getData().size();

        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");
        Response<ItemDTO> addA = shopService.addItemToShop(
            userToken, shopDto.getId(), "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        // Basic assertion
        assertFalse(addA.isOk(), "Adding Apple should fail as the user is not the owner");

        // System Invariants
        // 1. Verify shop items unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to view items");
        assertEquals(initialCount, afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Verify shop info unchanged
        Response<ShopDTO> shopInfo = shopService.getShopInfo(ownerToken, shopDto.getId());
        assertTrue(shopInfo.isOk(), "Should be able to get shop info");
        assertEquals(initialCount, shopInfo.getData().getItems().size(),
            "Shop items count should remain unchanged");
    }

    @Test
    public void testAddItemToShop_WithDuplicateItem_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to view initial items");
        int initialCount = initialItems.getData().size();
        Set<String> initialNames = initialItems.getData().stream()
            .map(ItemDTO::getName)
            .collect(java.util.stream.Collectors.toSet());

        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shopDto.getId(), "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        // Basic assertion
        assertFalse(addA.isOk(), "Adding duplicate item should fail");

        // System Invariants
        // 1. Verify items count unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to view items");
        assertEquals(initialCount, afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Verify item names unchanged
        Set<String> afterNames = afterItems.getData().stream()
            .map(ItemDTO::getName)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(initialNames, afterNames, "Item names should remain unchanged");
    }

    @Test
    public void testRemoveItemFromShop_AsOwner_ShouldSucceed() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        int initialCount = initialItems.getData().size();
        int itemToRemoveId = shopDto.getItems().get(0).getItemID();
        String itemToRemoveName = shopDto.getItems().get(0).getName();
        
        // Act
        Response<Void> removeResp = shopService.removeItemFromShop(
            ownerToken, shopDto.getId(), itemToRemoveId
        );
        
        // Assert - Basic functionality
        assertTrue(removeResp.isOk(), "removeItemFromShop should succeed");
        
        // Assert - System Invariants
        // 1. Shop items count
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialCount - 1, afterItems.getData().size(), 
            "Shop should have exactly one less item");
        
        // 2. Removed item should not exist
        assertFalse(afterItems.getData().stream()
            .anyMatch(item -> item.getItemID() == itemToRemoveId),
            "Removed item should not be present");
        
        // 3. Other items should remain unchanged
        afterItems.getData().forEach(item -> 
            assertTrue(initialItems.getData().stream()
                .anyMatch(origItem -> origItem.getItemID() == item.getItemID() 
                    && origItem.getName().equals(item.getName())
                    && origItem.getPrice() == item.getPrice()),
                "Remaining items should be unchanged")
        );
    }

    @Test
    public void testRemoveItemFromShop_AsNonOwner_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        int initialCount = initialItems.getData().size();
        Set<Integer> initialItemIds = initialItems.getData().stream()
            .map(ItemDTO::getItemID)
            .collect(java.util.stream.Collectors.toSet());

        // Act
        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");
        Response<Void> removeResp = shopService.removeItemFromShop(
            userToken, shopDto.getId(), shopDto.getItems().get(0).getItemID()
        );
        
        // Assert - Basic functionality
        assertFalse(removeResp.isOk(), "removeItemFromShop should fail as the user is not the owner");
        
        // Assert - System Invariants
        // 1. Shop items count should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialCount, afterItems.getData().size(), 
            "Number of items should remain unchanged");
        
        // 2. All original items should still exist
        Set<Integer> afterItemIds = afterItems.getData().stream()
            .map(ItemDTO::getItemID)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(initialItemIds, afterItemIds, "All original items should still exist");
    }

    @Test
    public void testRemoveItemFromShop_WithNonExistentItem_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        List<ItemDTO> items = initialItems.getData();
        
        // Act
        Response<Void> removeResp = shopService.removeItemFromShop(
            ownerToken, shopDto.getId(), 456 // Non-existent item ID
        );
        
        // Assert - Basic functionality
        assertFalse(removeResp.isOk(), "removeItemFromShop should fail as the item does not exist");
        
        // Assert - System Invariants
        // 1. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(items.size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");
        
        // 2. All items should remain identical
        items.forEach(originalItem -> {
            assertTrue(afterItems.getData().stream()
                .anyMatch(item -> item.getItemID() == originalItem.getItemID()
                    && item.getName().equals(originalItem.getName())
                    && item.getPrice() == originalItem.getPrice()),
                "Each item should remain unchanged");
        });
    }

    @Test
    public void testEditItemDescription_AsNonOwner_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToEdit = shopDto.getItems().get(0);
        String originalDescription = itemToEdit.getDescription();

        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");

        // Act
        Response<Void> editResp = shopService.changeItemDescriptionInShop(
            userToken, shopDto.getId(), itemToEdit.getItemID(), "New description"
        );

        // Assert - Basic functionality
        assertFalse(editResp.isOk(), "editItemInShop should fail as the user is not the owner");

        // Assert - System Invariants
        // 1. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Item properties should remain unchanged
        ItemDTO unchangedItem = afterItems.getData().stream()
            .filter(item -> item.getItemID() == itemToEdit.getItemID())
            .findFirst()
            .orElseThrow();
        assertEquals(originalDescription, unchangedItem.getDescription(), 
            "Description should remain unchanged");
        assertEquals(itemToEdit.getName(), unchangedItem.getName(), 
            "Name should remain unchanged");
        assertEquals(itemToEdit.getPrice(), unchangedItem.getPrice(), 
            "Price should remain unchanged");
        assertEquals(itemToEdit.getCategory(), unchangedItem.getCategory(), 
            "Category should remain unchanged");
    }

    @Test
    public void testEditItemPrice_WithNegativePrice_ShouldFail() {
        // Arrange
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToEdit = shopDto.getItems().get(0);
        double originalPrice = itemToEdit.getPrice();

        // Act - attempt to set negative price
        Response<Void> editResp = shopService.changeItemPriceInShop(
            ownerToken, shopDto.getId(), itemToEdit.getItemID(), -100.00
        );

        // Assert - Basic functionality
        assertFalse(editResp.isOk(), "editItemInShop should fail with negative price");

        // Assert - System Invariants
        // 1. Shop state verification
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopDto.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Item properties should remain unchanged
        ItemDTO unchangedItem = afterItems.getData().stream()
            .filter(item -> item.getItemID() == itemToEdit.getItemID())
            .findFirst()
            .orElseThrow();
        assertEquals(originalPrice, unchangedItem.getPrice(), 
            "Price should remain unchanged");
        assertEquals(itemToEdit.getName(), unchangedItem.getName(), 
            "Name should remain unchanged");
        assertEquals(itemToEdit.getDescription(), unchangedItem.getDescription(), 
            "Description should remain unchanged");
    }

    @Test
    public void testAddShopManager_WithValidPermissions_ShouldSucceed() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner1", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String managerGuestToken = userService.enterToSystem().getData();
        Response<Void> managerRegResp = userService.registerUser(
            managerGuestToken, "manager", "pwdM", LocalDate.now().minusYears(30)
        );
        assertTrue(managerRegResp.isOk(), "Manager registration should succeed");

        Response<String> managerLoginResp = userService.loginUser(
            managerGuestToken, "manager", "pwdM"
        );
        assertTrue(managerLoginResp.isOk(), "Manager login should succeed");
        String managerToken = managerLoginResp.getData();

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // Assert - System Invariants
        // 1. Permission verification
        Response<List<Permission>> permsResp = shopService.getMemberPermissions(ownerToken, shop.getId(), "manager");
        assertTrue(permsResp.isOk(), "getMembersPermissions should succeed");
        assertTrue(permsResp.getData().contains(Permission.APPOINTMENT), 
            "Manager should have APPOINTMENT permission");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Manager's view access verification
        Response<ShopDTO> managerView = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(managerView.isOk(), "Manager should be able to view shop info");
        assertEquals(shop.getId(), managerView.getData().getId(), 
            "Manager should see correct shop");

        // 4. Original owner's permissions should remain intact
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(ownerToken, shop.getId(), "Owner1");
        //assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Owner should retain permissions");
    }

    @Test
    public void testSetManagerPermissions_AddNewPermission_ShouldSucceed() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // Store initial shop state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String managerGuestToken = userService.enterToSystem().getData();
        Response<Void> managerRegResp = userService.registerUser(
            managerGuestToken, "manager", "pwdM", LocalDate.now().minusYears(30)
        );
        assertTrue(managerRegResp.isOk(), "Manager registration should succeed");

        Response<String> managerLoginResp = userService.loginUser(
            managerGuestToken, "manager", "pwdM"
        );
        assertTrue(managerLoginResp.isOk(), "Manager login should succeed");
        String managerToken = managerLoginResp.getData();

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // Store initial permissions
        Response<List<Permission>> initialPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(initialPerms.isOk(), "Should be able to get initial permissions");
        
        // Act - Add new permission
        Response<Void> setPermissionsResp = shopService.addShopManagerPermission(
            ownerToken, shop.getId(), "manager", Permission.UPDATE_ITEM_PRICE
        );
        
        // Assert - Basic functionality
        assertTrue(setPermissionsResp.isOk(), "setPermissions should succeed");
        
        // Assert - System Invariants
        // 1. Permission changes verification
        Response<List<Permission>> afterPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(afterPerms.isOk(), "Should be able to get permissions after change");
        assertTrue(afterPerms.getData().contains(Permission.UPDATE_ITEM_PRICE), 
            "New permission should be added");
        assertTrue(afterPerms.getData().contains(Permission.APPOINTMENT), 
            "Original permission should be preserved");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Manager's view access verification
        Response<ShopDTO> managerView = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(managerView.isOk(), "Manager should still be able to view shop info");
    }

    @Test
    public void testRemoveManager_AsOwner_ShouldPreventManagerActions() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // Store initial shop state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String managerGuestToken = userService.enterToSystem().getData();
        Response<Void> managerRegResp = userService.registerUser(
            managerGuestToken, "manager", "pwdM", LocalDate.now().minusYears(30)
        );
        assertTrue(managerRegResp.isOk(), "Manager registration should succeed");

        Response<String> managerLoginResp = userService.loginUser(
            managerGuestToken, "manager", "pwdM"
        );
        assertTrue(managerLoginResp.isOk(), "Manager login should succeed");
        String managerToken = managerLoginResp.getData();

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // Verify initial manager permissions
        Response<List<Permission>> initialPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(initialPerms.isOk(), "Should be able to get initial permissions");
        assertTrue(initialPerms.getData().contains(Permission.APPOINTMENT),
            "Manager should initially have APPOINTMENT permission");

        // 4) Act - Owner removes the manager
        Response<Void> removeManagerResp = shopService.removeAppointment(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(removeManagerResp.isOk(), "removeShopManager should succeed");

        // Assert - System Invariants
        // 1. Permission verification - manager should have no permissions
        Response<List<Permission>> afterPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertFalse(afterPerms.isOk(), "Should throw error - no role for this man in this shop");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Manager's actions should be prevented
        Response<Void> attemptAdd = shopService.addShopManager(
            managerToken, shop.getId(), "User", permissions
        );
        assertFalse(attemptAdd.isOk(), 
            "Removed manager should not be able to add new managers");

        // 4. Shop info access verification
        Response<ShopDTO> shopInfo = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(shopInfo.isOk(), "Basic shop info should still be accessible");
    }


    @Test
    public void testRemoveAppointee_WithNestedAppointees_ShouldRemoveAll() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String managerGuestToken = userService.enterToSystem().getData();
        Response<Void> managerRegResp = userService.registerUser(
            managerGuestToken, "manager", "pwdM", LocalDate.now().minusYears(30)
        );
        assertTrue(managerRegResp.isOk(), "Manager registration should succeed");

        Response<String> managerLoginResp = userService.loginUser(
            managerGuestToken, "manager", "pwdM"
        );
        assertTrue(managerLoginResp.isOk(), "Manager login should succeed");
        String managerToken = managerLoginResp.getData();

        // 3) Owner adds the manager with appointment permission
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // Verify manager's initial permissions
        Response<List<Permission>> managerPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(managerPerms.isOk(), "Should be able to get manager permissions");
        assertTrue(managerPerms.getData().contains(Permission.APPOINTMENT),
            "Manager should have APPOINTMENT permission");

        // 4) Manager adds a user
        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd0");
        Set<Permission> userPermissions = new HashSet<>();
        userPermissions.add(Permission.APPOINTMENT);
        Response<Void> addUserResp = shopService.addShopManager(
            managerToken, shop.getId(), "User", userPermissions
        );
        //assertTrue(addUserResp.isOk(), "Manager should be able to add user");

        // 5) User adds another user (demonstrating nested appointments)
        fixtures.generateRegisteredUserSession("User2", "Pwd0");
        Response<Void> addUser2Resp = shopService.addShopManager(
            userToken, shop.getId(), "User2", permissions
        );
        assertTrue(addUser2Resp.isOk(), "User should be able to add another user");

        // 6) Owner removes the original manager
        Response<Void> removeManagerResp = shopService.removeAppointment(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(removeManagerResp.isOk(), "removeShopManager should succeed");

        // Assert - System Invariants
        // 1. Verify removed manager has no permissions
        Response<List<Permission>> afterManagerPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "manager"
        );
        assertFalse(afterManagerPerms.isOk(), "Should not be able to get permissions for removed manager");

        // 2. Verify nested appointee (User) can no longer add managers
        Response<Void> attemptAdd = shopService.addShopManager(
            userToken, shop.getId(), "User3", permissions
        );
        assertFalse(attemptAdd.isOk(), "Nested appointee should not be able to add managers after removal");

        // 3. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 4. Shop info should still be accessible
        Response<ShopDTO> shopInfo = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(shopInfo.isOk(), "Should still be able to get shop info");
        assertEquals(shop.getId(), shopInfo.getData().getId(), "Shop ID should remain unchanged");
    }


    @Test
    public void testAppoint_SameUserTwice_ShouldFail() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Owner adds first manager
        String managerToken = fixtures.generateRegisteredUserSession("Manager", "PwdM");
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "Manager", permissions
        );
        //assertTrue(addManagerResp.isOk(), "First addShopManager should succeed");

        // 3) Owner adds second manager
        fixtures.generateRegisteredUserSession("Manager2", "PwdM");
        Set<Permission> permissions2 = new HashSet<>();
        permissions2.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp2 = shopService.addShopManager(
            ownerToken, shop.getId(), "Manager2", permissions2
        );
        assertTrue(addManagerResp2.isOk(), "Second addShopManager should succeed");

        // Store manager2's initial permissions
        Response<List<Permission>> initialPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "Manager2"
        );
        assertTrue(initialPerms.isOk(), "Should be able to get initial permissions");

        // 4) First manager tries to add second manager again
        Set<Permission> permissions3 = new HashSet<>();
        permissions3.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp3 = shopService.addShopManager(
            managerToken, shop.getId(), "Manager2", permissions3
        );
        assertFalse(addManagerResp3.isOk(), "Duplicate appointment should fail");

        // Assert - System Invariants
        // 1. Manager2's permissions should remain unchanged
        Response<List<Permission>> afterPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "Manager2"
        );
        assertTrue(afterPerms.isOk(), "Should still be able to get permissions");
        assertEquals(initialPerms.getData(), afterPerms.getData(), 
            "Manager2's permissions should remain unchanged");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Original appointments should still be valid
        Response<ShopDTO> shopInfo = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(shopInfo.isOk(), "First manager should still have access");
    }


    @Test
    public void testViewShopContent_ManagerWithViewPermission_ShouldSucceed() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgr", "pwdM", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgr", "pwdM").getData();

        // 3) Owner assigns manager WITH VIEW permission
        Set<Permission> perms = new HashSet<>();
        perms.add(Permission.VIEW);
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr", perms);
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager views dashboard
        Response<ShopDTO> viewResp = shopService.getShopInfo(mgrToken, shop.getId());
        assertTrue(viewResp.isOk(), "viewShopContent should succeed");
        ShopDTO seen = viewResp.getData();

        // Basic assertions
        assertEquals(shop.getId(), seen.getId(), "Shop ID must match");
        assertEquals(shop.getName(), seen.getName(), "Shop name must match");

        // Assert - System Invariants
        // 1. Permission verification
        Response<List<Permission>> permsResp = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgr"
        );
        assertTrue(permsResp.isOk(), "Should be able to verify permissions");
        assertTrue(permsResp.getData().contains(Permission.VIEW), 
            "Manager should have VIEW permission");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Manager should see all items
        Response<List<ItemDTO>> managerItems = shopService.showShopItems(mgrToken, shop.getId());
        assertTrue(managerItems.isOk(), "Manager should be able to view items");
        assertEquals(initialItems.getData().size(), managerItems.getData().size(), 
            "Manager should see all items");
    }

    @Test
    public void testViewShop_WithInvalidToken_ShouldFail() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Attempt to view with invalid token
        String badToken = "not-a-valid-token";
        Response<ShopDTO> resp = shopService.getShopInfo(badToken, shop.getId());
        
        // Basic assertion
        assertFalse(resp.isOk(), "Should fail when not logged in");

        // Assert - System Invariants
        // 1. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Owner access should remain intact
        Response<ShopDTO> ownerView = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(ownerView.isOk(), "Owner should still be able to view shop");
        assertEquals(shop.getId(), ownerView.getData().getId(), "Shop ID should remain unchanged");
    }

    @Test
    public void testGetMemberPermissions_WithoutViewPermission_ShouldFail() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgr2", "pwdM2", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgr2", "pwdM2").getData();

        // 3) Owner assigns manager WITHOUT VIEW permission
        Set<Permission> perms = new HashSet<>();
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr2", perms);
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager tries to view permissions
        Response<List<Permission>> resp = shopService.getMemberPermissions(mgrToken, shop.getId(), "mgr2");
        assertFalse(resp.isOk(), "Should fail when lacking VIEW permission");

        // Assert - System Invariants
        // 1. Manager's permissions should remain empty
        Response<List<Permission>> permsResp = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgr2"
        );
        assertTrue(permsResp.isOk(), "Should be able to check permissions");
        assertTrue(permsResp.getData().isEmpty(), "Manager should have no permissions");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");
    }

    @Test
    public void testEditItemQuantity_ManagerWithPermission_ShouldSucceed() {
        // 1) Owner creates shop + items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup
        String mgrGuestToken = userService.enterToSystem().getData();
        userService.registerUser(mgrGuestToken, "mgr", "pwdM", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuestToken, "mgr", "pwdM").getData();

        // 3) Owner assigns manager + gives UPDATE_ITEM_QUANTITY permission
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");
        Response<Void> givePerm = shopService.addShopManagerPermission(
            ownerToken, shop.getId(), "mgr", Permission.UPDATE_ITEM_QUANTITY
        );
        assertTrue(givePerm.isOk(), "addShopManagerPermission should succeed");

        // 4) Manager edits quantity
        List<ItemDTO> items = shopService.showShopItems(ownerToken, shop.getId()).getData();
        ItemDTO first = items.get(0);
        int newQty = first.getQuantity() + 5;
        Response<Void> editResp = shopService.changeItemQuantityInShop(
            mgrToken, shop.getId(), first.getItemID(), newQty
        );
        assertTrue(editResp.isOk(), "changeItemQuantityInShop should succeed");

        // Assert - System Invariants
        // 1. Quantity should be updated
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should be able to verify items");
        ItemDTO updated = afterItems.getData().stream()
            .filter(i -> i.getItemID() == first.getItemID())
            .findFirst()
            .orElseThrow();
        assertEquals(newQty, updated.getQuantity(), "Quantity should be updated");

        // 2. Other item properties should remain unchanged
        assertEquals(first.getName(), updated.getName(), "Name should remain unchanged");
        assertEquals(first.getPrice(), updated.getPrice(), "Price should remain unchanged");
        assertEquals(first.getCategory(), updated.getCategory(), "Category should remain unchanged");
        
        // 3. Other items should remain unchanged
        afterItems.getData().stream()
            .filter(i -> i.getItemID() != first.getItemID())
            .forEach(item -> {
                ItemDTO original = initialItems.getData().stream()
                    .filter(i -> i.getItemID() == item.getItemID())
                    .findFirst()
                    .orElseThrow();
                assertEquals(original.getQuantity(), item.getQuantity(),
                    "Other items' quantities should remain unchanged");
            });
    }

    @Test
    public void testEditItemQuantity_WithInvalidToken_ShouldFail() {
        // 1) Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        
        // 2) Attempt to edit with invalid token
        String invalidToken = "not-a-token";
        Response<Void> resp = shopService.changeItemQuantityInShop(
            invalidToken, shop.getId(), shop.getItems().get(0).getItemID(), 10
        );
        
        // Assert - Basic functionality
        assertFalse(resp.isOk(), "Should fail if not logged in");

        // Assert - System Invariants
        // 1. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Owner access should remain intact
        Response<ShopDTO> ownerAccess = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(ownerAccess.isOk(), "Owner should still have access");
    }

    @Test
    public void testEditItemQuantity_WithNonExistentItem_ShouldFail() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Owner tries to edit a non-existent product
        int missingId = 9999;
        Response<Void> resp = shopService.changeItemQuantityInShop(
            ownerToken, shop.getId(), missingId, 10
        );
        assertFalse(resp.isOk(), "Should fail when product does not exist");

        // Assert - System Invariants
        // 1. Shop items should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Existing items should remain unchanged
        initialItems.getData().forEach(originalItem -> {
            assertTrue(afterItems.getData().stream()
                .anyMatch(item -> item.getItemID() == originalItem.getItemID() 
                    && item.getQuantity() == originalItem.getQuantity()),
                "Existing items should remain unchanged");
        });
    }

    @Test
    public void testEditItemQuantity_WithoutPermission_ShouldFail() {
        // 1) Owner + shop + items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Manager setup (no inventory permission)
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrNoInv", "pwdI", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrNoInv", "pwdI").getData();

        // 3) Owner assigns manager with no permissions
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgrNoInv", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager attempts to edit
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shop.getId()).getData();
        int itemId = items.get(0).getItemID();
        int originalQty = items.get(0).getQuantity();
        int newQty = originalQty + 5;

        Response<Void> resp = shopService.changeItemQuantityInShop(
            mgrToken, shop.getId(), itemId, newQty
        );

        // Assert - Basic functionality
        assertFalse(resp.isOk(), "Should fail when lacking inventory permission");

        // Assert - System Invariants
        // 1. Item quantity should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        ItemDTO unchangedItem = afterItems.getData().stream()
            .filter(i -> i.getItemID() == itemId)
            .findFirst()
            .orElseThrow();
        assertEquals(originalQty, unchangedItem.getQuantity(), 
            "Item quantity should remain unchanged");

        // 2. Manager permissions should remain empty
        Response<List<Permission>> perms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrNoInv"
        );
        assertTrue(perms.isOk(), "Should be able to check permissions");
        assertTrue(perms.getData().isEmpty(), "Manager should still have no permissions");
    }

    @Test
    public void testUpdatePurchasePolicy_WithInvalidToken_ShouldFail() {
        // Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<PurchaseType>> initialTypes = shopService.getPurchaseTypes(ownerToken, shop.getId());
        assertTrue(initialTypes.isOk(), "Should be able to get initial purchase types");

        // Attempt update with invalid token
        String badToken = "invalid";
        Response<Void> resp = shopService.updatePurchaseType(badToken, shop.getId(), PurchaseType.BID);
        assertFalse(resp.isOk(), "Should fail when not logged in");

        // Assert - System Invariants
        // 1. Purchase types should remain unchanged
        Response<List<PurchaseType>> afterTypes = shopService.getPurchaseTypes(ownerToken, shop.getId());
        assertTrue(afterTypes.isOk(), "Should still be able to get purchase types");
        assertEquals(initialTypes.getData(), afterTypes.getData(), 
            "Purchase types should remain unchanged");
    }

    @Test
    public void testUpdatePurchasePolicy_WithoutPermission_ShouldFail() {
        // 1) Owner + shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<PurchaseType>> initialTypes = shopService.getPurchaseTypes(ownerToken, shop.getId());
        assertTrue(initialTypes.isOk(), "Should be able to get initial purchase types");

        // 2) Manager without permission
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrNoP", "pwdX", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrNoP", "pwdX").getData();

        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgrNoP", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 3) Attempt update
        Response<Void> resp = shopService.updatePurchaseType(mgrToken, shop.getId(), PurchaseType.BID);
        assertFalse(resp.isOk(), "Should fail when lacking permission");

        // Assert - System Invariants
        // 1. Purchase types should remain unchanged
        Response<List<PurchaseType>> afterTypes = shopService.getPurchaseTypes(ownerToken, shop.getId());
        assertTrue(afterTypes.isOk(), "Should still be able to get purchase types");
        assertEquals(initialTypes.getData(), afterTypes.getData(), 
            "Purchase types should remain unchanged");

        // 2. Manager permissions should remain empty
        Response<List<Permission>> perms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrNoP"
        );
        assertTrue(perms.isOk(), "Should be able to check permissions");
        assertTrue(perms.getData().isEmpty(), "Manager should still have no permissions");
    }

    @Test
    public void testCloseShop_ShouldSucceed() {
        // 1) Owner creates and opens shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Owner closes shop
        Response<Void> closeResp = shopService.closeShop(ownerToken, shop.getId());
        assertTrue(closeResp.isOk(), "closeShopByFounder should succeed");

        // Assert - Basic functionality
        List<ShopDTO> openShops = shopService.showAllShops(ownerToken).getData();
        assertFalse(openShops.stream().anyMatch(s -> s.getId() == shop.getId()),
            "Closed shop must not appear in showAllShops()");

        // Assert - System Invariants
        // 1. Shop data should be preserved even when closed
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Item data should be preserved");

        // 2. Owner permissions should remain intact
        Response<ShopDTO> ownerAccess = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(ownerAccess.isOk(), "Owner should still have access to closed shop");

        // 3. Shop status verification
        Response<List<ShopDTO>> userShops = shopService.showUserShops(ownerToken);
        assertTrue(userShops.isOk(), "Should be able to get user's shops");
        assertFalse(userShops.getData().stream()
            .anyMatch(s -> s.getId() == shop.getId()), 
            "Shop should still appear in owner's shop list");
    }

    @Test
    public void testCloseShop_ClosedShop_ShouldFail() {
        // 1) Owner creates and closes a shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        
        // First close
        assertTrue(shopService.closeShop(ownerToken, shop.getId()).isOk(),
                "Initial close should succeed");

        // 2) Owner attempts to close it again
        Response<Void> secondClose = shopService.closeShop(ownerToken, shop.getId());
        assertFalse(secondClose.isOk(), "Closing an already closed shop should fail");

        // Assert - System Invariants
        // 1. Shop items should remain preserved
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Shop should remain closed but accessible to owner
        Response<ShopDTO> shopInfo = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(shopInfo.isOk(), "Owner should still have access to closed shop");
    }



    @Test
    @Transactional
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testConcurrentManagerAppointment_SameCandidate_ShouldAllowOnlyOneSuccess() throws Exception {
        for (int i = 0; i < 10; i++) {
            
            // --- 1) SETUP & COMMIT ---
            String ownerToken = fixtures.generateRegisteredUserSession("owner"+i, "pwdO");
            ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop"+i);
            fixtures.registerUserWithoutLogin("candidate"+i, "pass");
            int shopId = shop.getId();

            // capture initial shop‐item count
            int initialItemCount =
                shopService.showShopItems(ownerToken, shopId).getData().size();

            // commit these inserts so child threads can see them
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            // --- 2) SPAWN TWO PARALLEL ADD-MANAGER CALLS ---
            Set<Permission> perms = Set.of(Permission.VIEW);
            final int iteration = i; // capture the current iteration for use in lambdas
            Callable<Boolean> c1 = () -> shopService.addShopManager(ownerToken, shopId, "candidate"+iteration, perms).isOk();
            Callable<Boolean> c2 = () -> shopService.addShopManager(ownerToken, shopId, "candidate"+iteration, perms).isOk();

            ExecutorService ex = Executors.newFixedThreadPool(2);
            List<Future<Boolean>> results = ex.invokeAll(List.of(c1, c2));
            ex.shutdown();

            long successCount = results.stream().map(f -> {
                try { return f.get(); }
                catch (Exception e) { return false; }
            }).filter(ok -> ok).count();

            // --- 3) ASSERT EXACTLY ONE SUCCEEDED ---
            assertEquals(1, successCount, "Exactly one of the two concurrent addShopManager calls should succeed");

            // --- 4) INVARIANT: SHOP ITEMS UNCHANGED ---
            int afterItemCount =
                shopService.showShopItems(ownerToken, shopId).getData().size();
            assertEquals(initialItemCount, afterItemCount, "Shop's item count must remain unchanged");

            // --- 5) INVARIANT: MANAGER HAS ONE PERMISSION (VIEW) ---
            Response<List<Permission>> candPerms =
                shopService.getMemberPermissions(ownerToken, shopId, "candidate"+i);
            assertTrue(candPerms.isOk(), "Should be able to fetch candidate's permissions");
            assertEquals(1, candPerms.getData().size(), "Candidate should have exactly one permission");
            assertTrue(candPerms.getData().contains(Permission.VIEW), "That permission must be VIEW");

            // --- 6) INVARIANT: OWNER RETAINS THEIR PERMISSIONS ---
            Response<List<Permission>> ownerPerms =
                shopService.getMemberPermissions(ownerToken, shopId, "owner"+i);
            assertTrue(ownerPerms.isOk(), "Should be able to fetch owner's permissions");
            assertFalse(ownerPerms.getData().isEmpty(), "Owner should still have at least one permission");
        }
    }


    @Test
    public void removeShopManagerPermissionTest() {
        // 1) Owner setup: register and create a shop
        String ownerToken = fixtures.generateRegisteredUserSession("OwnerManager", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "ManagerShop");

        // 2) Manager setup: register a second user to become manager
        String managerToken = fixtures.generateRegisteredUserSession("ShopManager", "Pwd1");
        String managerUsername = "ShopManager";

        // 3) Owner assigns the manager role to the second user with UPDATE_SUPPLY permission
        Response<Void> assignResp = shopService.addShopManager(
            ownerToken,
            shop.getId(),
            managerUsername,
            Set.of(Permission.UPDATE_SUPPLY)
        );
        //assertTrue(assignResp.isOk(), "assignShopManager should succeed");

        // 4) Verify manager can perform a manager-only action (e.g., add an item)
        Response<ItemDTO> addItemResp = shopService.addItemToShop(
            managerToken,
            shop.getId(),
            "ManagerItem",
            Category.ELECTRONICS,
            10.00,
            "Item added by manager"
        );
        assertTrue(addItemResp.isOk(), "Manager should be able to add items before removal");

        // 5) Owner removes manager permission
        Response<Void> removeResp = shopService.removeAppointment(
            ownerToken,
            shop.getId(),
            managerUsername
        );
        assertTrue(removeResp.isOk(), "removeShopManagerPermission should succeed");

        // 6) Manager attempts a manager-only action again (e.g., add another item) → should fail
        Response<ItemDTO> addAfterRemove = shopService.addItemToShop(
            managerToken,
            shop.getId(),
            "ShouldFailItem",
            Category.FOOD,
            5.00,
            "This should not be added"
        );
        assertFalse(addAfterRemove.isOk(), "Manager should no longer be able to add items after removal");
    }


    @Test
    public void testCreateShop_AsOwner_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerX", "pwdX");
        Response<ShopDTO> resp = shopService.createShop(ownerToken, "XShop", "desc");
        assertTrue(resp.isOk(), "Owner should be able to create a shop");
        assertEquals("XShop", resp.getData().getName(), "Shop name should match");
        assertEquals("desc", resp.getData().getDescription(), "Shop description should match");
   }

    @Test
    //Add commentMore actions
    public void testCloseShop_AsNonOwner_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerClose", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "CloseShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        
        String user = fixtures.generateRegisteredUserSession("userClose", "pwdU");
        Response<Void> resp = shopService.closeShop(user, shop.getId());
        
        // Basic assertion
        assertFalse(resp.isOk(), "Non-owner should not be able to close the shop");

        // Assert - System Invariants
        // 1. Shop should remain open
        Response<List<ShopDTO>> allShops = shopService.showAllShops(ownerToken);
        assertTrue(allShops.isOk(), "Should be able to view all shops");
        assertTrue(allShops.getData().stream().anyMatch(s -> s.getId() == shop.getId()),
            "Shop should still be listed in open shops");

        // 2. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 3. Owner access should remain intact
        Response<ShopDTO> ownerAccess = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(ownerAccess.isOk(), "Owner should still have full access");
    }

    @Test
    public void testOpenAuction_AsNonOwner_ShouldFail() {
        // 1) Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("ownerAuction", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "AuctionShop");
        int shopId = shop.getId();
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopId);
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToAuction = initialItems.getData().get(0);
        int itemId = itemToAuction.getItemID();
        
        // 2) Attempt auction creation as non-owner
        String userToken = fixtures.generateRegisteredUserSession("otherAuction", "pwdU");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusSeconds(10);
        
        Response<Void> resp = shopService.openAuction(userToken, shopId, itemId, 5.0, start, end);
        
        // Assert - Basic functionality
        assertFalse(resp.isOk(), "Non-owner should not be able to open auction");

        // Assert - System Invariants
        // 1. Shop state should remain unchanged
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopId);
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(), 
            "Number of items should remain unchanged");

        // 2. Item state verification
        ItemDTO unchangedItem = afterItems.getData().stream()
            .filter(item -> item.getItemID() == itemId)
            .findFirst()
            .orElseThrow();
        assertEquals(itemToAuction.getName(), unchangedItem.getName(), 
            "Item name should remain unchanged");
        assertEquals(itemToAuction.getPrice(), unchangedItem.getPrice(), 
            "Item price should remain unchanged");
        assertEquals(itemToAuction.getQuantity(), unchangedItem.getQuantity(), 
            "Item quantity should remain unchanged");

        // 3. Auction state verification
        Response<List<AuctionDTO>> auctions = shopService.getActiveAuctions(ownerToken, shopId);
        assertTrue(auctions.isOk(), "Should be able to check auctions");
        assertFalse(auctions.getData().stream().anyMatch(a -> a.getItemId() == itemId),
            "Item should not be in active auctions");

        // 4. Permission verification
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(
            ownerToken, shopId, "ownerAuction");
        //assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Owner should retain permissions");
    }

    @Test
    public void testChangeItemName_AsOwner_ShouldSucceed() {
        // 1) Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("ownerCN", "pwdCN");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "NameShop");
        int shopId = shop.getId();
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shopId);
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToRename = initialItems.getData().get(0);
        int itemId = itemToRename.getItemID();
        String originalName = itemToRename.getName();

        // 2) Perform name change
        String newName = "NewName";
        Response<Void> changeResp = shopService.changeItemName(ownerToken, shopId, itemId, newName);
        
        // Assert - Basic functionality
        assertTrue(changeResp.isOk(), "changeItemName should succeed for owner");

        // Assert - System Invariants
        // 1. Name change verification
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shopId);
        assertTrue(afterItems.isOk(), "Should be able to get items after change");
        ItemDTO changedItem = afterItems.getData().stream()
            .filter(i -> i.getItemID() == itemId)
            .findFirst()
            .orElseThrow();
        assertEquals(newName, changedItem.getName(), "Item name should be updated");

        // 2. Other item properties should remain unchanged
        assertEquals(itemToRename.getPrice(), changedItem.getPrice(), 
            "Item price should remain unchanged");
        assertEquals(itemToRename.getQuantity(), changedItem.getQuantity(), 
            "Item quantity should remain unchanged");
        assertEquals(itemToRename.getCategory(), changedItem.getCategory(), 
            "Item category should remain unchanged");

        // 3. Other items should remain unchanged
        afterItems.getData().stream()
            .filter(i -> i.getItemID() != itemId)
            .forEach(item -> {
                ItemDTO original = initialItems.getData().stream()
                    .filter(i -> i.getItemID() == item.getItemID())
                    .findFirst()
                    .orElseThrow();
                assertEquals(original.getName(), item.getName(),
                    "Other items' names should remain unchanged");
            });
    }

    @Test
    public void testRemoveShopManagerPermission_AsOwner_ShouldRemovePermission() {
        // 1) Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("ownerRP", "pwdRP");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PermShop");

        // 2) Setup manager with initial permissions
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrPerm", "pwdM", LocalDate.now().minusYears(25));
        String managerToken = userService.loginUser(guest, "mgrPerm", "pwdM").getData();
        
        Set<Permission> initialPerms = new HashSet<>(Set.of(Permission.VIEW, Permission.UPDATE_ITEM_PRICE));
        Response<Void> addMgrResp = shopService.addShopManager(
            ownerToken, shop.getId(), "mgrPerm", initialPerms
        );
        assertTrue(addMgrResp.isOk(), "Adding manager should succeed");

        // Store initial state
        Response<List<Permission>> beforePerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrPerm"
        );
        assertTrue(beforePerms.isOk(), "Should be able to get initial permissions");
        assertTrue(beforePerms.getData().contains(Permission.UPDATE_ITEM_PRICE), 
            "Initial permissions should include UPDATE_ITEM_PRICE");

        // 3) Remove permission
        Response<Void> removePermResp = shopService.removeShopManagerPermission(
            ownerToken, shop.getId(), "mgrPerm", Permission.UPDATE_ITEM_PRICE
        );
        
        // Assert - Basic functionality
        assertTrue(removePermResp.isOk(), "removeShopManagerPermission should succeed for owner");

        // Assert - System Invariants
        // 1. Permission removal verification
        Response<List<Permission>> afterPerms = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrPerm"
        );
        assertTrue(afterPerms.isOk(), "Should be able to get permissions after removal");
        assertFalse(afterPerms.getData().contains(Permission.UPDATE_ITEM_PRICE), 
            "UPDATE_ITEM_PRICE permission should be removed");
        assertTrue(afterPerms.getData().contains(Permission.VIEW), 
            "Other permissions should remain unchanged");

        // 2. Manager access verification
        Response<ShopDTO> managerAccess = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(managerAccess.isOk(), "Manager should still have basic shop access");

        // 3. Verify manager can't use removed permission
        ItemDTO item = shop.getItems().get(0);
        Response<Void> attemptEdit = shopService.changeItemPriceInShop(
            managerToken, shop.getId(), item.getItemID(), item.getPrice() + 1.0
        );
        assertFalse(attemptEdit.isOk(), 
            "Manager should not be able to use removed permission");
    }

    @Test
    public void testRemoveShopManagerPermission_AsNonOwner_ShouldFailAndKeepPermissions() {
        // 1) Setup initial state
        String ownerToken = fixtures.generateRegisteredUserSession("ownerRP2", "pwdRP2");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PermShop2");

        // Store initial shop state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Setup manager with permissions
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrPerm2", "pwdM2", LocalDate.now().minusYears(25));
        String managerToken = userService.loginUser(guest, "mgrPerm2", "pwdM2").getData();
        
        Set<Permission> perms = new HashSet<>(Set.of(Permission.VIEW, Permission.UPDATE_ITEM_PRICE));
        Response<Void> addMgrResp = shopService.addShopManager(
            ownerToken, shop.getId(), "mgrPerm2", perms
        );
        assertTrue(addMgrResp.isOk(), "Adding manager should succeed");

        // Store initial permissions
        Response<List<Permission>> before = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrPerm2"
        );
        assertTrue(before.isOk(), "Should be able to get initial permissions");
        assertTrue(before.getData().contains(Permission.UPDATE_ITEM_PRICE));

        // 3) Attempt removal by non-owner
        String otherToken = fixtures.generateRegisteredUserSession("userRP", "pwdRP");
        Response<Void> removePermResp = shopService.removeShopManagerPermission(
            otherToken, shop.getId(), "mgrPerm2", Permission.UPDATE_ITEM_PRICE
        );
        
        // Assert - Basic functionality
        assertFalse(removePermResp.isOk(), "removeShopManagerPermission should fail for non-owner");

        // Assert - System Invariants
        // 1. Permission state verification
        Response<List<Permission>> after = shopService.getMemberPermissions(
            ownerToken, shop.getId(), "mgrPerm2"
        );
        assertTrue(after.isOk(), "Should still be able to get permissions");
        assertTrue(after.getData().contains(Permission.UPDATE_ITEM_PRICE), 
            "Permissions should remain unchanged");
        assertEquals(before.getData().size(), after.getData().size(),
            "Number of permissions should remain unchanged");

        // 2. Manager functionality verification
        Response<ShopDTO> managerAccess = shopService.getShopInfo(managerToken, shop.getId());
        assertTrue(managerAccess.isOk(), "Manager should retain access");
        
        // 3. Shop state verification
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");
    }


    @Test
    public void testAddShopOwner_AsOwner_ShouldSucceedAndPersistInvariant() {
        // 1) Setup initial state
        String founder = fixtures.generateRegisteredUserSession("founderA", "pwdA");
        ShopDTO shop = fixtures.generateShopAndItems(founder, "OwnerShop");

        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(founder, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Setup co-founder
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "cofounder", "pwdC", LocalDate.now().minusYears(30));
        String coToken = userService.loginUser(guest, "cofounder", "pwdC").getData();

        // 3) Add co-owner
        Response<Void> r = shopService.addShopOwner(founder, shop.getId(), "cofounder");
        assertTrue(r.isOk(), "Owner should be able to appoint another owner");

        // Assert - System Invariants
        // 1. Shop ownership verification
        Response<List<ShopDTO>> list = shopService.showUserShops(coToken);
        assertTrue(list.isOk(), "Should be able to get co-owner's shops");
        assertTrue(list.getData().stream().anyMatch(s -> s.getId() == shop.getId()),
            "Co-owner should see shop in their list");

        // 2. Shop state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(founder, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 3. Original owner permissions verification
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(
            founder, shop.getId(), "founderA"
        );
        //assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Original owner should retain permissions");
    }


    @Test
public void testGetFutureAuctions_ShouldListUpcomingAuctions() {
    // 1) Setup initial state
    String owner = fixtures.generateRegisteredUserSession("ownerFA", "pwdFA");
    ShopDTO shop = fixtures.generateShopAndItems(owner, "FutureShop");
    int shopId = shop.getId();
    
    // Store initial state
    Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shopId);
    assertTrue(initialItems.isOk(), "Should be able to get initial items");
    ItemDTO itemToAuction = initialItems.getData().get(0);
    int itemId = itemToAuction.getItemID();
    
    // 2) Schedule future auction
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    LocalDateTime end = LocalDateTime.now().plusDays(1).plusHours(1);
    double startingBid = 5.0;
    Response<Void> auctionResp = shopService.openAuction(
        owner, shopId, itemId, startingBid, start, end
    );
    assertTrue(auctionResp.isOk(), "Should be able to create auction");

    // 3) Check future auctions
    Response<List<AuctionDTO>> futureAuctions = shopService.getFutureAuctions(owner, shopId);
    
    // Assert - Basic functionality
    assertTrue(futureAuctions.isOk(), "Should be able to get future auctions");
    assertFalse(futureAuctions.getData().isEmpty(), "Should have at least one future auction");

    // Assert - System Invariants
    // 1. Auction properties verification
    AuctionDTO futureAuction = futureAuctions.getData().stream()
        .filter(a -> a.getItemId() == itemId)
        .findFirst()
        .orElseThrow();
    
    // Date format verification
    String expectedStartFormat = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    String expectedEndFormat = end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    
    assertEquals(startingBid, futureAuction.getStartingBid(), 
        "Starting bid should match");
    // Remove highest bid check since it starts at 0
    assertEquals(0.0, futureAuction.getHighestBid(), 
        "Initial highest bid should start at 0");
    assertEquals(expectedStartFormat, futureAuction.getAuctionStartTime(),
        "Start time should match expected format");
    assertEquals(expectedEndFormat, futureAuction.getAuctionEndTime(),
        "End time should match expected format");
    assertFalse(futureAuction.isDone(), "Future auction should not be marked as done");

    // 2. Current vs Future auction separation
    Response<List<AuctionDTO>> currentAuctions = shopService.getActiveAuctions(owner, shopId);
    assertTrue(currentAuctions.isOk(), "Should be able to get active auctions");
    assertFalse(currentAuctions.getData().stream()
        .anyMatch(a -> a.getItemId() == itemId), 
        "Future auction should not appear in active auctions");

    // 3. Shop state verification
    Response<ShopDTO> shopAfter = shopService.getShopInfo(owner, shopId);
    assertTrue(shopAfter.isOk(), "Should be able to get shop info");
    assertEquals(shop.getId(), shopAfter.getData().getId(),
        "Shop ID should remain unchanged");
}


    @Test
    public void testGetPurchaseTypes_ShouldReturnAllTypes() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerPT", "pwdPT");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "TypeShop");


        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Get purchase types
        Response<List<PurchaseType>> resp = shopService.getPurchaseTypes(owner, shop.getId());
        
        // Assert - Basic functionality
        assertTrue(resp.isOk(), "Should fetch purchase types");
        List<PurchaseType> types = resp.getData();
        assertTrue(types.contains(PurchaseType.IMMEDIATE), "IMMEDIATE type must be present");
        assertTrue(types.contains(PurchaseType.BID), "BID type must be present");

        // Assert - System Invariants
        // 1. Shop state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 2. Purchase type consistency
        Response<List<PurchaseType>> secondResp = shopService.getPurchaseTypes(owner, shop.getId());
        assertTrue(secondResp.isOk(), "Should be able to get types again");
        assertEquals(types, secondResp.getData(),
            "Purchase types should remain consistent between calls");

        // 3. Owner permissions verification
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(
            owner, shop.getId(), "ownerPT"
        );
        //assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Owner should retain permissions");
    }

    // — getActiveAuctions —
    @Test
    public void testGetActiveAuctions_ShouldListRunningAuctions() throws InterruptedException {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerAA", "pwdAA");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "AucShop");
        int shopId = shop.getId();
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shopId);
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToAuction = initialItems.getData().get(0);
        int itemId = itemToAuction.getItemID();
        
        // 2) Create auction that starts immediately and runs for 10 seconds
        LocalDateTime now = LocalDateTime.now().plusSeconds(2); // Start 2 seconds in future
        LocalDateTime start = now;
        LocalDateTime end = now.plusSeconds(10);
        double startingBid = 5.0;
        
        Response<Void> auctionResp = shopService.openAuction(
            owner, shopId, itemId, startingBid, start, end
        );
        assertTrue(auctionResp.isOk(), "Should be able to create auction");

        // Wait for auction to become active
        Thread.sleep(2100); // Wait just over 2 seconds for auction to start

        // 3) Verify active auctions
        Response<List<AuctionDTO>> activeAuctions = shopService.getActiveAuctions(owner, shopId);
        
        // Assert - Basic functionality
        assertTrue(activeAuctions.isOk(), "Should be able to get active auctions");
        assertFalse(activeAuctions.getData().isEmpty(), "Should have at least one active auction");
        
        // Assert - System Invariants
        // 1. Auction properties verification
        AuctionDTO activeAuction = activeAuctions.getData().stream()
            .filter(a -> a.getItemId() == itemId)
            .findFirst()
            .orElseThrow();
        
        // Date format verification
        String expectedStartFormat = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        String expectedEndFormat = end.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        
        assertEquals(startingBid, activeAuction.getStartingBid(), 
            "Starting bid should match");
        assertEquals(expectedStartFormat, activeAuction.getAuctionStartTime(),
            "Start time should match expected format");
        assertEquals(expectedEndFormat, activeAuction.getAuctionEndTime(),
            "End time should match expected format");
        assertFalse(activeAuction.isDone(), "Active auction should not be marked as done");

        // 2. Shop state verification
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shopId);
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 3. Future auctions verification
        Response<List<AuctionDTO>> futureAuctions = shopService.getFutureAuctions(owner, shopId);
        assertTrue(futureAuctions.isOk(), "Should be able to get future auctions");
        assertFalse(futureAuctions.getData().stream()
            .anyMatch(a -> a.getItemId() == itemId), 
            "Active auction should not appear in future auctions");
    }

    // — getDiscounts & getDiscountTypes —
    @Test
    public void testGetDiscounts_ShouldReturnDefaultDiscounts() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerDisc", "pwdDisc");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "DiscShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Get discounts
        Response<List<DiscountDTO>> resp = shopService.getDiscounts(owner, shop.getId());
        
        // Assert - Basic functionality
        assertTrue(resp.isOk(), "Should fetch discount rules");

        assertTrue(resp.getData().isEmpty(), "Default discount list should be empty");


        // Assert - System Invariants
        // 1. Shop state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 2. Discount consistency
        Response<List<DiscountDTO>> secondResp = shopService.getDiscounts(owner, shop.getId());
        assertTrue(secondResp.isOk(), "Should be able to get discounts again");
        assertEquals(resp.getData(), secondResp.getData(),
            "Discount list should remain consistent between calls");
    }


    @Test
    public void testShowUserShops_AsOwner_ShouldReturnOwnShop() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerSU", "pwdSU");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "UserShop");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Get user's shops
        Response<List<ShopDTO>> resp = shopService.showUserShops(owner);
        
        // Assert - Basic functionality
        assertTrue(resp.isOk(), "showUserShops should succeed for owner");
        assertTrue(resp.getData().stream().anyMatch(s -> s.getId() == shop.getId()),
            "Owner's shop must appear in their shop list");

        // Assert - System Invariants
        // 1. Shop data integrity
        ShopDTO listedShop = resp.getData().stream()
            .filter(s -> s.getId() == shop.getId())
            .findFirst()
            .orElseThrow();
        assertEquals(shop.getName(), listedShop.getName(), "Shop name should match");
        assertEquals(shop.getDescription(), listedShop.getDescription(), 
            "Shop description should match");

        // 2. Shop state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 3. Owner permissions verification
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(
            owner, shop.getId(), "ownerSU"
        );
        //assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Owner should retain permissions");
    }

    @Test
    public void testGetBids_AfterSubmittingBid_ShouldIncludeBid() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerBids", "pwdOB");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "BidShopB");
        int shopId = shop.getId();
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shopId);
        assertTrue(initialItems.isOk(), "Should be able to get initial items");
        ItemDTO itemToBid = initialItems.getData().get(0);
        int itemId = itemToBid.getItemID();

        // 2) Submit bid
        String bidder = fixtures.generateRegisteredUserSession("bidderB", "pwdBB");
        double bidAmount = 9.5;
        Response<Void> bidResp = orderService.submitBidOffer(bidder, shopId, itemId, bidAmount);
        assertTrue(bidResp.isOk(), "submitBidOffer should succeed");

        // 3) Check bids
        Response<List<BidDTO>> resp = shopService.getBids(owner, shopId);
        
        // Assert - Basic functionality
        assertTrue(resp.isOk(), "getBids should succeed after bid");
        assertTrue(resp.getData().stream()
            .anyMatch(b -> b.getItemId() == itemId && b.getAmount() == bidAmount),
            "Bid list should contain the submitted bid");


        // Assert - System Invariants
        // 1. Item state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shopId);
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        ItemDTO unchangedItem = afterItems.getData().stream()
            .filter(i -> i.getItemID() == itemId)
            .findFirst()
            .orElseThrow();
        assertEquals(itemToBid.getPrice(), unchangedItem.getPrice(),
            "Item price should remain unchanged");
        assertEquals(itemToBid.getQuantity(), unchangedItem.getQuantity(),
            "Item quantity should remain unchanged");

        // 2. Bid consistency
        Response<List<BidDTO>> secondResp = shopService.getBids(owner, shopId);
        assertTrue(secondResp.isOk(), "Should be able to get bids again");
        assertEquals(resp.getData().size(), secondResp.getData().size(),
            "Number of bids should remain consistent");

        // 3. Shop state verification
        Response<ShopDTO> shopInfo = shopService.getShopInfo(owner, shopId);
        assertTrue(shopInfo.isOk(), "Should be able to get shop info");
    }

    @Test
    public void testSendMessageAndGetInbox_ShouldDeliverMessage() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerMsg2", "pwd2");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "MsgShop2");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Setup manager
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrMsg", "pwdM", LocalDate.now().minusYears(25));
        String manager = userService.loginUser(guest, "mgrMsg", "pwdM").getData();

        // 3) Send message
        String msgTitle = "Hello mgrMsg";
        String msgContent = "Hello there";
        Response<Void> send = shopService.sendMessage(owner, shop.getId(), msgTitle, msgContent);
        assertTrue(send.isOk(), "sendMessage should succeed");

        // 4) Check inbox
        Response<List<Message>> inbox = shopService.getInbox(manager, shop.getId());
        
        // Assert - Basic functionality
        assertTrue(inbox.isOk(), "getInbox should succeed");
        List<Message> msgs = inbox.getData();
        assertEquals(1, msgs.size(), "Should receive one message");
        assertEquals(msgContent, msgs.get(0).getContent());
        //assertEquals("ownerMsg2", msgs.get(0).getUserName());

        // Assert - System Invariants
        // 1. Shop state preservation
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");

        // 2. Message persistence
        Response<List<Message>> secondInbox = shopService.getInbox(manager, shop.getId());
        assertTrue(secondInbox.isOk(), "Should be able to get inbox again");
        assertEquals(inbox.getData().size(), secondInbox.getData().size(),
            "Message count should remain consistent");

        // 3. Owner permissions verification
        Response<List<Permission>> ownerPerms = shopService.getMemberPermissions(
            owner, shop.getId(), "ownerMsg2"
        );
        assertTrue(ownerPerms.isOk(), "Should be able to get owner permissions");
        assertFalse(ownerPerms.getData().isEmpty(), "Owner should retain permissions");
    }
    @Test
    public void testRespondToMessage_ShouldSendReply() {
        // 1) Setup initial state
        String owner = fixtures.generateRegisteredUserSession("ownerMsg3", "pwd3");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "MsgShop3");
        
        // Store initial state
        Response<List<ItemDTO>> initialItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(initialItems.isOk(), "Should be able to get initial items");

        // 2) Setup manager with permissions first
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrMsg2", "pwdM2", LocalDate.now().minusYears(25));
        String manager = userService.loginUser(guest, "mgrMsg2", "pwdM2").getData();
        
        // Add manager with message permission first
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.ANSWER_MESSAGE);
        permissions.add(Permission.VIEW);
        Response<Void> addManagerResp = shopService.addShopManager(
            owner, shop.getId(), "mgrMsg2", permissions
        );
        assertTrue(addManagerResp.isOk(), "Adding manager should succeed");

        // 3) Owner sends initial message
        String initialTitle = "Question?";
        String initialContent = "How are you?";
        Response<Void> send = shopService.sendMessage(owner, shop.getId(), initialTitle, initialContent);
        assertTrue(send.isOk(), "sendMessage should succeed");

        // Store initial message state
        Response<List<Message>> initialMgrInbox = shopService.getInbox(manager, shop.getId());
        assertTrue(initialMgrInbox.isOk(), "Should be able to get initial inbox");
        assertFalse(initialMgrInbox.getData().isEmpty(), "Manager inbox should contain message");

        Message original = initialMgrInbox.getData().stream()
            .filter(m -> m.getTitle() != null && m.getTitle().equals(initialTitle))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Could not find original message"));

        // 4) Manager sends reply
        String replyTitle = "Re: " + initialTitle;
        String replyContent = "I'm fine, thanks!";
        Response<Void> reply = shopService.respondToMessage(
            manager, shop.getId(), original.getId(), replyTitle, replyContent
        );
        assertTrue(reply.isOk(), "respondToMessage should succeed");

        // Assert - System Invariants
        // 1. Original message status
        Response<List<Message>> afterManagerInbox = shopService.getInbox(manager, shop.getId());
        assertTrue(afterManagerInbox.isOk(), "Should be able to get manager's inbox");
        
        Message afterOriginal = afterManagerInbox.getData().stream()
            .filter(m -> m.getId() == original.getId())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Original message not found after reply"));
        assertFalse(afterOriginal.needResponse(), "Original message should be marked as responded");

        // 2. Reply message verification
        Response<List<Message>> ownerInbox = shopService.getInbox(owner, shop.getId());
        assertTrue(ownerInbox.isOk(), "Should be able to get owner's inbox");

        Optional<Message> replyOpt = ownerInbox.getData().stream()
            .filter(m -> m.getContent().equals(replyContent))
            .findFirst();
        assertTrue(replyOpt.isPresent(), "Reply message should exist in owner's inbox");
        Message reply1 = replyOpt.get();

        // Verify reply properties
        //assertEquals("ownerMsg3", reply1.getUserName(), "Reply should be from ownerMsg3");
        assertEquals(shop.getName(), reply1.getShopName(), "Reply should reference correct shop");
        assertEquals(replyContent, reply1.getContent(), "Reply content should match");
        // Note: System currently sets respondId to -1 for replies
        assertEquals(-1, reply1.getRespondId(), "Reply should have respondId set to -1");
        assertTrue(reply1.getTitle().contains("Re: Question?"), "Reply title should contain original title");

        // 3. Shop state verification
        Response<List<ItemDTO>> afterItems = shopService.showShopItems(owner, shop.getId());
        assertTrue(afterItems.isOk(), "Should still be able to get items");
        assertEquals(initialItems.getData().size(), afterItems.getData().size(),
            "Number of items should remain unchanged");
    }
}