package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Message;

public class ShoppingTests extends BaseAcceptanceTests {
    @Test
    public void getShopsAndItems() {
        //  1) Owner setup 
        // Owner enters as guest
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        fixtures.generateShopAndItems(ownerToken,"MyShop");;
        
        Response<List<ShopDTO>> shops = shopService.showAllShops(ownerToken);
        assertNotNull(shops.getData(), "showAllShops should not return null");
        assertEquals(1, shops.getData().size());
        assertEquals(3, shops.getData().get(0).getItems().size());
    }

    @Test
    public void searchItemsWithFilters() {
        // 1) Owner enters & registers
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "enterToSystem should succeed");
        String guestToken = guestResp.getData();

        Response<Void> regResp = userService.registerUser(
            guestToken, "owner", "pwdO", LocalDate.now().minusYears(30)
        );
        assertTrue(regResp.isOk(), "Owner registration should succeed");

        // 2) Owner logs in
        Response<String> loginResp = userService.loginUser(
            guestToken, "owner", "pwdO"
        );
        assertTrue(loginResp.isOk(), "Owner login should succeed");
        String ownerToken = loginResp.getData();

        // 3) Owner creates a shop
        Response<ShopDTO> createResp = shopService.createShop(
            ownerToken, "MyShop", "A shop for tests"
        );
        assertTrue(createResp.isOk(), "createShop should succeed");
        ShopDTO shop = createResp.getData();

        // 4) Owner adds three items
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

        // 5) (Optional) Retrieve them if you need IDs or to verify all three exist
        Response<List<ItemDTO>> allResp = shopService.showShopItems(ownerToken,shop.getId());
        assertTrue(allResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> allItems = allResp.getData();
        assertEquals(3, allItems.size(), "Shop should now contain 3 items");

        // 6) Build a composite filter:
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

        // 7) Call the service
        Response<List<ItemDTO>> filteredResp = shopService.filterItemsAllShops(ownerToken,filters);
        assertTrue(filteredResp.isOk(), "filterItemsAllShops should succeed");

        List<ItemDTO> result = filteredResp.getData();
        assertNotNull(result, "Filtered list must not be null");

        // 8) Verify that only "Apple" remains
        assertEquals(1, result.size(), "Exactly one item should survive all filters");
        assertEquals("Apple", result.get(0).getName(), "That one item should be Apple");
    }

    @Test
    public void searchItemsWithoutFilters() {
        // Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        fixtures.generateShopAndItems(ownerToken,"MyShop");;

        // Guest enters the system
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest enterToSystem should succeed");
        guestResp.getData();

        // 1) Search without any filters
        HashMap<String,String> emptyFilters = new HashMap<>();
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,emptyFilters);
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed");
        List<ItemDTO> items = searchResp.getData();
        assertEquals(3, items.size(), "Should return all 3 available items");
    }

    @Test
    public void emptySearchResults() {
        // Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        fixtures.generateShopAndItems(ownerToken,"MyShop");

        // Guest enters
        userService.enterToSystem();

        // 2) Search with a name that matches nothing
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name", "NoSuchItem");
        Response<List<ItemDTO>> searchResp = shopService.filterItemsAllShops(ownerToken,filters);
        assertTrue(searchResp.isOk(), "filterItemsAllShops should succeed even if empty");
        assertTrue(searchResp.getData().isEmpty(), "No items should be found");
    }


    @Test
    public void searchItemsInSpecificShop() {
        // Owner creates a shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");;

        // Guest enters
        userService.enterToSystem();

        // 4) Search in that shop for bananas priced <=0.50
        HashMap<String,String> filters = new HashMap<>();
        filters.put("name",     "Banana");
        filters.put("category", Category.FOOD.name());
        filters.put("minPrice","0");
        filters.put("maxPrice","0.50");

        Response<List<ItemDTO>> resp = shopService.filterItemsInShop(ownerToken,shop.getId(), filters);
        assertTrue(resp.isOk(), "filterItemsInShop should succeed");
        List<ItemDTO> results = resp.getData();
        assertEquals(1, results.size(), "Exactly one banana at price <= 0.50 should match");
        assertEquals("Banana", results.get(0).getName());
    }

    @Test
    public void shopNotFound() {
        // Guest enters
        String guestToken=userService.enterToSystem().getData();
        

        // 5) Use a non-existent shop ID
        int missingShopId = 9999;
        HashMap<String,String> filters = new HashMap<>();
        Response<List<ItemDTO>> resp = shopService.filterItemsInShop(guestToken,missingShopId, filters);

        // Right now this blows up with a NullPointerException. You need to catch that
        // inside filterItemsInShop and return Response.error("Shop not found");
        assertFalse(resp.isOk());
    }

    @Test
    public void addItemToBasketTest() {
        //  1) Owner setup 
        // Owner enters as guest
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        
        //  2) Buyer setup 
        // Buyer enters as guest
        Response<String> buyerGuestResp = userService.enterToSystem();
        assertTrue(buyerGuestResp.isOk(), "Buyer enterToSystem should succeed");
        String buyerGuestToken = buyerGuestResp.getData();
        assertNotNull(buyerGuestToken, "Buyer guest token must not be null");

        //  3) Buyer shopping & checkout 
        // Buyer views the shop's items
        Response<List<ItemDTO>> viewResp = shopService.showShopItems(ownerToken,shop.getId());
        assertTrue(viewResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> shopItems = viewResp.getData();
        assertNotNull(shopItems, "shopItems list must not be null");
        assertEquals(3, shopItems.size(), "Shop should contain exactly one item");

        // Buyer adds that item to cart
        Response<Void> addToCart = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            shopItems.get(0).getItemID(),
            1
        );
        assertTrue(addToCart.isOk(), "addItemsToCart should succeed");

        List<ItemDTO> cartItems = orderService.checkCartContent(buyerGuestToken).getData();
        assertEquals(1, cartItems.size(), "Cart should contain exactly one item");
    }

    @Test
    public void checkCartContentTest() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        // grab the first item from the shop
        List<ItemDTO> items = shopService.showShopItems(ownerToken,shop.getId()).getData();
        assertEquals(3, items.size(), "Shop should have 3 items");

        //  2) Buyer setup 
        // Buyer enters as guest
        Response<String> buyerGuestResp = userService.enterToSystem();
        assertTrue(buyerGuestResp.isOk(), "Buyer enterToSystem should succeed");
        String buyerGuestToken = buyerGuestResp.getData();
        assertNotNull(buyerGuestToken, "Buyer guest token must not be null");

        
        Response<Void> addToCart = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            items.get(0).getItemID(),
            1
        );
        assertTrue(addToCart.isOk(), "addItemsToCart should succeed");

        // verify cart contents
        List<ItemDTO> cartItems = orderService.checkCartContent(buyerGuestToken).getData();
        assertEquals(1, cartItems.size(), "Cart should contain exactly one item");
    }

    @Test
    public void successfulBuyCartContentTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(0);

        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");


        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemsToCart should succeed");

        // 4) Purchase (pass dummy payment/shipment; replace with valid data if needed)
        Order created = fixtures.successfulBuyCartContent(buyerToken, p, s);

        // verify stock was decremented by 1
        Response<ShopDTO> updatedShopResp = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(updatedShopResp.isOk(), "getShopInfo should succeed after cart operation");
        ShopDTO updatedShop = updatedShopResp.getData();
        int originalQty = shop.getItems().get(shopItems.get(0).getItemID()).getQuantity();
        int newQty      = updatedShop.getItems().get(shopItems.get(0).getItemID()).getQuantity();
        assertEquals(originalQty - 1, newQty, "Stock should decrease by 1 when added to cart");

        // 5) Verify via service (no direct repo access)
        Response<List<Order>> historyResp = orderService.viewPersonalOrderHistory(buyerToken);
        assertTrue(historyResp.isOk(), "viewPersonalOrderHistory should succeed");
        List<Order> history = historyResp.getData();
        assertEquals(1, history.size(), "Exactly one order should exist for this buyer");

        Order recorded = history.get(0);
        assertEquals(created.getId(), recorded.getId(), "Order IDs should match");
        assertEquals(1, recorded.getItems().size(), "Order must contain exactly one item");
        assertEquals(
            toBuy.getName(),
            recorded.getItems().get(0).getName(),
            "Purchased item's name must match what was added"
        );

        verify(payment, atLeastOnce()).validatePaymentDetails(p); // validatePaymentDetails should be called at least once
        verify(payment).processPayment(1.0, p);
        verify(shipment, atLeastOnce()).validateShipmentDetails(s); // validateShipmentDetails should be called at least once
        verify(shipment).processShipment(s);
    }

    @Test
    public void BuyCartContentTest_paymentFails() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockNegativePayment(p);
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(0);

        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");


        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemsToCart should succeed");

        // 4) Purchase (pass dummy payment/shipment; replace with valid data if needed)
        Response<Order> orderResp = orderService.buyCartContent(buyerToken, p, s);
        assertFalse(orderResp.isOk(), "buyCartContent should fail due to payment validation failure");

        verify(payment).validatePaymentDetails(p);
    }

    @Test
    public void BuyCartContentTest_shipmentFails() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockNegativeShipment(s);

        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(0);

        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");


        // 3) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemsToCart should succeed");

        // 4) Purchase (pass dummy payment/shipment; replace with valid data if needed)
        Response<Order> orderResp = orderService.buyCartContent(buyerToken, p, s);
        assertFalse(orderResp.isOk(), "buyCartContent should fail due to shipment validation failure");

        verify(shipment).validateShipmentDetails(s);
    }

    @Test
    public void changeCartContentTest() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();

        // 2) Buyer setup: enter, register, login
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Buyer enterToSystem should succeed");
        String guestToken = guestResp.getData();


        // 3) Add two different items to cart
        Response<Void> addResp = orderService.addItemToCart(
            guestToken,
            shop.getId(),
            shopItems.get(0).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemsToCart should succeed");
        addResp = orderService.addItemToCart(
            guestToken,
            shop.getId(),
            shopItems.get(1).getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemsToCart should succeed");

        List<ItemDTO> cartAfterAdd = orderService.checkCartContent(guestToken).getData();
        assertEquals(2, cartAfterAdd.size(), "Cart should contain exactly two items");

        HashMap<Integer, List<Integer>> itemMap1 = new HashMap<>();
        itemMap1.put(shop.getId(), List.of(shopItems.get(0).getItemID()));

        // 4) Remove the first item
        Response<Void> removeResp = orderService.removeItemFromCart(
            guestToken,
            shop.getId(),
            shopItems.get(0).getItemID()
        );
        assertTrue(removeResp.isOk(), "removeItemsFromCart should succeed");

        // 5) Re-fetch and verify
        List<ItemDTO> cartAfterRemove = orderService.checkCartContent(guestToken).getData();
        assertEquals(1, cartAfterRemove.size(), "Cart should contain exactly one item after removal");
        assertEquals(
            shopItems.get(1).getItemID(),
            cartAfterRemove.get(0).getItemID(),
            "Remaining item should be the second one originally added"
        );
    }

    @Test
    public void openShopTest()
    {
        Response<String> ownerGuestResp = userService.enterToSystem();
        assertTrue(ownerGuestResp.isOk(), "Owner enterToSystem should succeed");
        String ownerGuestToken = ownerGuestResp.getData();
        assertNotNull(ownerGuestToken, "Owner guest token must not be null");

        // Owner registers
        Response<Void> ownerReg = userService.registerUser(
            ownerGuestToken, "owner", "pwdO", LocalDate.now().minusYears(30)
        );
        assertTrue(ownerReg.isOk(), "Owner registration should succeed");

        // Owner logs in
        Response<String> ownerLogin = userService.loginUser(
            ownerGuestToken, "owner", "pwdO"
        );
        assertTrue(ownerLogin.isOk(), "Owner login should succeed");
        String ownerToken = ownerLogin.getData();
        assertNotNull(ownerToken, "Owner token must not be null");

        // Owner creates shop
        Response<ShopDTO> shopResp = shopService.createShop(
            ownerToken, "MyShop", "A shop for tests"
        );
        assertTrue(shopResp.isOk(), "Shop creation should succeed");
        ShopDTO shop = shopResp.getData();
        assertNotNull(shop, "Returned ShopDTO must not be null");
    }


    @Test
    public void rateShopTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");

        // 2) Buyer setup: enter -> register -> login
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");

        // 3) Buyer purchases one item (necessary to enable rating)
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        assertFalse(shopItems.isEmpty(), "Shop must have at least one item");
        ItemDTO toBuy = shopItems.get(0);

        Response<Void> addToCart = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addToCart.isOk(), "addItemsToCart should succeed");

        // dummy payment & shipment (fill in real fields if needed)
        fixtures.successfulBuyCartContent(buyerToken, p, s);

        // 4) Rate the shop
        int ratingScore = 5;
        Response<Void> rateResp = shopService.rateShop(buyerToken, shop.getId(), ratingScore);
        assertTrue(rateResp.isOk(), "rateShop should succeed");

        // 5) Fetch shop info and verify its rating
        Response<ShopDTO> infoResp = shopService.getShopInfo(buyerToken, shop.getId());
        assertTrue(infoResp.isOk(), "getShopInfo should succeed after rating");
        ShopDTO ratedShop = infoResp.getData();
        assertEquals(ratingScore, ratedShop.getRating(), "Shop rating should match the score given");
    }

    @Test
    public void rateItemTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("OwnerItem", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "ItemShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken, shop.getId()).getData();
        assertFalse(shopItems.isEmpty(), "Shop must have at least one item");
        ItemDTO toBuy = shopItems.get(0);
        int shopId = shop.getId();
        int itemId = toBuy.getItemID();

        // 2) Buyer setup: register and login
        String buyerToken = fixtures.generateRegisteredUserSession("itemBuyer", "Pwd1");

        // 3) Add to cart
        Response<Void> addToCart = orderService.addItemToCart(
            buyerToken,
            shopId,
            itemId,
            1
        );
        assertTrue(addToCart.isOk(), "addItemsToCart should succeed");

        // 4) Complete purchase
        fixtures.successfulBuyCartContent(buyerToken, p, s);

        // 5) Rate the item
        int ratingScore = 4;
        Response<Void> rateResp = shopService.rateItem(buyerToken, shopId, itemId, ratingScore);
        assertTrue(rateResp.isOk(), "rateItem should succeed");

        // 6) Fetch shop items and verify the item's rating
        List<ItemDTO> updatedItems = shopService.showShopItems(ownerToken, shopId).getData();
        boolean found = false;
        for (ItemDTO item : updatedItems) {
            if (item.getItemID() == itemId) {
                assertEquals(ratingScore, item.getRating(), "Item rating should match the score given");
                found = true;
                break;
            }
        }
        assertTrue(found, "Rated item must be present in shop items");
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
    public void respondToMessageTest() {
        // 1) Owner setup: register and create a shop
        String ownerToken = fixtures.generateRegisteredUserSession("OwnerMsg", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MsgShop");
        int shopId = shop.getId();

        // 2) Customer setup: register/login a user who will send the question
        String customerToken = fixtures.generateRegisteredUserSession("CustomerA", "CustPwd");

        // 3) Customer sends a message to the shop
        String title = "Question about an item";
        String message = "Do you have refunds?";
        Response<Void> sendResp = shopService.sendMessage(
            customerToken,
            shopId,
            title,
            message
        );
        assertTrue(sendResp.isOk(), "sendMessageToShop should succeed");

        // 4) Fetch messages for the shop so we can grab the new message ID.
        //    (Assumes you have a method like viewShopMessages or getAllMessagesInShop.)
        //    If your API returns List<MessageDTO>, find the one whose content is `questionText`.
        Response<List<Message>> inboxResp = shopService.getInbox(ownerToken, shopId);
        assertTrue(inboxResp.isOk(), "viewShopMessages should succeed for owner/manager");
        List<Message> inbox = inboxResp.getData();
        assertFalse(inbox.isEmpty(), "There must be at least one message");
        // Find the message just inserted
        Message incoming = inbox.stream()
            .filter(m -> m.getContent().equals(message))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Could not find the question message"));

        int messageId = incoming.getId();

        

        // 5) Manager setup: register a second user and assign them as manager with ANSWER_MESSAGE permission
        String managerToken = fixtures.generateRegisteredUserSession("ShopMgr", "MgrPwd");
        String managerUsername = "ShopMgr";

        // Give the manager the ANSWER_MESSAGE permission so they can respond:
        Response<Void> addMgrResp = shopService.addShopManager(
            ownerToken,
            shopId,
            managerUsername,
            Set.of(Permission.ANSWER_MESSAGE)
        );
        assertTrue(addMgrResp.isOk(), "addShopManager(with ANSWER_MESSAGE) should succeed");

        // 6) Manager calls respondToMessage
        String title_2 = "Response to your question";
        String responseText = "Yes—this item is in stock!";
        Response<Void> replyResp = shopService.respondToMessage(
            managerToken,
            shopId,
            messageId,
            title_2,
            responseText
        );
        assertTrue(replyResp.isOk(), "respondToMessage should succeed when user has ANSWER_MESSAGE permission");

        // 7) Verify that the reply was saved in the message thread.
        //    For example, view the entire thread again and check that the “inReplyTo” field points to messageId:
        Response<List<Message>> threadResp = shopService.getInbox(ownerToken, shopId);
        assertTrue(threadResp.isOk(), "viewShopMessages should still succeed after reply");
        List<Message> updatedInbox = threadResp.getData();

        // Find the newly inserted reply
        Message replyMsg = updatedInbox.stream()
            .filter(m -> m.getContent().equals(message))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reply message was not found"));
        assertEquals(responseText, replyMsg.getContent(), "The reply content must match what the manager sent");
        assertEquals(managerUsername, replyMsg.getUserName(), "Reply sender should be the shop manager");
        assertEquals(messageId, replyMsg.getRespondId(), "Reply should reference the original message ID");
    }

    // @Test
    // public void itemUnavailableForPurchaseTest() {
    //     PaymentDetailsDTO p = new PaymentDetailsDTO(
    //         "1234567890123456", "Some Name", "1", "123", "12", "25"
    //     );
    //     ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

    //     // --- Setup -------------------------------------------------------------
    //     // 1) Prepare payment/shipment mocks
    //     fixtures.mockPositivePayment(p);
    //     fixtures.mockPositiveShipment(s);

    //     // 2) Owner creates shop with 3 items
    //     String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
    //     ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
    //     int shopId = shop.getId();

    //     // 3) Set the quantity of the first item to 0 (unavailable)
    //     ItemDTO firstItem = shop.getItems().get(0);
    //     Response<Void> setQtyResp = shopService.changeItemQuantityInShop(
    //         ownerToken, shopId, firstItem.getItemID(), 0
    //     );
    //     assertTrue(setQtyResp.isOk(), "Setting quantity to 0 should succeed");

    //     // Capture the stock before any buyer interactions
    //     int stockBefore = shopService
    //         .getShopInfo(ownerToken, shopId)
    //         .getData()
    //         .getItems()
    //         .get(firstItem.getItemID())
    //         .getQuantity();
    //     assertEquals(0, stockBefore, "Stock should now be zero");

    //     // Buyer enters the system
    //     String buyerToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd1");

    //     // --- Action ------------------------------------------------------------
    //     // Buyer attempts to add all three items (including the unavailable one) to cart
    //     List<ItemDTO> availableItems = shopService.showShopItems(ownerToken,shopId).getData();
    //     HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
    //     HashMap<Integer, Integer> itemMap = new HashMap<>();
    //     for (ItemDTO it : availableItems) {
    //         itemMap.put(it.getItemID(), 1);
    //     }
    //     itemsMap.put(shopId, itemMap);

    //     Response<Void> addToCartResp = orderService.addItemsToCart(buyerToken, itemsMap);

    //     // Buyer then attempts to checkout
    //     Response<Order> checkoutResp = orderService.buyCartContent(buyerToken, p, s);

    //     // --- Verification -------------------------------------------------------
    //     // 1) Adding to cart should fail because one item has zero stock
    //     assertFalse(addToCartResp.isOk(),
    //         "addItemsToCart must fail when any requested item is out of stock");

    //     // 2) Checkout should also fail
    //     assertFalse(checkoutResp.isOk(),
    //         "buyCartContent must fail when cart contains unavailable items");

    //     // 3) Cart should remain empty after the failed attempts
    //     List<ItemDTO> cartAfter = orderService.checkCartContent(buyerToken).getData();
    //     assertNotNull(cartAfter, "checkCartContent must return a (possibly empty) list");
    //     assertTrue(cartAfter.isEmpty(), "Cart must be empty after a failed addition/checkout");

    //     // 4) Stock must be unchanged (still zero for the first item)
    //     int stockAfter = shopService
    //         .getShopInfo(ownerToken, shopId)
    //         .getData()
    //         .getItems()
    //         .get(firstItem.getItemID())
    //         .getQuantity();
    //     assertEquals(stockBefore, stockAfter,
    //         "Stock of the unavailable item must remain unchanged");
    // }

    @Test
    public void concurrentSingleStockCartPurchaseTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        // --- Arrange mocks for payments and shipments ---
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // --- 1) Owner creates shop with a single‐unit item ---
        String ownerToken = fixtures.generateRegisteredUserSession("owner", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();

        // Pick the first item and force its stock to exactly 1
        ItemDTO item = shop.getItems().get(0);
        assertTrue(
            shopService.changeItemQuantityInShop(ownerToken, shopId, item.getItemID(), 1).isOk(),
            "Setting item stock to 1 should succeed"
        );

        // --- 2) Two buyers each add that same single unit to their carts ---
        String buyer1 = fixtures.generateRegisteredUserSession("buyer1", "pwd1");
        String buyer2 = fixtures.generateRegisteredUserSession("buyer2", "pwd2");


        // Buyer1 adds to cart
        Response<Void> add1 = orderService.addItemToCart(buyer1, shopId, item.getItemID(), 1);
        assertTrue(add1.isOk(), "Buyer1 should add the item to cart");

        // Buyer2 also adds to cart
        Response<Void> add2 = orderService.addItemToCart(buyer2, shopId, item.getItemID(), 1);
        assertTrue(add2.isOk(), "Buyer2 should add the item to cart");

        // --- 3) Buyer1 completes purchase successfully ---
        Response<Order> buy1 = orderService.buyCartContent(buyer1, p, s);
        assertTrue(buy1.isOk(), "Buyer1 purchase should succeed");

        // --- 4) Buyer2 attempt to purchase must fail (out of stock) ---
        Response<Order> buy2 = orderService.buyCartContent(buyer2, p, s);
        assertFalse(buy2.isOk(), "Buyer2 purchase should fail due to no stock");

        // --- 5) Verify final stock is zero and buyer2’s cart is empty ---
        int finalStock = shopService
            .getShopInfo(ownerToken, shopId)
            .getData()
            .getItems()
            .get(item.getItemID())
            .getQuantity();
        assertEquals(0, finalStock, "Final stock should be zero after Buyer1 purchase");

    }

    @Test
    public void auctionFlowSuccessTest() throws InterruptedException {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();
        int appleId = shop.getItems().values().iterator().next().getItemID();

        // Open auction: start in 1s, end in 3s (2s duration)
        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusSeconds(3);
        Response<Void> openResp = shopService.openAuction(
            ownerToken, shopId, appleId, 5.0, start, end
        );
        assertTrue(openResp.isOk(), "Opening auction should succeed");

        // Wait for auction to start
        Thread.sleep(1100);

        // Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd1");
        int auctionId = 1;

        // Submit offer (should succeed)
        assertTrue(
            orderService.submitAuctionOffer(buyerToken, shopId, auctionId, 7.0).isOk(),
            "Submitting auction offer should succeed"
        );

        // Wait for auction to end
        Thread.sleep(2100);

        // Now purchase (should succeed)
        assertTrue(
            orderService.purchaseAuctionItem(buyerToken, shopId, auctionId, p, s).isOk(),
            "Purchasing auction item should succeed"
        );

        // Verify order history
        List<Order> history = orderService.viewPersonalOrderHistory(buyerToken).getData();
        assertEquals(1, history.size(), "There should be one auction-based order");
    }

    @Test
    public void sumbitAuctionOfferBeforeAuctionStartsShouldFailTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();
        int appleId = shop.getItems().values().iterator().next().getItemID();

        // Schedule auction to start shortly
        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusSeconds(3);
        assertTrue(
            shopService.openAuction(ownerToken, shopId, appleId, 5.0, start, end).isOk(),
            "Opening auction should succeed"
        );

        // Buyer setup and attempt to submit offer before auction starts
        String buyerToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd1");
        int auctionId = 1;
        Response<Void> offerResp = orderService.submitAuctionOffer(buyerToken, shopId, auctionId, 7.0);
        assertFalse(offerResp.isOk(), "Submitting auction offer before start should fail");
    }

    @Test
    public void purchaseAuctionOfferBeforeAuctionEndsShouldFailTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();
        int appleId = shop.getItems().values().iterator().next().getItemID();

        // Schedule auction to start shortly
        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusSeconds(3);
        assertTrue(
            shopService.openAuction(ownerToken, shopId, appleId, 5.0, start, end).isOk(),
            "Opening auction should succeed"
        );
        try {
            Thread.sleep(1000); // wait for auction to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Buyer setup and attempt to submit offer before auction starts
        String buyerToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd1");
        int auctionId = 1;
        Response<Void> offerResp = orderService.submitAuctionOffer(buyerToken, shopId, auctionId, 7.0);
        assertTrue(offerResp.isOk(), "Submitting auction offer after start should succeed");
        
        // Attempt purchase immediately before auction starts
        Response<Void> purchaseResp = orderService.purchaseAuctionItem(buyerToken, shopId, auctionId, p, s);        assertFalse(purchaseResp.isOk(), "Purchasing before auction ends should fail");
    }

    @Test
    public void anotherUserCannotPurchaseWonAuctionTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);


        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();
        int appleId = shop.getItems().values().iterator().next().getItemID();

        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusSeconds(2);
        assertTrue(shopService.openAuction(ownerToken, shopId, appleId, 5.0, start, end).isOk(), "Opening auction should succeed");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String bidder1 = fixtures.generateRegisteredUserSession("Alice", "PwdA");
        String bidder2 = fixtures.generateRegisteredUserSession("Bob", "PwdB");
        int auctionId = 1;
        assertTrue(orderService.submitAuctionOffer(bidder2, shopId, auctionId, 8.0).isOk(), "First bid should succeed");
        assertTrue(orderService.submitAuctionOffer(bidder1, shopId, auctionId, 10.0).isOk(), "First bid should succeed");
        
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Bob tries to purchase after auction ends
        Response<Void> bobPurchase = orderService.purchaseAuctionItem(bidder2, shopId, auctionId, p, s);
        assertFalse(bobPurchase.isOk(), "Non-winning bidder should not be able to purchase");

        // Alice purchases successfully
        Response<Void> alicePurchase = orderService.purchaseAuctionItem(bidder1, shopId, auctionId, p, s);
        assertTrue(alicePurchase.isOk(), "Winning bidder should purchase successfully");
    }

    @Test
    public void concurrentPurchaseSameItem() throws InterruptedException {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        for (int i = 0; i < 10; i++) {
            // 1) Owner creates shop with exactly 1 unit of “Apple”
            String ownerToken = fixtures.generateRegisteredUserSession("owner"+i, "pwdO");
            ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop"+i);
            // find the apple’s ID and set its stock to 1
            int shopId = shop.getId();
            int appleId = shop.getItems().values().stream()
                                .filter(j -> j.getName().equals("Apple"))
                                .findFirst().get().getItemID();
            Response<Void> setOne = shopService.changeItemQuantityInShop(
                ownerToken, shopId, appleId, 1
            );
            assertTrue(setOne.isOk(), "Should be able to set stock to 1");

            // 2) Two separate buyers each enter & add that one apple to their cart
            String buyer1 = userService.enterToSystem().getData();
            String buyer2 = userService.enterToSystem().getData();
            assertNotNull(buyer1);
            assertNotNull(buyer2);


            Response<Void> add1 = orderService.addItemToCart(buyer1,shopId, appleId, 1);
            Response<Void> add2 = orderService.addItemToCart(buyer2,shopId, appleId, 1);
            assertTrue(add1.isOk(), "Buyer1 should add Apple");
            assertTrue(add2.isOk(), "Buyer2 should add Apple");

            // 3) Concurrently attempt to buy
            List<Callable<Response<Order>>> tasks = List.of(
                () -> orderService.buyCartContent(buyer1, p, s),
                () -> orderService.buyCartContent(buyer2, p, s)
            );

            ExecutorService ex = Executors.newFixedThreadPool(2);
            List<Future<Response<Order>>> futures = ex.invokeAll(tasks);
            ex.shutdown();

            long successCount = futures.stream()
            .map(f -> {
                try { return f.get().isOk(); }
                catch(Exception e) { return false; }
            })
            .filter(ok -> ok)
            .count();

            long failureCount = futures.size() - successCount;

            assertEquals(1, successCount, "Exactly one purchase may succeed");
            assertEquals(1, failureCount, "Exactly one purchase must fail due to out-of-stock");
        }
    }

    @Test
    public void concurrentRemoveAndPurchase() throws InterruptedException {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        
        for (int i = 0; i < 10; i++) {
            // 1) Owner creates shop with exactly 1 unit of “Apple”
            String ownerToken = fixtures.generateRegisteredUserSession("owner"+i, "pwdO");
            ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop"+i);
            int shopId = shop.getId();
            int appleId = shop.getItems().values().stream()
                                .filter(j -> j.getName().equals("Apple"))
                                .findFirst().get().getItemID();
            // set stock = 1
            assertTrue(shopService.changeItemQuantityInShop(ownerToken, shopId, appleId, 1)
                    .isOk(), "Should be able to set stock to 1");

            // 2) Buyer enters & adds that one apple to cart
            String buyer = userService.enterToSystem().getData();
            var itemsMap = new HashMap<Integer, HashMap<Integer,Integer>>();
            itemsMap.put(shopId, new HashMap<>(Map.of(appleId, 1)));
            assertTrue(orderService.addItemToCart(buyer, shopId, appleId, 1)
                    .isOk(), "Buyer should add the only Apple");

            // 3) Concurrently: owner removes item, buyer attempts checkout
            List<Callable<Boolean>> tasks = List.of(
                () -> shopService.removeItemFromShop(ownerToken, shopId, appleId).isOk(),
                () -> orderService.buyCartContent(buyer, p, s).isOk()
            );

            ExecutorService ex = Executors.newFixedThreadPool(2);
            List<Future<Boolean>> results = ex.invokeAll(tasks);
            ex.shutdown();

            long succeeded = results.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return false; }
                })
                .filter(ok -> ok)
                .count();

            // exactly one action may succeed
            assertEquals(1, succeeded,
                "Exactly one of removeItem or buyCartContent must succeed; succeeded=" + succeeded);
        }
    }

    @Test
    public void submitValidBidTest() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("owner", "pwdO");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();

        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "pwdB");

        // 3) Submit a valid bid (higher than base price, e.g. base price assumed 5.0)
        Response<Void> bidResp = orderService.submitBidOffer(buyerToken, shopId, itemId, 10.0);
        assertTrue(bidResp.isOk(), "submitBidOffer should succeed for a valid bid");
    }

    @Test
    public void onlyHighestBidderCanPurchaseTest() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("owner3", "pwdO3");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "Shop3");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();

        // 2) Two buyers register
        String bidder1 = fixtures.generateRegisteredUserSession("bidder1", "pwd1");
        String bidder2 = fixtures.generateRegisteredUserSession("bidder2", "pwd2");

        // 3) Submit bids
        Response<Void> bid1 = orderService.submitBidOffer(bidder1, shopId, itemId, 8.0);
        assertTrue(bid1.isOk(), "First bid (8.0) should succeed");
        Response<Void> bid2 = orderService.submitBidOffer(bidder2, shopId, itemId, 12.0);
        assertTrue(bid2.isOk(), "Second bid (12.0) should succeed");

        Response<Void> answer = shopService.answerBid(ownerToken, shopId, 2, true);
        assertTrue(answer.isOk(), "Owner acceptance of bid2 should succeed");

        // We assume bid IDs are assigned sequentially: bid1 → ID=1, bid2 → ID=2
        int bidId1 = 1;
        int bidId2 = 2;

        // 4) Mock positive payment and shipment
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1111222233334444", "Name1", "1", "123", "01", "30"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO(
            "A1", "Name1", "", "555-1234", "CountryX", "CityY", "AddressZ", "10101"
        );
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 5) Non-highest bidder (bidId1) tries to purchase → should fail
        Response<Void> purchase1 = orderService.purchaseBidItem(bidder1, shopId, bidId1, p, s);
        assertFalse(purchase1.isOk(), "Non-highest bidder purchase should fail");
        
        // 6) Highest bidder (bidId2) tries to purchase → should succeed
        Response<Void> purchase2 = orderService.purchaseBidItem(bidder2, shopId, bidId2, p, s);
        assertTrue(purchase2.isOk(), "Highest bidder purchase should succeed");
    }

    @Test
    public void counterBidWorkflowTest() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("ownerCB", "pwdOCB");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "ShopCB");
        int shopId = shop.getId();
        int itemId = shop.getItems().values().iterator().next().getItemID();

        // 2) Buyer submits initial bid
        String buyer = fixtures.generateRegisteredUserSession("buyerCB", "pwdBCB");
        Response<Void> initialBid = orderService.submitBidOffer(buyer, shopId, itemId, 8.0);
        assertTrue(initialBid.isOk(), "Initial bid (8.0) should succeed");

        // We assume the first bid’s ID is 1
        int bidId = 1;

        // 3) Owner counters that bid (accept = false)
        Response<Void> ownerCounter = shopService.submitCounterBid(ownerToken, shopId, bidId, 10.0);
        assertTrue(ownerCounter.isOk(), "Owner's counter (reject) should succeed");

        // 4) Buyer accepts the counter (accept = true)
        Response<Void> buyerAccepts = orderService.answerOnCounterBid(buyer, shopId, bidId, true);
        assertTrue(buyerAccepts.isOk(), "Buyer's acceptance of counter should succeed");

        // 5) Mock positive payment and shipment
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "3333444455556666", "CB Buyer", "3", "345", "03", "32"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO(
            "CB1", "CB Buyer", "", "555-3333", "CountryCB", "CityCB", "AddressCB", "30303"
        );
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 6) Buyer attempts to purchase the countered bid → should now succeed
        Response<Void> purchaseAfterCounter = orderService.purchaseBidItem(buyer, shopId, bidId, p, s);
        assertTrue(purchaseAfterCounter.isOk(), "Buyer should succeed purchasing after accepting counter");
    }
}
