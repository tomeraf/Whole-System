package Tests.AcceptanceTests;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;


import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.PriceCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.QuantityCondition;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition.AndCondition;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;

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
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        
        //  2) Buyer setup 
        Response<String> buyerGuestResp = userService.enterToSystem();
        assertTrue(buyerGuestResp.isOk(), "Buyer enterToSystem should succeed");
        String buyerGuestToken = buyerGuestResp.getData();
        assertNotNull(buyerGuestToken, "Buyer guest token must not be null");

        //  3) Buyer shopping & checkout 
        Response<List<ItemDTO>> viewResp = shopService.showShopItems(ownerToken,shop.getId());
        assertTrue(viewResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> shopItems = viewResp.getData();
        assertNotNull(shopItems, "shopItems list must not be null");
        assertEquals(3, shopItems.size(), "Shop should contain exactly 3 items");
        
        // Capture item details before adding to cart
        ItemDTO itemToAdd = shopItems.get(0);
        int initialQuantity = itemToAdd.getQuantity();
        
        // Buyer adds that item to cart
        Response<Void> addToCart = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            itemToAdd.getItemID(),
            1
        );
        assertTrue(addToCart.isOk(), "addItemToCart should succeed");

        // Verify cart content
        Response<List<ItemDTO>> cartResponse = orderService.checkCartContent(buyerGuestToken);
        assertTrue(cartResponse.isOk(), "checkCartContent should succeed");
        List<ItemDTO> cartItems = cartResponse.getData();
        assertEquals(1, cartItems.size(), "Cart should contain exactly one item");
        
        // Verify the item in cart matches what was added
        ItemDTO cartItem = cartItems.get(0);
        assertEquals(itemToAdd.getItemID(), cartItem.getItemID(), "Item ID in cart should match added item");
        assertEquals(itemToAdd.getName(), cartItem.getName(), "Item name in cart should match added item");
        assertEquals(itemToAdd.getPrice(), cartItem.getPrice(), "Item price in cart should match added item");
        assertEquals(1, cartItem.getQuantity(), "Item quantity in cart should be 1");
        
        // Verify shop inventory is unchanged
        Response<ShopDTO> shopAfterResp = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(shopAfterResp.isOk(), "getShopInfo should succeed after adding to cart");
        ShopDTO shopAfter = shopAfterResp.getData();
        int quantityAfter = shopAfter.getItems().get(itemToAdd.getItemID()).getQuantity();
        assertEquals(initialQuantity, quantityAfter, "Shop inventory should not change when adding to cart");
        
        // Add more quantity of the same item
        Response<Void> addMoreQty = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            itemToAdd.getItemID(),
            2
        );
        assertTrue(addMoreQty.isOk(), "Adding more quantity should succeed");
        
        // Verify quantity was updated, not duplicated
        Response<List<ItemDTO>> updatedCartResp = orderService.checkCartContent(buyerGuestToken);
        List<ItemDTO> updatedCart = updatedCartResp.getData();
        assertEquals(1, updatedCart.size(), "Cart should still contain only one item type");
        assertEquals(3, updatedCart.get(0).getQuantity(), "Item quantity should now be 3");
        
        // Try to add a different item
        ItemDTO secondItem = shopItems.get(1);
        Response<Void> addSecondItem = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            secondItem.getItemID(),
            1
        );
        assertTrue(addSecondItem.isOk(), "Adding second item should succeed");
        
        // Verify cart now has two different items
        Response<List<ItemDTO>> finalCartResp = orderService.checkCartContent(buyerGuestToken);
        List<ItemDTO> finalCart = finalCartResp.getData();
        assertEquals(2, finalCart.size(), "Cart should now contain two different items");
        
        // Try adding with invalid quantity (assuming zero or negative should fail)
        Response<Void> invalidAdd = orderService.addItemToCart(
            buyerGuestToken,
            shop.getId(),
            shopItems.get(2).getItemID(),
            0
        );
        assertFalse(invalidAdd.isOk(), "Adding zero quantity should fail");
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
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerGuestToken);
        assertTrue(cartResp.isOk(), "checkCartContent should succeed");
        List<ItemDTO> cartItems = cartResp.getData();
        assertEquals(1, cartItems.size(), "Cart should contain exactly one item");
        
        // Verify the correct item is in cart with correct details
        ItemDTO cartItem = cartItems.get(0);
        assertEquals(items.get(0).getItemID(), cartItem.getItemID(), "Item ID should match");
        assertEquals(items.get(0).getName(), cartItem.getName(), "Item name should match");
        assertEquals(items.get(0).getPrice(), cartItem.getPrice(), "Item price should match");
        assertEquals(1, cartItem.getQuantity(), "Quantity should be 1");
        
        // Check empty cart for a different user
        Response<String> anotherUserResp = userService.enterToSystem();
        String anotherUserToken = anotherUserResp.getData();
        Response<List<ItemDTO>> emptyCartResp = orderService.checkCartContent(anotherUserToken);
        assertTrue(emptyCartResp.isOk(), "Empty cart check should succeed");
        assertTrue(emptyCartResp.getData().isEmpty(), "New user's cart should be empty");
    }

    @Test
    public void successfulBuyCartContentTest() {
        // 1) Setup payment and shipment details
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 2) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(0);

        // 3) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");

        // Verify payment and shipment details are valid
        assertTrue(p.fullDetails(), "Payment details must be complete");
        assertTrue(s.fullShipmentDetails(), "Shipment details must be complete");

        // 4) Add to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "addItemToCart should succeed");

        // 5) Purchase (pass payment/shipment details)
        Order created = fixtures.successfulBuyCartContent(buyerToken, p, s);

        // 6) Verify payment and shipment IDs 
        assertEquals(12345, created.getPaymentId(), "Payment ID should match");
        assertEquals(67890, created.getShipmentId(), "Shipment ID should match");

        // 7) Verify stock was decremented by 1
        Response<ShopDTO> updatedShopResp = shopService.getShopInfo(ownerToken, shop.getId());
        assertTrue(updatedShopResp.isOk(), "getShopInfo should succeed after cart operation");
        ShopDTO updatedShop = updatedShopResp.getData();
        int originalQty = shop.getItems().get(toBuy.getItemID()).getQuantity();
        int newQty = updatedShop.getItems().get(toBuy.getItemID()).getQuantity();
        assertEquals(originalQty - 1, newQty, "Stock should decrease by 1 when purchased");

        // 8) Verify buyer's cart is now empty
        Response<List<ItemDTO>> cartAfterPurchase = orderService.checkCartContent(buyerToken);
        assertTrue(cartAfterPurchase.isOk(), "Cart check after purchase should succeed");
        assertTrue(cartAfterPurchase.getData().isEmpty(), "Cart should be empty after purchase");
        
        // 9) Verify order is in buyer's order history
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

        // 10) Verify services were called
        verify(payment, atLeastOnce()).validatePaymentDetails(p);
        verify(payment).processPayment(1.0, p);
        verify(shipment, atLeastOnce()).validateShipmentDetails(s);
        verify(shipment).processShipment(s);
    }

    @Test
    public void BuyCartContentTest_paymentFails() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        // Mock the payment service to FAIL validation
        fixtures.mockNegativePayment(p);
        fixtures.mockPositiveShipment(s);  // Shipment is fine, only payment fails

        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO toBuy = shopItems.get(0);
        
        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");
        
        // 3) Add item to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            toBuy.getItemID(),
            1
        );
        assertTrue(addResp.isOk(), "Adding item to cart should succeed");
        
        // Verify item is in cart
        Response<List<ItemDTO>> cartBeforeResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartBeforeResp.isOk(), "Checking cart should succeed");
        List<ItemDTO> cartBefore = cartBeforeResp.getData();
        assertEquals(1, cartBefore.size(), "Cart should contain one item");
        
        // Capture shop inventory before attempted purchase
        int initialStock = shop.getItems().get(toBuy.getItemID()).getQuantity();
        
        // 4) Attempt to purchase - THIS SHOULD FAIL due to payment validation
        Response<Order> buyResp = orderService.buyCartContent(buyerToken, p, s);
        assertFalse(buyResp.isOk(), "buyCartContent should fail due to payment validation failure");
        
        // 5) Verify cart remains unchanged
        Response<List<ItemDTO>> cartAfterResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartAfterResp.isOk(), "Checking cart after failed purchase should succeed");
        List<ItemDTO> cartAfter = cartAfterResp.getData();
        assertEquals(1, cartAfter.size(), "Cart should still contain one item after failed purchase");
        
        // 6) Verify shop inventory not reduced
        Response<ShopDTO> shopAfterResp = shopService.getShopInfo(ownerToken, shop.getId());
        ShopDTO updatedShop = shopAfterResp.getData();
        int finalStock = updatedShop.getItems().get(toBuy.getItemID()).getQuantity();
        assertEquals(initialStock, finalStock, "Shop inventory should not change after failed purchase");
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

    
    // Class: ShoppingTests.java - Additional OrderService tests

    @Test
    public void testPurchaseAuctionItem_ExpiredAuction_ShouldFail() throws InterruptedException {
        // Arrange
        String owner = fixtures.generateRegisteredUserSession("ownerEXP", "pwdE");
        ShopDTO shop = fixtures.generateShopAndItems(owner, "ExpShop");
        int shopId = shop.getId();
        int itemId = shop.getItems().get(0).getItemID();
        LocalDateTime start = LocalDateTime.now().minusSeconds(5);
        LocalDateTime end   = LocalDateTime.now().minusSeconds(1);
        shopService.openAuction(owner, shopId, itemId, 5.0, start, end);

        String bidder = fixtures.generateRegisteredUserSession("expBidder", "pwdX");
        PaymentDetailsDTO p = new PaymentDetailsDTO("4444333322221111","N","1","123","01","30");
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("S","N","","000","C","City","Addr","00000");

        // Act
        Response<Void> resp = orderService.purchaseAuctionItem(bidder, shopId, 1, p, s);
        assertFalse(resp.isOk(), "Cannot purchase from an expired auction");
    }

    @Test
    public void testViewPersonalOrderHistory_Empty_ShouldReturnEmptyList() {
        // Arrange
        String buyer = fixtures.generateRegisteredUserSession("histEmpty", "pwdH");
        // Act
        Response<List<Order>> resp = orderService.viewPersonalOrderHistory(buyer);
        assertTrue(resp.isOk());
        assertTrue(resp.getData().isEmpty(), "New user should have empty order history");
    }

    @Test
    public void itemUnavailableForPurchaseTest() {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");

        // --- Setup -------------------------------------------------------------
        // 1) Prepare payment/shipment mocks
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        // 2) Owner creates shop with 3 items
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        int shopId = shop.getId();

        // 3) Set the quantity of the first item to 0 (unavailable)
        ItemDTO firstItem = shop.getItems().get(0);
        Response<Void> setQtyResp = shopService.changeItemQuantityInShop(
            ownerToken, shopId, firstItem.getItemID(), 0
        );
        assertTrue(setQtyResp.isOk(), "Setting quantity to 0 should succeed");

        // Capture the stock before any buyer interactions
        int stockBefore = shopService
            .getShopInfo(ownerToken, shopId)
            .getData()
            .getItems()
            .get(firstItem.getItemID())
            .getQuantity();
        assertEquals(0, stockBefore, "Stock should now be zero");

        // Buyer enters the system
        String buyerToken = fixtures.generateRegisteredUserSession("Buyer", "Pwd1");

        // --- Action ------------------------------------------------------------
        // Buyer attempts to add the out-of-stock item to cart
        Response<Void> addToCartResp = orderService.addItemToCart(
            buyerToken, 
            shopId, 
            firstItem.getItemID(), 
            1
        );
        
        // --- Verification -------------------------------------------------------
        // 1) Adding to cart should fail because the item has zero stock
        assertFalse(addToCartResp.isOk(),
            "addItemToCart must fail when the requested item is out of stock");

        // 2) Let's try to add a different item that is in stock, then checkout
        ItemDTO secondItem = shop.getItems().get(1);
        Response<Void> addSecondItemResp = orderService.addItemToCart(
            buyerToken,
            shopId,
            secondItem.getItemID(),
            1
        );
        assertTrue(addSecondItemResp.isOk(), "Adding in-stock item should succeed");
        
        // 3) Checkout should succeed as the cart now only contains available items
        Response<Order> checkoutResp = orderService.buyCartContent(buyerToken, p, s);
        assertTrue(checkoutResp.isOk(), "Checkout should succeed with only in-stock items");
        
        // 4) Verify inventory was properly updated - first item unchanged (still 0)
        int stockAfter = shopService
            .getShopInfo(ownerToken, shopId)
            .getData()
            .getItems()
            .get(firstItem.getItemID())
            .getQuantity();
        assertEquals(stockBefore, stockAfter,
            "Stock of the unavailable item must remain unchanged");
        
        // 5) Second item's stock should be decremented
        int secondItemStockAfter = shopService
            .getShopInfo(ownerToken, shopId)
            .getData()
            .getItems()
            .get(secondItem.getItemID())
            .getQuantity();
        assertEquals(secondItem.getQuantity() - 1, secondItemStockAfter,
            "Stock of the purchased item should be decremented");
    }

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
        int auctionId = shopRepository.getShopById(shopId).getActiveAuctions().get(0).getId();

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
        int auctionId = shopRepository.getShopById(shopId).getActiveAuctions().get(0).getId();
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
        int auctionId = shopRepository.getShopById(shopId).getActiveAuctions().get(0).getId();
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
    @Transactional
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
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

            // commit these inserts so child threads can see them
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();


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
    @Transactional
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
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
            
                // commit these inserts so child threads can see them
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();
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
        // We assume bid IDs are assigned sequentially: bid1 → ID=1, bid2 → ID=2
        int bidId1 = shopRepository.getShopById(shopId).getBids().get(0).getId(); // bidder1's bid
        int bidId2 = shopRepository.getShopById(shopId).getBids().get(1).getId(); // bidder2's bid

        Response<Void> answer = shopService.answerBid(ownerToken, shopId, bidId2, true);
        assertTrue(answer.isOk(), "Owner acceptance of bid2 should succeed");


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

        int bidId = shopRepository
            .getShopById(shopId)
            .getBids()
            .get(0).getId(); // Assuming the first bid is the one we just submitted

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

    @Test
    public void addItemToCart_InvalidShopOrItem_ShouldFail() {
        String buyerToken = fixtures.generateRegisteredUserSession("buyerX", "PwdX");
        // a) Non-existent shop:
        assertFalse(
            orderService.addItemToCart(buyerToken, 9999, 1, 1).isOk(),
            "Adding to cart in a missing shop should fail"
        );
        // b) Real shop but invalid item ID:
        String ownerToken = fixtures.generateRegisteredUserSession("ownerX", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "TestShopX");
        int shopId = shop.getId();
        assertFalse(
            orderService.addItemToCart(buyerToken, shopId, 9999, 1).isOk(),
            "Adding a non-existent item to cart should fail"
        );
        // Verify cart is still empty after failed attempts
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartResp.isOk(), "checkCartContent should still succeed");
        assertTrue(cartResp.getData().isEmpty(), "Cart should be empty after failed add attempts");
    }

    @Test
    public void buyCartContent_EmptyCart_ShouldFail() {
        PaymentDetailsDTO p = new PaymentDetailsDTO("1111222211112222","Name","1","123","01","30");
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1","Name","","000","C","City","Addr","00000");
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);

        String buyerToken = fixtures.generateRegisteredUserSession("emptyBuyer", "PwdE");
        // Buyer’s cart is empty, so this should fail:
        Response<Order> resp = orderService.buyCartContent(buyerToken, p, s);
        assertFalse(resp.isOk(), "Cannot buy when cart is empty");
        // Verify order history is still empty after failed purchase
        Response<List<Order>> historyResp = orderService.viewPersonalOrderHistory(buyerToken);
        assertTrue(historyResp.isOk(), "viewPersonalOrderHistory should succeed");
        assertTrue(historyResp.getData().isEmpty(), "Order history should be empty after failed purchase");
    }

    @Test
    public void rateShop_BeforePurchase_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerY", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "RatingShopY");
        String buyerToken = fixtures.generateRegisteredUserSession("buyerY", "PwdY");
        // Buyer never purchased anything, so rating should be rejected:
        Response<Void> resp = shopService.rateShop(buyerToken, shop.getId(), 3);
        assertFalse(resp.isOk(), "Should not be able to rate shop before buying");
        // Verify the shop's rating is still default (e.g., zero)
        Response<ShopDTO> shopInfoAfter = shopService.getShopInfo(buyerToken, shop.getId());
        assertTrue(shopInfoAfter.isOk(), "getShopInfo should succeed after failed rating");
        ShopDTO unchangedShop = shopInfoAfter.getData();
        assertEquals(0, unchangedShop.getRating(), "Shop rating should remain unchanged after failed attempt");
    }

    @Test
    public void rateItem_BeforePurchase_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("ownerZ", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "RatingShopZ");
        List<ItemDTO> items = shopService.showShopItems(ownerToken, shop.getId()).getData();
        int itemId = items.get(0).getItemID();
        String buyerToken = fixtures.generateRegisteredUserSession("buyerZ", "PwdZ");
        // Buyer never purchased item; rating should be rejected:
        Response<Void> resp = shopService.rateItem(buyerToken, shop.getId(), itemId, 4);
        assertFalse(resp.isOk(), "Should not be able to rate item before buying");
        // Verify the item's rating is still default (e.g., zero)
        Response<List<ItemDTO>> itemsAfter = shopService.showShopItems(ownerToken, shop.getId());
        assertTrue(itemsAfter.isOk(), "showShopItems should succeed after failed rating");
        List<ItemDTO> itemsList = itemsAfter.getData();
        ItemDTO unchanged = itemsList.stream()
            .filter(i -> i.getItemID() == itemId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Item not found after failed rating"));
        assertEquals(0, unchanged.getRating(), "Item rating should remain unchanged after failed attempt");
    }

    @Test
    public void removeItemFromCart_InvalidShopOrItem_ShouldFail() {
        // Arrange: create a shop and a buyer
        String ownerToken = fixtures.generateRegisteredUserSession("ownerR", "PwdR");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "RemovalShopR");
        int shopId = shop.getId();
        String buyerToken = fixtures.generateRegisteredUserSession("buyerR", "PwdR");

        // Act & Assert: attempt to remove from a non-existent shop
        Response<Void> removeInvalidShop = orderService.removeItemFromCart(buyerToken, 9999, 1);
        assertFalse(removeInvalidShop.isOk(), "Removing from an invalid shop should fail");

        // Act & Assert: attempt to remove an invalid item from a valid shop
        Response<Void> removeInvalidItem = orderService.removeItemFromCart(buyerToken, shopId, 18);
        assertFalse(removeInvalidItem.isOk(), "Removing a non-existent item should fail");

        // Verify cart is still empty
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartResp.isOk(), "checkCartContent should succeed after failed removal");
        assertTrue(cartResp.getData().isEmpty(), "Cart should remain empty after failed removal attempts");
    }

    @Test
    public void removeItemFromCart_BeforeAddingAny_ShouldFail() {
        // Arrange: create a shop and a buyer, but do not add anything to cart
        String ownerToken = fixtures.generateRegisteredUserSession("ownerEmptyR", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "EmptyRemovalShop");
        int shopId = shop.getId();
        String buyerToken = fixtures.generateRegisteredUserSession("buyerEmptyR", "Pwd1");

        // Act: attempt to remove an item before adding any
        Response<Void> removeResp = orderService.removeItemFromCart(buyerToken, shopId,
            shop.getItems().values().iterator().next().getItemID());
        assertFalse(removeResp.isOk(), "Removing an item from an empty cart should fail");

        // Verify cart remains empty
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartResp.isOk(), "checkCartContent should succeed");
        assertTrue(cartResp.getData().isEmpty(), "Cart should still be empty");
    }

    @Test
    public void filterItemsAllShops_InvalidRatingFilter_ShouldFail() {
        // Arrange: create a shop and items
        String ownerToken = fixtures.generateRegisteredUserSession("ownerF", "PwdF");
        fixtures.generateShopAndItems(ownerToken, "FilterShopF");

        // Act: create invalid rating filter
        HashMap<String,String> badFilters = new HashMap<>();
        badFilters.put("minRating", "notANumber");
        Response<List<ItemDTO>> resp = shopService.filterItemsAllShops(ownerToken, badFilters);

        // Assert: should fail
        assertFalse(resp.isOk(), "filterItemsAllShops should fail with invalid rating filter");
    }

    // TODO:
    @Test
    public void checkoutWithInsufficientPaymentBalance_ShouldFailAndLeaveCartIntact() {
        // Arrange: mock payment failure, valid shipment, and create shop/item
        PaymentDetailsDTO p = new PaymentDetailsDTO("1111222211112222","Name","1","123","01","30");
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1","Name","","000","C","City","Addr","00000");
        fixtures.mockNegativePayment(p);
        fixtures.mockPositiveShipment(s);

        String ownerToken = fixtures.generateRegisteredUserSession("ownerPay", "PwdPay");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PayShop");
        int shopId = shop.getId();
        ItemDTO toBuy = shopService.showShopItems(ownerToken, shopId).getData().get(0);

        String buyerToken = fixtures.generateRegisteredUserSession("buyerPay", "PwdPay");
        // Add item to cart
        assertTrue(orderService.addItemToCart(buyerToken, shopId, toBuy.getItemID(), 1).isOk(),
                   "addItemToCart should succeed");

        // Act: attempt checkout
        Response<Order> checkoutResp = orderService.buyCartContent(buyerToken, p, s);
        //assertFalse(checkoutResp.isOk(), "buyCartContent should fail due to payment failure");

        // Assert: cart still contains the item
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        assertTrue(cartResp.isOk(), "checkCartContent should succeed after failed checkout");
        //assertEquals(1, cartResp.getData().size(), "Cart should still have the item after failed checkout");

        // Assert: order history remains empty
        Response<List<Order>> historyResp = orderService.viewPersonalOrderHistory(buyerToken);
        assertTrue(historyResp.isOk(), "viewPersonalOrderHistory should succeed");
        assertTrue(historyResp.getData().isEmpty(), "Order history should remain empty after failed checkout");
    }
    
    @Test
    public void testCartPersistenceAcrossSessions() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        ItemDTO item = shopItems.get(0);
        
        // 2) Buyer setup - register a user (not just guest)
        Response<String> guestResp = userService.enterToSystem();
        String guestToken = guestResp.getData();
        userService.registerUser(guestToken, "persistence", "pass123", LocalDate.now().minusYears(25));
        Response<String> loginResp = userService.loginUser(guestToken, "persistence", "pass123");
        String buyerToken = loginResp.getData();
        
        // 3) Add item to cart
        Response<Void> addResp = orderService.addItemToCart(
            buyerToken,
            shop.getId(),
            item.getItemID(),
            2
        );
        assertTrue(addResp.isOk(), "Adding item to cart should succeed");
        
        // Verify cart has the item
        Response<List<ItemDTO>> cartBeforeLogout = orderService.checkCartContent(buyerToken);
        assertEquals(1, cartBeforeLogout.getData().size(), "Cart should have one item before logout");
        
        // 4) Logout
        Response<String> logoutResp = userService.logoutRegistered(buyerToken);
        assertTrue(logoutResp.isOk(), "Logout should succeed");
        
        // 5) Login again with fresh token
        Response<String> newGuestResp = userService.enterToSystem();
        String newGuestToken = newGuestResp.getData();
        Response<String> reloginResp = userService.loginUser(newGuestToken, "persistence", "pass123");
        String newBuyerToken = reloginResp.getData();
        
        // 6) Verify cart contents persisted
        Response<List<ItemDTO>> cartAfterLogin = orderService.checkCartContent(newBuyerToken);
        assertTrue(cartAfterLogin.isOk(), "Cart check after re-login should succeed");
        
        List<ItemDTO> persistedCart = cartAfterLogin.getData();
        assertEquals(1, persistedCart.size(), "Cart should still have one item after re-login");
        assertEquals(item.getItemID(), persistedCart.get(0).getItemID(), "Item ID should be preserved");
        assertEquals(2, persistedCart.get(0).getQuantity(), "Item quantity should be preserved");
    }

    @Test
    public void testPriceCalculationVerification() {
        // 1) Owner setup
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken,shop.getId()).getData();
        
        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "Pwd0");
        
        // 3) Add multiple items with different quantities
        ItemDTO item1 = shopItems.get(0);
        ItemDTO item2 = shopItems.get(1);

        // Ensure sufficient quantity exists for each item
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), item1.getItemID(), 5);  
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), item2.getItemID(), 5);
        
        Response<Void> add1 = orderService.addItemToCart(buyerToken, shop.getId(), item1.getItemID(), 2);
        Response<Void> add2 = orderService.addItemToCart(buyerToken, shop.getId(), item2.getItemID(), 3);
        assertTrue(add1.isOk() && add2.isOk(), "Adding items to cart should succeed");
        
        // 4) Calculate expected total
        double expectedTotal = (item1.getPrice() * 2) + (item2.getPrice() * 3);
        
        // 5) Get cart for verification
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        List<ItemDTO> cart = cartResp.getData();
        
        // Calculate actual total from cart
        double actualTotal = cart.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        
        // Verify calculations match
        assertEquals(expectedTotal, actualTotal, 0.001, "Cart total calculation should be accurate");
        
        // 6) Complete purchase and verify order total
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO(
            "1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345"
        );
        
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        
        Response<Order> orderResp = orderService.buyCartContent(buyerToken, p, s);
        assertTrue(orderResp.isOk(), "Purchase should succeed");
        Order order = orderResp.getData();
        
        // Verify order total matches expected total
        assertEquals(expectedTotal, order.getTotalPrice(), 0.001, "Order total should match calculated total");
        
        // Verify payment was processed with correct amount
        //verify(payment).processPayment(expectedTotal, p);
    }

    @Test
    public void testMultiShopOrderHandling() {
        // 1) Create two different shops with items
        String owner1Token = fixtures.generateRegisteredUserSession("Owner1", "Pwd1");
        ShopDTO shop1 = fixtures.generateShopAndItems(owner1Token, "Shop1");
        
        String owner2Token = fixtures.generateRegisteredUserSession("Owner2", "Pwd2");
        ShopDTO shop2 = fixtures.generateShopAndItems(owner2Token, "Shop2");
        
        // Get items from both shops
        List<ItemDTO> shop1Items = shopService.showShopItems(owner1Token, shop1.getId()).getData();
        List<ItemDTO> shop2Items = shopService.showShopItems(owner2Token, shop2.getId()).getData();
        
        // 2) Buyer setup
        String buyerToken = fixtures.generateRegisteredUserSession("multiShopBuyer", "Pwd0");
        
        // 3) Add items from both shops to cart
        Response<Void> add1 = orderService.addItemToCart(
            buyerToken, shop1.getId(), shop1Items.get(0).getItemID(), 1
        );
        Response<Void> add2 = orderService.addItemToCart(
            buyerToken, shop2.getId(), shop2Items.get(0).getItemID(), 1
        );
        
        assertTrue(add1.isOk() && add2.isOk(), "Adding items from multiple shops should succeed");
        
        // 4) Verify cart has items from both shops
        Response<List<ItemDTO>> cartResp = orderService.checkCartContent(buyerToken);
        List<ItemDTO> cartItems = cartResp.getData();
        assertEquals(2, cartItems.size(), "Cart should have items from both shops");
        
        // 5) Complete purchase
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1", "123", "12", "25"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO(
            "1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345"
        );
        
        fixtures.mockPositivePayment(p);
        fixtures.mockPositiveShipment(s);
        
        Response<Order> orderResp = orderService.buyCartContent(buyerToken, p, s);
        assertTrue(orderResp.isOk(), "Multi-shop purchase should succeed");
        Order order = orderResp.getData();
        
        // 6) Verify order contains items from both shops
        assertEquals(2, order.getItems().size(), "Order should contain items from both shops");
        
        // 7) Verify inventory updated in both shops
        Response<ShopDTO> shop1AfterResp = shopService.getShopInfo(owner1Token, shop1.getId());
        Response<ShopDTO> shop2AfterResp = shopService.getShopInfo(owner2Token, shop2.getId());
        
        ShopDTO shop1After = shop1AfterResp.getData();
        ShopDTO shop2After = shop2AfterResp.getData();
        
        // Find the items and verify quantity decreased
        ItemDTO shop1ItemAfter = shop1After.getItems().values().stream()
            .filter(i -> i.getItemID() == shop1Items.get(0).getItemID())
            .findFirst()
            .orElseThrow();
            
        ItemDTO shop2ItemAfter = shop2After.getItems().values().stream()
            .filter(i -> i.getItemID() == shop2Items.get(0).getItemID())
            .findFirst()
            .orElseThrow();
            
        assertEquals(shop1Items.get(0).getQuantity() - 1, shop1ItemAfter.getQuantity(), 
            "Shop1 item quantity should be decremented");
        assertEquals(shop2Items.get(0).getQuantity() - 1, shop2ItemAfter.getQuantity(), 
            "Shop2 item quantity should be decremented");
    }

    @Test
    public void testCartPersistenceFromGuestToRegistered() {
        // 1) Owner setup and shop creation
        String ownerToken = fixtures.generateRegisteredUserSession("OwnerPersist", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "PersistShop");
        List<ItemDTO> shopItems = shopService.showShopItems(ownerToken, shop.getId()).getData();
        
        // 2) Guest enters the system
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest should enter system successfully");
        String guestToken = guestResp.getData();
        
        // 3) Guest adds items to cart
        ItemDTO item1 = shopItems.get(0);
        ItemDTO item2 = shopItems.get(1);
        
        Response<Void> add1 = orderService.addItemToCart(guestToken, shop.getId(), item1.getItemID(), 2);
        Response<Void> add2 = orderService.addItemToCart(guestToken, shop.getId(), item2.getItemID(), 1);
        assertTrue(add1.isOk() && add2.isOk(), "Adding items to guest cart should succeed");
        
        // 4) Verify items are in guest's cart
        Response<List<ItemDTO>> guestCartResp = orderService.checkCartContent(guestToken);
        assertTrue(guestCartResp.isOk(), "Checking guest cart should succeed");
        List<ItemDTO> guestCartItems = guestCartResp.getData();
        assertEquals(2, guestCartItems.size(), "Guest cart should contain 2 items");
        
        // 5) Test both registration and login scenarios
        
        // 5a) REGISTRATION SCENARIO - register the guest as a new user
        userService.registerUser(guestToken, "cartPersistUser", "Pass123", LocalDate.now().minusYears(25));
        
        // Verify cart persists after registration
        Response<List<ItemDTO>> registeredCartResp = orderService.checkCartContent(guestToken);
        assertTrue(registeredCartResp.isOk(), "Checking registered user cart should succeed");
        List<ItemDTO> registeredCartItems = registeredCartResp.getData();
        assertEquals(2, registeredCartItems.size(), "Registered user cart should still contain 2 items");
        
        // Verify item details are preserved
        boolean foundItem1 = false;
        boolean foundItem2 = false;
        
        for (ItemDTO cartItem : registeredCartItems) {
            if (cartItem.getItemID() == item1.getItemID()) {
                foundItem1 = true;
                assertEquals(2, cartItem.getQuantity(), "Item1 quantity should be preserved");
            } else if (cartItem.getItemID() == item2.getItemID()) {
                foundItem2 = true;
                assertEquals(1, cartItem.getQuantity(), "Item2 quantity should be preserved");
            }
        }
        assertTrue(foundItem1 && foundItem2, "Both items should be found in the registered user's cart");
        
        // 5b) LOGIN SCENARIO - Create a separate guest, add items, then login as existing user
        Response<String> guest2Resp = userService.enterToSystem();
        String guest2Token = guest2Resp.getData();
        
        shopService.changeItemQuantityInShop(ownerToken, shop.getId(), shopItems.get(2).getItemID(), 5);
        
        // Guest2 adds an item to cart
        Response<Void> guest2Add = orderService.addItemToCart(guest2Token, shop.getId(), shopItems.get(2).getItemID(), 3);
        assertTrue(guest2Add.isOk(), "Adding item to second guest cart should succeed");
        
        // Verify guest2's cart
        Response<List<ItemDTO>> guest2CartResp = orderService.checkCartContent(guest2Token);
        assertEquals(1, guest2CartResp.getData().size(), "Guest2 cart should contain 1 item");
        
        // Login as the previously registered user
        Response<String> loginResp = userService.loginUser(guest2Token, "cartPersistUser", "Pass123");
        assertTrue(loginResp.isOk(), "Login should succeed");
        String loggedInToken = loginResp.getData();
        
        // Verify cart contents after login (should now have the registered user's cart, not guest2's cart)
        Response<List<ItemDTO>> loggedInCartResp = orderService.checkCartContent(loggedInToken);
        assertTrue(loggedInCartResp.isOk(), "Checking logged in cart should succeed");
        List<ItemDTO> loggedInCartItems = loggedInCartResp.getData();
        
        // Should have the registered user's items (2 items), not guest2's items (1 item)
        assertEquals(2, loggedInCartItems.size(), "After login, cart should contain the registered user's 2 items");
        
        // Verify same items as before are still in the cart
        foundItem1 = false;
        foundItem2 = false;
        
        for (ItemDTO cartItem : loggedInCartItems) {
            if (cartItem.getItemID() == item1.getItemID()) {
                foundItem1 = true;
                assertEquals(2, cartItem.getQuantity(), "Item1 quantity should be preserved after login");
            } else if (cartItem.getItemID() == item2.getItemID()) {
                foundItem2 = true;
                assertEquals(1, cartItem.getQuantity(), "Item2 quantity should be preserved after login");
            }
        }
        assertTrue(foundItem1 && foundItem2, "Both original items should be found after login");
    }

    // @Test
    // public void testEqualBidAmount_OnlyOneAccepted() throws InterruptedException, ExecutionException {
    //     // 1) Owner and shop setup with an auction on one item
    //     String ownerToken = fixtures.generateRegisteredUserSession("ownerEq", "Pwd0");
    //     ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "EqShop");
    //     int shopId = shop.getId();
    //     int itemId = shop.getItems().values().iterator().next().getItemID();
    //     LocalDateTime start = LocalDateTime.now().plusSeconds(1);
    //     LocalDateTime end = LocalDateTime.now().plusSeconds(20);
    //     shopService.openAuction(ownerToken, shopId, itemId, 10.0, start, end);

    //     Thread.sleep(1100);

    //     int auctionId = shopRepository.getShopById(shopId).getActiveAuctions().get(0).getId();

    //     // ensure auction is fully active before concurrent bids
    //     Thread.sleep(50);
        

    //     // 2) Two bidders enter the system
    //     String bidder1 = fixtures.generateRegisteredUserSession("bidderEq1", "pwd1");
    //     String bidder2 = fixtures.generateRegisteredUserSession("bidderEq2", "pwd2");

    //     double bidAmount = 20.0;

    //     // 3) Concurrently submit bids with the same amount
    //     ExecutorService executor = Executors.newFixedThreadPool(2);
    //     Callable<Response<Void>> task1 = () -> orderService.submitAuctionOffer(bidder1, shopId, auctionId, bidAmount);
    //     Callable<Response<Void>> task2 = () -> orderService.submitAuctionOffer(bidder2, shopId, auctionId, bidAmount);

    //     List<Future<Response<Void>>> futures = executor.invokeAll(List.of(task1, task2));
    //     executor.shutdown();

    //     boolean firstOk = futures.get(0).get().isOk();
    //     boolean secondOk = futures.get(1).get().isOk();

    //     // 4) Verify that exactly one bid succeeded and one failed
    //     assertTrue(firstOk ^ secondOk, "Exactly one bid should be accepted when both bids are equal");
    // }
}