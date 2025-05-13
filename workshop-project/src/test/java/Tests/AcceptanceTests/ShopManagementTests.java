package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;


import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.*;

import java.util.HashSet;
import java.util.Set;
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
        fixtures.generateShopAndItems(ownerToken);
        
        // Act
        Response<List<ShopDTO>> shops = shopService.showAllShops(ownerToken);
        
        // Assert
        assertNotNull(shops.getData(), "showAllShops should not return null");
        assertEquals(1, shops.getData().size(), "Should return exactly one shop");
        assertEquals(3, shops.getData().get(0).getItems().size(), "Shop should contain 3 items");
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
        
        // Assert
        assertTrue(filteredResp.isOk(), "filterItemsAllShops should succeed");

        List<ItemDTO> result = filteredResp.getData();
        assertNotNull(result, "Filtered list must not be null");

        // Verify that only "Apple" remains
        assertEquals(1, result.size(), "Exactly one item should survive all filters");
        assertEquals("Apple", result.get(0).getName(), "That one item should be Apple");
    }

    @Test
    public void testSearchItemsWithoutFilters_ShouldReturnAllItems() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        fixtures.generateShopAndItems(ownerToken);

        // Guest enters the system
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest enterToSystem should succeed");
        //String guestToken = guestResp.getData();

        // Act - Search without any filters
        HashMap<String,String> emptyFilters = new HashMap<>();
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,emptyFilters);
        
        // Assert
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed");
        List<ItemDTO> items = searchResp.getData();
        assertEquals(3, items.size(), "Should return all 3 available items");
    }

    @Test
    public void testSearchItemsWithNoMatches_ShouldReturnEmptyList() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        fixtures.generateShopAndItems(ownerToken);

        // Guest enters
        userService.enterToSystem();

        // Act - Search with a name that matches nothing
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name", "NoSuchItem");
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,filters);
        
        // Assert
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed even if empty");
        assertTrue(searchResp.getData().isEmpty(), "No items should be found");
    }

    @Test
    public void testSearchItemsInSpecificShop_ShouldReturnMatchingItems() {
        // Arrange - Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // Guest enters
        userService.enterToSystem();

        // Act - Search in that shop for bananas priced <=0.50
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name",     "Banana");
        filters.put("category", Category.FOOD.name());
        filters.put("minPrice","0");
        filters.put("maxPrice","0.50");

        Response<List<ItemDTO>> resp = shopService.filterItemsInShop(ownerToken,shop.getId(), filters);
        
        // Assert
        assertTrue(resp.isOk(), "filterItemsInShop should succeed");
        List<ItemDTO> results = resp.getData();
        assertEquals(1, results.size(), "Exactly one banana at price <= 0.50 should match");
        assertEquals("Banana", results.get(0).getName(), "Item should be a banana");
    }

    @Test
    public void testSearchItemsInNonExistentShop_ShouldFail() {
        // Arrange - Guest enters
        userService.enterToSystem();
        String guestToken = userService.enterToSystem().getData();

        // Act - Use a non-existent shop ID
        int missingShopId = 9999;
        HashMap<String,String> filters = new HashMap<>();
        Response<List<ItemDTO>> resp = shopService.filterItemsInShop(guestToken,missingShopId, filters);

        // Assert
        // Right now this blows up with a NullPointerException. You need to catch that
        // inside filterItemsInShop and return Response.error("Shop not found");
        assertFalse(resp.isOk(), "Search in non-existent shop should fail");
    }

    
    @Test
    public void testCreateShop_WithGuestToken_ShouldFail() {
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "enterToSystem should succeed");
        String guestToken = guestResp.getData();
        assertNotNull(guestToken, "Guest token must not be null");
        
        Response<ShopDTO> createShopResp = shopService.createShop(guestResp.getData(), "MyShop", "desc");
        assertFalse(createShopResp.isOk(), "Shop creation should fail for guest user");
    }

    @Test
    public void testRateShop_WhenNotLoggedIn_ShouldFail() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1","12/25", "123"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        // 1) Owner creates a shop with items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);
        
        String guestToken = userService.enterToSystem().getData();
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shopDto.getId()).getData();
        HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
        HashMap<Integer, Integer> itemMap = new HashMap<>();
        itemMap.put(items.get(0).getItemID(), 1);
        itemMap.put(items.get(1).getItemID(), 1);
        itemsMap.put(shopDto.getId(), itemMap);

        orderService.addItemsToCart(guestToken, itemsMap);

        fixtures.successfulBuyCartContent(guestToken, p, s);
        
        Response<Void> res = shopService.rateShop(guestToken, shopDto.getId(), 5);
        assertFalse(res.isOk(), "Rate shop should fail when not logged in");
    }

    @Test
    public void testAddItemToShop_AsOwner_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");

        Response<ShopDTO> shopResp = shopService.createShop(
            ownerToken, 
            "MyShop", 
            "A shop for tests"
        );
        assertTrue(shopResp.isOk(), "Shop creation should succeed");
        ShopDTO shop = shopResp.getData();
        assertNotNull(shop, "Returned ShopDTO must not be null");
        int shopId = shop.getId();

        // 2) Owner adds item
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shopId,
            "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        Response<ShopDTO> infoResp = shopService.getShopInfo(ownerToken, shopId);
        assertTrue(infoResp.isOk(), "getShopInfo should succeed");

        assertTrue(addA.isOk(), "Adding Apple should succeed");
        assertEquals("Apple", addA.getData().getName(), "Item name should be 'Apple'");
        assertEquals(Category.FOOD, addA.getData().getCategory(), "Item category should be FOOD");
        assertEquals(1.00, addA.getData().getPrice(), "Item price should be 1.00");
        assertEquals("fresh apple", addA.getData().getDescription());
        assertEquals(1, infoResp.getData().getItems().size(), "Shop should contain exactly one item after adding Apple");
    }
    
    @Test
    public void testAddItemToShop_WithNonExistentShop_ShouldFail() 
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");

        // 2) Owner adds three items
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, 0,
            "Apple", Category.FOOD, 1.00, "fresh apple"
        );

        assertFalse(addA.isOk(), "Adding Apple should fail");
    }

    @Test
    public void testAddItemToShop_AsNonOwner_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");
        Response<ItemDTO> addA = shopService.addItemToShop(
            userToken, shopDto.getId(),
            "Apple", Category.FOOD, 1.00, "fresh apple"
        );
        assertFalse(addA.isOk(), "Adding Apple should fail as the user is not the owner");
    }

    @Test
    public void testAddItemToShop_WithDuplicateItem_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shopDto.getId(),
            "Apple", Category.FOOD, 1.00, "fresh apple"
        );
        assertFalse(addA.isOk(), "Adding duplicate item should fail");
    }

    @Test
    public void testRemoveItemFromShop_AsOwner_ShouldSucceed()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);
        assertEquals(3, shopDto.getItems().size(), "Shop should contain exactly three items after removal");

        // 2) Owner removes an item
        Response<Void> removeResp = shopService.removeItemFromShop(
            ownerToken, shopDto.getId(), shopDto.getItems().get(0).getItemID()
        );
        assertTrue(removeResp.isOk(), "removeItemFromShop should succeed");

        // 3) Verify the item is removed
        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken,shopDto.getId());
        assertTrue(itemsResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> items = itemsResp.getData();
        assertEquals(2, items.size(), "Shop should contain exactly two items after removal");
    }

    @Test
    public void testRemoveItemFromShop_AsNonOwner_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");
        Response<Void> removeResp = shopService.removeItemFromShop(
            userToken, shopDto.getId(), shopDto.getItems().get(0).getItemID()
        );
        assertFalse(removeResp.isOk(), "removeItemFromShop should fail as the user is not the owner");
    }

    @Test
    public void testRemoveItemFromShop_WithNonExistentItem_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        Response<Void> removeResp = shopService.removeItemFromShop(
            ownerToken, shopDto.getId(), 456 // Non-existent item ID
        );
        assertFalse(removeResp.isOk(), "removeItemFromShop should fail as the item does not exist");
    }

    @Test
    public void testEditItemDescription_AsOwner_ShouldSucceed()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        // 2) Owner edits an item
        ItemDTO itemToEdit = shopDto.getItems().get(0);
        Response<Void> editResp = shopService.changeItemDescriptionInShop(
            ownerToken, shopDto.getId(), itemToEdit.getItemID(), "New description"
        );
        assertTrue(editResp.isOk(), "editItemInShop should succeed");

        // 3) Verify the item is edited
        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken,shopDto.getId());
        assertTrue(itemsResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> items = itemsResp.getData();
        assertEquals(3, items.size(), "Shop should contain exactly three items after editing");
        assertEquals("New description", items.get(0).getDescription(), "Item description should be updated");
    }

    @Test
    public void testEditItemDescription_AsNonOwner_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        String userToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd0");

        // 2) Owner edits an item
        ItemDTO itemToEdit = shopDto.getItems().get(0);
        Response<Void> editResp = shopService.changeItemDescriptionInShop(
            userToken, shopDto.getId(), itemToEdit.getItemID(), "New description"
        );
        assertFalse(editResp.isOk(), "editItemInShop should fail as the user is not the owner");
    }

    @Test
    public void testEditItemPrice_WithNegativePrice_ShouldFail()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken);

        // 2) Owner edits an item
        ItemDTO itemToEdit = shopDto.getItems().get(0);
        Response<Void> editResp = shopService.changeItemPriceInShop(
            ownerToken, shopDto.getId(), itemToEdit.getItemID(), -100.00
        );
        assertFalse(editResp.isOk(), "editItemInShop should succeed");

        // 3) Verify the item is edited
        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken,shopDto.getId());
        assertTrue(itemsResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> items = itemsResp.getData();
        assertEquals(3, items.size(), "Shop should contain exactly three items after editing");
    }

    
    @Test
    public void testAddShopManager_WithValidPermissions_ShouldSucceed() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner1", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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
        //String managerToken = managerLoginResp.getData(); // after login
        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");
        

        // 4) Now verify that manager was actually added to the shop
        Response<String> permsResp = shopService.getMembersPermissions(ownerToken, shop.getId());
        assertTrue(permsResp.isOk(), "getMembersPermissions should succeed");
        String permsData = permsResp.getData();

        assertTrue(permsData.contains(Permission.APPOINTMENT.name()));
        
    }

    @Test
    public void testSetManagerPermissions_AddNewPermission_ShouldSucceed() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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
        //String managerToken = managerLoginResp.getData(); // after login

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        Response<String> res = shopService.getMembersPermissions(ownerToken, shop.getId());
        
        Response<Void> setPermissionsResp = shopService.addShopManagerPermission
        (
            ownerToken, shop.getId(), "manager", Permission.UPDATE_ITEM_PRICE
        );
        assertTrue(setPermissionsResp.isOk(), "setPermissions should succeed");
        assertNotEquals(res.getData(), shopService.getMembersPermissions(ownerToken, shop.getId()).getData());
        
    }

    @Test
    public void testRemoveManager_AsOwner_ShouldPreventManagerActions() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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
        String managerToken = managerLoginResp.getData(); // after login

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();
        
        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // 4) Owner removes the manager
        Response<Void> removeManagerResp = shopService.removeAppointment(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(removeManagerResp.isOk(), "removeShopManager should succeed");
        
        //String userToken = fixtures.generateRegisteredUserSession("User", "Pwd0");
        Response<Void> res = shopService.addShopManager(managerToken, shop.getId(), "User", permissions);
        assertFalse(res.isOk(), "addShopManager should fail as the manager was removed");
    }

    @Test
    public void testRemoveAppointee_WithNestedAppointees_ShouldRemoveAll() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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
        String managerToken = managerLoginResp.getData(); // after login

        // 3) Owner adds the manager
        Set<Permission> permissions = new HashSet<>();

        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd0");

        // 3) Owner adds the manager
        Set<Permission> userpermissions = new HashSet<>();

        userpermissions.add(Permission.APPOINTMENT);
        Response<Void> addManager2Resp = shopService.addShopManager(
            managerToken, shop.getId(), "User", userpermissions
        );
        assertTrue(addManager2Resp.isOk(), "addShopManager should succeed");
        
        fixtures.generateRegisteredUserSession("User2", "Pwd0");
        Response<Void> res = shopService.addShopManager(userToken, shop.getId(), "User2", permissions);
        assertTrue(res.isOk(), "addShopManager should fail as the manager was removed");

        // 4) Owner removes the manager
        Response<Void> removeManager2Resp = shopService.removeAppointment(
            ownerToken, shop.getId(), "manager"
        );
        assertTrue(removeManager2Resp.isOk(), "removeShopManager should succeed");

        //String user3Token = fixtures.generateRegisteredUserSession("User3", "Pwd0");
        Response<Void> res3 = shopService.addShopManager(userToken, shop.getId(), "User3", permissions);
        assertFalse(res3.isOk(), "addShopManager should fail as the manager was removed");
    }

    @Test
    public void testAppoint_SameUserTwice_ShouldFail() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Owner adds the manager
        String managerToken = fixtures.generateRegisteredUserSession("Manager", "PwdM");
        
        Set<Permission> permissions = new HashSet<>();

        permissions.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp = shopService.addShopManager(
            ownerToken, shop.getId(), "Manager", permissions
        );
        assertTrue(addManagerResp.isOk(), "addShopManager should succeed");

        // 3) Owner adds the manager
        fixtures.generateRegisteredUserSession("Manager2", "PwdM");
        
        Set<Permission> permissions2 = new HashSet<>();

        permissions2.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp2 = shopService.addShopManager(
            ownerToken, shop.getId(), "Manager2", permissions2
        );
        assertTrue(addManagerResp2.isOk(), "addShopManager should succeed");

        // 4) Manager tries adding manager2        
        Set<Permission> permissions3 = new HashSet<>();

        permissions3.add(Permission.APPOINTMENT);
        Response<Void> addManagerResp3 = shopService.addShopManager(
            managerToken, shop.getId(), "Manager2", permissions3
        );
        assertFalse(addManagerResp3.isOk(), "addShopManager should succeed");
    }

    @Test
    public void testViewShopContent_ManagerWithViewPermission_ShouldSucceed() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

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

        assertEquals(shop.getId(),   seen.getId(),   "Shop ID must match");
        assertEquals(shop.getName(), seen.getName(), "Shop name must match");
    }

    @Test
    public void testViewShop_WithInvalidToken_ShouldFail() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) User never logged in → use an invalid token
        String badToken = "not-a-valid-token";

        // 3) Attempt to view
        Response<ShopDTO> resp = shopService.getShopInfo(badToken, shop.getId());
        assertFalse(resp.isOk(), "Should fail when not logged in");
    }

    
    @Test
    public void testGetMemberPermissions_WithoutViewPermission_ShouldFail() {
        // 1) Owner creates shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Manager setup
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgr2", "pwdM2", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgr2", "pwdM2").getData();

        // 3) Owner assigns manager WITHOUT VIEW permission
        Set<Permission> perms = new HashSet<>();  // empty set
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr2", perms);
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager tries to view
        Response<String> resp = shopService.getMembersPermissions(mgrToken, shop.getId());
        assertFalse(resp.isOk(), "Should fail when lacking VIEW permission");
    }

    @Test
    public void testEditItemQuantity_ManagerWithPermission_ShouldSucceed() {
        // 1) Owner creates shop + items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Manager setup
        String mgrGuestToken = userService.enterToSystem().getData();
        userService.registerUser(mgrGuestToken, "mgr", "pwdM", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuestToken, "mgr", "pwdM").getData();

        // 3) Owner assigns manager + gives INVENTORY permission
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");
        Response<Void> givePerm = shopService.addShopManagerPermission(
            ownerToken, shop.getId(), "mgr", Permission.UPDATE_ITEM_QUANTITY
        );
        assertTrue(givePerm.isOk(), "addShopManagerPermission should succeed");

        // 4) Manager edits the quantity of an existing item
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO first = items.get(0);
        int newQty = first.getQuantity() + 5;
        Response<Void> editResp = shopService.changeItemQuantityInShop(
            mgrToken, shop.getId(), first.getItemID(), newQty
        );
        assertTrue(editResp.isOk(), "changeItemQuantityInShop should succeed");

        // 5) Verify via service
        List<ItemDTO> updated = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO updatedFirst = updated.stream()
            .filter(i -> i.getItemID() == first.getItemID())
            .findFirst()
            .orElseThrow();
        assertEquals(newQty, updatedFirst.getQuantity(), "Quantity should be updated");
    }

    @Test
    public void testEditItemQuantity_WithInvalidToken_ShouldFail() {
        // 1) Attempt to edit without logging in
        String invalidToken = "not-a-token";
        Response<Void> resp = shopService.changeItemQuantityInShop(
            invalidToken, 1, 42, 10
        );
        assertFalse(resp.isOk(), "Should fail if not logged in");
    }

    @Test
    public void testEditItemQuantity_WithNonExistentItem_ShouldFail() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Owner tries to edit a non-existent product
        int missingId = 9999;
        Response<Void> resp = shopService.changeItemQuantityInShop(
            ownerToken, shop.getId(), missingId, 10
        );
        assertFalse(resp.isOk(), "Should fail when product does not exist");
    }

    @Test
    public void testEditItemQuantity_WithoutPermission_ShouldFail() {
        // 1) Owner + shop + items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Manager setup (no inventory permission)
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrNoInv", "pwdI", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrNoInv", "pwdI").getData();

        // 3) Owner assigns the user as manager—but with an empty permission set
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgrNoInv", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager attempts to edit the first product’s quantity
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shop.getId()).getData();
        int itemId = items.get(0).getItemID();
        int newQty = items.get(0).getQuantity() + 5;

        Response<Void> resp = shopService.changeItemQuantityInShop(
            mgrToken, shop.getId(), itemId, newQty
        );

        // 5) Expect a permission-denied error
        assertFalse(resp.isOk(), "Should fail when lacking inventory permission");
    }

    @Test
    public void testUpdatePurchasePolicy_WithInvalidToken_ShouldFail() {
        // use an invalid token
        String badToken = "invalid";
        Response<Void> resp = shopService.updatePurchaseType(badToken, 1, "ANY_POLICY");
        assertFalse(resp.isOk(), "Should fail when not logged in");
    }

    @Test
    public void testUpdatePurchasePolicy_WithoutPermission_ShouldFail() {
        // 1) Owner + shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Manager without that permission
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrNoP", "pwdX", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrNoP", "pwdX").getData();

        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgrNoP", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 3) Attempt to edit
        Response<Void> resp = shopService.updatePurchaseType(mgrToken, shop.getId(), "ANY_POLICY");
        assertFalse(resp.isOk(), "Should fail when lacking permission");
    }

    @Test
    public void testPurchasePolicyFormat_InvalidFormat_ShouldFail() {
        // 1) Owner + shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Manager with permission
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrBadP", "pwdB", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrBadP", "pwdB").getData();

        Set<Permission> perms = new HashSet<>();
        perms.add(Permission.UPDATE_PURCHASE_POLICY);
        shopService.addShopManager(ownerToken, shop.getId(), "mgrBadP", perms);

        // 3) Submit malformed policy
        Response<Void> resp = shopService.updatePurchaseType(mgrToken, shop.getId(), "not a valid format");
        assertFalse(resp.isOk(), "Should fail on invalid format");
    }

    @Test
    public void testCloseShop_ShouldSucceed() {
        // 1) System (owner) creates and opens a shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);

        // 2) Owner closes it
        Response<Void> closeResp = shopService.closeShop(ownerToken, shop.getId());
        assertTrue(closeResp.isOk(), "closeShopByFounder should succeed");

        // 3) After closing, it must no longer show up among open shops
        List<ShopDTO> openShops = shopService.showAllShops(ownerToken).getData();
        assertFalse(openShops.stream().anyMatch(s -> s.getId() == shop.getId()),
                    "Closed shop must not appear in showAllShops()");
    }
    @Test
    public void testCloseShop_ClosedShop_ShouldFail() {
        // 1) Owner creates and closes a shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        assertTrue(shopService.closeShop(ownerToken, shop.getId()).isOk(),
                   "Initial close should succeed");

        // 2) Owner attempts to close it again
        Response<Void> secondClose = shopService.closeShop(ownerToken, shop.getId());
        assertFalse(secondClose.isOk(), "Closing an already closed shop should fail");
    }

    
    @Test
    public void testConcurrentManagerAppointment_SameCandidate_ShouldAllowOnlyOneSuccess() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            // 1) Owner creates shop
            String ownerToken = fixtures.generateRegisteredUserSession("owner"+i, "pwdO");
            ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
            int shopId = shop.getId();

            // 2) Prepare candidate user
            String candGuest = userService.enterToSystem().getData();
            assertTrue(userService.registerUser(candGuest, "candidate"+i, "pwdC", LocalDate.now().minusYears(25))
                    .isOk(), "Candidate registration should succeed");

            // 3) Two concurrent attempts to appoint the same candidate
            int iteration = i;  // effectively final
            Set<Permission> perms = Set.of(Permission.VIEW);
            List<Callable<Boolean>> tasks = List.of(
                () -> shopService.addShopManager(ownerToken, shopId, "candidate"+iteration, perms).isOk(),
                () -> shopService.addShopManager(ownerToken, shopId, "candidate"+iteration, perms).isOk()
            );

            ExecutorService ex = Executors.newFixedThreadPool(2);
            List<Future<Boolean>> results = ex.invokeAll(tasks);
            ex.shutdown();

            long successCount = results.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return false; }
                })
                .filter(ok -> ok)
                .count();

            assertEquals(1, successCount,
                "Exactly one of the two concurrent addShopManager calls should succeed, but got " + successCount);
        }
    }
}