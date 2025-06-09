package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Message;
import java.util.List;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.UserDTO;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;

import java.util.Set;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;

import java.util.HashSet;
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
        fixtures.generateShopAndItems(ownerToken,"MyShop");
        
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
        fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        // 1) Owner creates a shop with items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");
        
        String guestToken = userService.enterToSystem().getData();
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shopDto.getId()).getData();

        orderService.addItemToCart(guestToken, shopDto.getId(), items.get(0).getItemID(), 1);
        
        
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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");
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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

        Response<Void> removeResp = shopService.removeItemFromShop(
            ownerToken, shopDto.getId(), 456 // Non-existent item ID
        );
        assertFalse(removeResp.isOk(), "removeItemFromShop should fail as the item does not exist");
    }

    @Test
    public void testEditItemDescription_AsOwner_ShouldSucceed()
    {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shopDto = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        Response<List<Permission>> permsResp = shopService.getMemberPermissions(ownerToken, shop.getId(),"manager");
        assertTrue(permsResp.isOk(), "getMembersPermissions should succeed");
        

        assertTrue(permsResp.getData().contains(Permission.APPOINTMENT), "Manager should have APPOINTMENT permission");
        
    }

    @Test
    public void testSetManagerPermissions_AddNewPermission_ShouldSucceed() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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

        Response<List<Permission>> res = shopService.getMemberPermissions(ownerToken, shop.getId(),"manager");
        
        Response<Void> setPermissionsResp = shopService.addShopManagerPermission
        (
            ownerToken, shop.getId(), "manager", Permission.UPDATE_ITEM_PRICE
        );
        assertTrue(setPermissionsResp.isOk(), "setPermissions should succeed");
        assertNotEquals(res.getData(), shopService.getMemberPermissions(ownerToken, shop.getId(),"manager").getData());
        
    }

    @Test
    public void testRemoveManager_AsOwner_ShouldPreventManagerActions() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // 2) Manager setup
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgr2", "pwdM2", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgr2", "pwdM2").getData();

        // 3) Owner assigns manager WITHOUT VIEW permission
        Set<Permission> perms = new HashSet<>();  // empty set
        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgr2", perms);
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 4) Manager tries to view
        Response<List<Permission>> resp = shopService.getMemberPermissions(mgrToken, shop.getId(), "mgr2");
        assertFalse(resp.isOk(), "Should fail when lacking VIEW permission");
    }

    @Test
    public void testEditItemQuantity_ManagerWithPermission_ShouldSucceed() {
        // 1) Owner creates shop + items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        Response<Void> resp = shopService.updatePurchaseType(badToken, 1, PurchaseType.BID);
        assertFalse(resp.isOk(), "Should fail when not logged in");
    }

    @Test
    public void testUpdatePurchasePolicy_WithoutPermission_ShouldFail() {
        // 1) Owner + shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // 2) Manager without that permission
        String mgrGuest = userService.enterToSystem().getData();
        userService.registerUser(mgrGuest, "mgrNoP", "pwdX", LocalDate.now().minusYears(30));
        String mgrToken = userService.loginUser(mgrGuest, "mgrNoP", "pwdX").getData();

        Response<Void> addMgr = shopService.addShopManager(ownerToken, shop.getId(), "mgrNoP", new HashSet<>());
        assertTrue(addMgr.isOk(), "addShopManager should succeed");

        // 3) Attempt to edit
        Response<Void> resp = shopService.updatePurchaseType(mgrToken, shop.getId(), PurchaseType.BID);
        assertFalse(resp.isOk(), "Should fail when lacking permission");
    }


    @Test
    public void testCloseShop_ShouldSucceed() {
        // 1) System (owner) creates and opens a shop
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
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
            ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop"+i);
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
        assertTrue(assignResp.isOk(), "assignShopManager should succeed");

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
    public void testCloseShop_AsNonOwner_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerClose", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "CloseShop");
        String user = fixtures.generateRegisteredUserSession("userClose", "pwdU");
        Response<Void> resp = shopService.closeShop(user, shop.getId());
        assertFalse(resp.isOk(), "Non-owner should not be able to close the shop");
    }

    @Test
    public void testOpenAuction_AsNonOwner_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerAuction", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "AuctionShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();
        String userToken = fixtures.generateRegisteredUserSession("otherAuction", "pwdU");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = LocalDateTime.now().plusSeconds(10);
        Response<Void> resp = shopService.openAuction(userToken, shopId, itemId, 5.0, start, end);
        assertFalse(resp.isOk(), "Non-owner should not be able to open auction");
    }

    @Test
    public void testChangeItemName_AsOwner_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerCN", "pwdCN");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "NameShop");
        int shopId = shop.getId();
        ItemDTO first = shop.getItems().get(0);
        int itemId = first.getItemID();
        String originalName = first.getName();

        Response<Void> changeResp = shopService.changeItemName(ownerToken, shopId, itemId, "NewName");
        assertTrue(changeResp.isOk(), "changeItemName should succeed for owner");

        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken, shopId);
        assertTrue(itemsResp.isOk(), "showShopItems should succeed after change");
        assertTrue(itemsResp.getData().stream()
            .anyMatch(i -> i.getItemID() == itemId && "NewName".equals(i.getName())),
            "Item name should be updated to NewName");
    }

    @Test
    public void testChangeItemName_AsNonOwner_ShouldFailAndKeepOriginal() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerCN2", "pwdCN2");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "NameShop2");
        int shopId = shop.getId();
        ItemDTO first = shop.getItems().get(0);
        int itemId = first.getItemID();
        String originalName = first.getName();
        String otherToken = fixtures.generateRegisteredUserSession("userCN", "pwdU");

        Response<Void> changeResp = shopService.changeItemName(otherToken, shopId, itemId, "ShouldNot");
        assertFalse(changeResp.isOk(), "changeItemName should fail for non-owner");

        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(ownerToken, shopId);
        assertTrue(itemsResp.isOk(), "showShopItems should still succeed");
        assertTrue(itemsResp.getData().stream()
            .anyMatch(i -> i.getItemID() == itemId && originalName.equals(i.getName())),
            "Original item name should remain unchanged");
    }

    @Test
    public void testRemoveShopManagerPermission_AsOwner_ShouldRemovePermission() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerRP", "pwdRP");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PermShop");
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrPerm", "pwdM", LocalDate.now().minusYears(25));
        String managerToken = userService.loginUser(guest, "mgrPerm", "pwdM").getData();
        Set<Permission> perms = new HashSet<>(Set.of(Permission.VIEW, Permission.UPDATE_ITEM_PRICE));
        shopService.addShopManager(ownerToken, shop.getId(), "mgrPerm", perms);
        Response<List<Permission>> before = shopService.getMemberPermissions(ownerToken, shop.getId(), "mgrPerm");
        assertTrue(before.isOk());
        assertTrue(before.getData().contains(Permission.UPDATE_ITEM_PRICE));

        Response<Void> removePermResp = shopService.removeShopManagerPermission(ownerToken, shop.getId(), "mgrPerm", Permission.UPDATE_ITEM_PRICE);
        assertTrue(removePermResp.isOk(), "removeShopManagerPermission should succeed for owner");

        Response<List<Permission>> after = shopService.getMemberPermissions(ownerToken, shop.getId(), "mgrPerm");
        assertTrue(after.isOk());
        assertFalse(after.getData().contains(Permission.UPDATE_ITEM_PRICE), "Permission should be removed");
        assertTrue(after.getData().contains(Permission.VIEW), "Other permissions should remain");
    }

    @Test
    public void testRemoveShopManagerPermission_AsNonOwner_ShouldFailAndKeepPermissions() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerRP2", "pwdRP2");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PermShop2");
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrPerm2", "pwdM2", LocalDate.now().minusYears(25));
        String managerToken = userService.loginUser(guest, "mgrPerm2", "pwdM2").getData();
        Set<Permission> perms = new HashSet<>(Set.of(Permission.VIEW, Permission.UPDATE_ITEM_PRICE));
        shopService.addShopManager(ownerToken, shop.getId(), "mgrPerm2", perms);
        Response<List<Permission>> before = shopService.getMemberPermissions(ownerToken, shop.getId(), "mgrPerm2");
        assertTrue(before.isOk());
        assertTrue(before.getData().contains(Permission.UPDATE_ITEM_PRICE));

        String otherToken = fixtures.generateRegisteredUserSession("userRP", "pwdRP");
        Response<Void> removePermResp = shopService.removeShopManagerPermission(otherToken, shop.getId(), "mgrPerm2", Permission.UPDATE_ITEM_PRICE);
        assertFalse(removePermResp.isOk(), "removeShopManagerPermission should fail for non-owner");

        Response<List<Permission>> after = shopService.getMemberPermissions(ownerToken, shop.getId(), "mgrPerm2");
        assertTrue(after.isOk());
        assertTrue(after.getData().contains(Permission.UPDATE_ITEM_PRICE), "Permissions should remain unchanged");
    }

    // — addShopOwner happy & sad paths —
    @Test
    public void testAddShopOwner_AsOwner_ShouldSucceedAndPersistInvariant() {
        String founder = fixtures.generateRegisteredUserSession("founderA","pwdA");
        ShopDTO shop = fixtures.generateShopAndItems(founder,"OwnerShop");
        // register a co‐founder
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest,"cofounder","pwdC",LocalDate.now().minusYears(30));
        String coToken = userService.loginUser(guest,"cofounder","pwdC").getData();

        Response<Void> r = shopService.addShopOwner(founder, shop.getId(), "cofounder");
        assertTrue(r.isOk(), "Owner should be able to appoint another owner");

        // Invariant: showUserShops for cofounder includes this shop
        Response<List<ShopDTO>> list = shopService.showUserShops(coToken);
        assertTrue(list.isOk());
        assertTrue(list.getData().stream().anyMatch(s -> s.getId()==shop.getId()));
    }

    @Test
    public void testAddShopOwner_AsNonOwner_ShouldFailAndKeepInvariant() {
        String founder = fixtures.generateRegisteredUserSession("founderB","pwdB");
        ShopDTO shop = fixtures.generateShopAndItems(founder,"OwnerShop2");
        String intruder = fixtures.generateRegisteredUserSession("intruder","pwdI");
        Response<Void> r = shopService.addShopOwner(intruder, shop.getId(), "someone");
        assertFalse(r.isOk(), "Non-owner must not appoint new owners");

        // Invariant: showUserShops for “someone” yields empty
        Response<List<ShopDTO>> list = shopService.showUserShops(intruder);
        assertTrue(list.isOk());
        assertTrue(list.getData().isEmpty());
    }

    @Test
    public void testGetFutureAuctions_ShouldListUpcomingAuctions() {
        String owner = fixtures.generateRegisteredUserSession("ownerFA","pwdFA");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"FutureShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();
        // schedule auction tomorrow
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end   = LocalDateTime.now().plusDays(1).plusSeconds(2);
        assertTrue(shopService.openAuction(owner, shopId, itemId, 5.0, start, end).isOk());

        Response<List<AuctionDTO>> fut = shopService.getFutureAuctions(owner, shopId);
        assertTrue(fut.isOk());
        assertTrue(fut.getData().stream().anyMatch(a -> a.getItemId()==itemId));
    }

    // — getPurchaseConditions & getPurchaseTypes —
    @Test
    public void testGetPurchaseConditions_ShouldReturnDefaultConditions() {
        String owner = fixtures.generateRegisteredUserSession("ownerPC","pwdPC");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"CondShop");
        Response<List<ConditionDTO>> resp = shopService.getPurchaseConditions(owner, shop.getId());
        assertTrue(resp.isOk(), "Should fetch purchase conditions");
        assertTrue(resp.getData().isEmpty(), "There should be 0 conditions by default");
    }

    // — getWonAuctions & getUserBids —
    @Test
    public void testGetUserBids_ShouldIncludeMyBid() {
        String owner = fixtures.generateRegisteredUserSession("ownerUB","pwdUB");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"BidShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();

        String bidder = fixtures.generateRegisteredUserSession("bidderUB","pwdB");
        orderService.submitBidOffer(bidder, shopId, itemId, 7.0);

        // Response<List<BidDTO>> bids = shopService.getUserBids(bidder, shopId, );
        // assertTrue(bids.isOk());
        // assertTrue(bids.getData().stream().anyMatch(b -> b.getItemId()==itemId && b.getAmount()==7.0));
    }

    @Test
    public void testGetWonAuctions_ShouldBeEmptyBeforeEnd() {
        String owner = fixtures.generateRegisteredUserSession("ownerWA","pwdWA");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"WinShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();

        String bidder = fixtures.generateRegisteredUserSession("bidderWA","pwdW");
        orderService.submitBidOffer(bidder, shopId, itemId, 8.0);

        // auction still open—no wins yet
        Response<List<AuctionDTO>> won = shopService.getWonAuctions(bidder, shopId);
        assertTrue(won.isOk());
        assertTrue(won.getData().isEmpty(), "Should have no won auctions before close");
    }
    // — getPurchaseTypes —
    @Test
    public void testGetPurchaseTypes_ShouldReturnAllTypes() {
        String owner = fixtures.generateRegisteredUserSession("ownerPT","pwdPT");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"TypeShop");
        Response<List<PurchaseType>> resp = shopService.getPurchaseTypes(owner, shop.getId());
        assertTrue(resp.isOk(), "Should fetch purchase types");
        List<PurchaseType> types = resp.getData();
        assertTrue(types.contains(PurchaseType.IMMEDIATE), "IMMEDIATE type must be present");
        assertTrue(types.contains(PurchaseType.BID),     "BID type must be present");
    }

    // // — getActiveAuctions —
    // @Test
    // public void testGetActiveAuctions_ShouldListRunningAuctions() throws InterruptedException {
    //     String owner = fixtures.generateRegisteredUserSession("ownerAA","pwdAA");
    //     ShopDTO shop = fixtures.generateShopAndItems(owner,"AucShop");
    //     int shopId = shop.getId();
    //     int itemId = shop.getItems().values().iterator().next().getItemID();
    //     LocalDateTime start = LocalDateTime.now();
    //     LocalDateTime end   = LocalDateTime.now().plusSeconds(2);
    //     assertTrue(shopService.openAuction(owner, shopId, itemId, 5.0, start, end).isOk());

    //     // fetch active auctions immediately
    //     Response<List<AuctionDTO>> resp = shopService.getActiveAuctions(owner, shop.getId());
    //     assertTrue(resp.isOk(), "getActiveAuctions should succeed");
    //     assertTrue(resp.getData().stream().anyMatch(a -> a.getItemId() == itemId),
    //                "Running auction should appear in active list");
    // }

    // — getDiscounts & getDiscountTypes —
    @Test
    public void testGetDiscounts_ShouldReturnDefaultDiscounts() {
        String owner = fixtures.generateRegisteredUserSession("ownerDisc","pwdDisc");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"DiscShop");
        Response<List<ConditionDTO>> conditions = shopService.getPurchaseConditions(owner, shop.getId()); // ensure domain ready
        Response<List<DiscountDTO>> resp = shopService.getDiscounts(owner, shop.getId());
        assertTrue(resp.isOk(), "Should fetch discount rules");
        // By default no discounts applied
        assertTrue(resp.getData().isEmpty(), "Default discount list should be empty");
    }

    @Test
    public void testGetDiscountTypes_ShouldReturnAllTypes() {
        String owner = fixtures.generateRegisteredUserSession("ownerDT","pwdDT");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"DTShop");
        Response<List<DiscountType>> resp = shopService.getDiscountTypes(owner, shop.getId());
        assertTrue(resp.isOk(), "Should fetch discount types");
        List<DiscountType> types = resp.getData();
        assertTrue(types.contains(DiscountType.BASE), "PERCENTAGE type must be present");
        assertTrue(types.contains(DiscountType.CONDITIONAL),      "FIXED type must be present");
    }

    // — showUserShops —
    @Test
    public void testShowUserShops_AsOwner_ShouldReturnOwnShop() {
        String owner = fixtures.generateRegisteredUserSession("ownerSU","pwdSU");
        ShopDTO shop = fixtures.generateShopAndItems(owner,"UserShop");
        Response<List<ShopDTO>> resp = shopService.showUserShops(owner);
        assertTrue(resp.isOk(), "showUserShops should succeed for owner");
        assertTrue(resp.getData().stream().anyMatch(s -> s.getId() == shop.getId()),
                   "Owner's shop must appear in their shop list");
    }

    // — getShopId —
    @Test
    public void testGetShopId_ShouldReturnCorrectId() {
        String owner = fixtures.generateRegisteredUserSession("ownerG", "pwdG");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "IdShop");

        Response<Integer> resp = shopService.getShopId(owner, "IdShop");
        assertTrue(resp.isOk(), "getShopId should succeed for existing shop");
        assertEquals(shop.getId(), resp.getData(),
                        "Returned ID should match created shop ID");
    }

    @Test
    public void testGetShopId_InvalidName_ShouldFail() {
        String owner = fixtures.generateRegisteredUserSession("ownerG2", "pwdG2");
        fixtures.generateShopAndItems(owner, "IdShop2");

        Response<Integer> resp = shopService.getShopId(owner, "UnknownShop");
        assertFalse(resp.isOk(), "getShopId should fail for non-existent shop name");
    }
    
    // — getBids —
    @Test
    public void testGetBids_NoBids_ShouldReturnEmptyList() {
        String owner = fixtures.generateRegisteredUserSession("ownerNoBids", "pwdNB");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "BidShopNB");
        int shopId = shop.getId();

        Response<List<BidDTO>> resp = shopService.getBids(owner, shopId);
        assertTrue(resp.isOk(), "getBids should succeed when no bids placed");
        assertTrue(resp.getData().isEmpty(), "Bid list should be empty initially");
    }

    @Test
    public void testGetBids_AfterSubmittingBid_ShouldIncludeBid() {
        String owner = fixtures.generateRegisteredUserSession("ownerBids", "pwdOB");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "BidShopB");
        int shopId = shop.getId();
        int itemId = shop.getItems().get(0).getItemID();

        String bidder = fixtures.generateRegisteredUserSession("bidderB", "pwdBB");
        assertTrue(orderService.submitBidOffer(bidder, shopId, itemId, 9.5).isOk(),
                    "submitBidOffer should succeed");

        Response<List<BidDTO>> resp = shopService.getBids(owner, shopId);
        assertTrue(resp.isOk(), "getBids should succeed after bid");
        assertTrue(resp.getData().stream()
            .anyMatch(b -> b.getItemId() == itemId && b.getAmount() == 9.5),
            "Bid list should contain the submitted bid");
    }

    // — messaging tests —
    @Test
    public void testGetInbox_EmptyAtStart_ShouldReturnEmptyList() {
        String owner = fixtures.generateRegisteredUserSession("ownerMsg", "pwdMsg");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "MsgShop");
        String user = fixtures.generateRegisteredUserSession("userMsg", "pwdU");

        Response<List<Message>> inbox = shopService.getInbox(user, shop.getId());
        assertTrue(inbox.isOk(), "getInbox should succeed");
        assertTrue(inbox.getData().isEmpty(), "Inbox should be empty initially");
    }

    @Test
    public void testSendMessageAndGetInbox_ShouldDeliverMessage() {
        String owner = fixtures.generateRegisteredUserSession("ownerMsg2", "pwd2");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "MsgShop2");
        String guest = userService.enterToSystem().getData();
        userService.registerUser(guest, "mgrMsg", "pwdM", LocalDate.now().minusYears(25));
        String manager = userService.loginUser(guest, "mgrMsg", "pwdM").getData();

        Response<Void> send = shopService.sendMessage(owner, shop.getId(), "Hello mgrMsg", "Hello there");
        assertTrue(send.isOk(), "sendMessage should succeed");

        Response<List<Message>> inbox = shopService.getInbox(manager, shop.getId());
        assertTrue(inbox.isOk(), "getInbox should succeed");
        List<Message> msgs = inbox.getData();
        assertEquals(1, msgs.size(), "Should receive one message");
        assertEquals("Hello there", msgs.get(0).getContent());
        assertEquals("ownerMsg2", msgs.get(0).getUserName());
    }

    // @Test
    // public void testRespondToMessage_ShouldSendReply() {
    //     String owner = fixtures.generateRegisteredUserSession("ownerMsg3", "pwd3");
    //     ShopDTO shop = fixtures.generateShopAndItems(owner, "MsgShop3");
    //     String guest = userService.enterToSystem().getData();
    //     userService.registerUser(guest, "mgrMsg2", "pwdM2", LocalDate.now().minusYears(25));
    //     String manager = userService.loginUser(guest, "mgrMsg2", "pwdM2").getData();

    //     // Owner sends a message to manager
    //     Response<Void> send = shopService.sendMessage(owner, shop.getId(), "mgrMsg2", "Question?");
    //     assertTrue(send.isOk(), "sendMessage should succeed");

    //     // Manager retrieves inbox and grabs the message ID
    //     Response<List<Message>> mgrInbox = shopService.getInbox(manager, shop.getId());
    //     assertTrue(mgrInbox.isOk(), "getInbox should succeed for manager");
    //     assertFalse(mgrInbox.getData().isEmpty(), "Manager inbox should contain the message");
    //     Message original = mgrInbox.getData().get(0);
    //     int msgId = original.getId(); // or getMessageID()

    //     // Manager replies using msgId, title, and content
    //     Response<Void> reply = shopService.respondToMessage(manager, shop.getId(), msgId, "Re:Question?", "Reply!");
    //     assertTrue(reply.isOk(), "respondToMessage should succeed");

    //     // Owner retrieves inbox and sees the reply
    //     Response<List<Message>> ownerInbox = shopService.getInbox(owner, shop.getId());
    //     assertTrue(ownerInbox.isOk(), "getInbox for owner should succeed");
    //     assertTrue(ownerInbox.getData().stream()
    //         .anyMatch(m -> "Reply!".equals(m.getContent()) && "mgrMsg2".equals(m.getUserName())),
    //         "Owner should receive the reply message");
    // }
}