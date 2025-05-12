package Tests.AcceptanceTests;

import Service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import Domain.Shop.*;
import Domain.User.Registered;
import Domain.Response;
import Domain.Adapters_and_Interfaces.*;
import Domain.DTOs.ItemDTO;
import Domain.DTOs.Order;
import Domain.DTOs.ShopDTO;
public class AcceptanceTestFixtures {
    private final UserService userService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final IPayment payment;
    private final IShipment shipment;

    public AcceptanceTestFixtures(UserService userService,
                                  ShopService shopService,
                                  OrderService orderService,
                                  IPayment payment,
                                  IShipment shipment) {
        this.userService  = userService;
        this.shopService  = shopService;
        this.orderService = orderService;
        this.payment      = payment;
        this.shipment     = shipment;
    }

    public String generateRegisteredUserSession(String name, String password) {
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Owner enterToSystem should succeed");
        String userToken = guestResp.getData();
        assertNotNull(userToken, "Owner guest token must not be null");

        // User registers
        Response<Void> ownerReg = userService.registerUser(
            userToken, name, password, LocalDate.now().minusYears(30)
        );
        assertTrue(ownerReg.isOk(), "Owner registration should succeed");

        // User logs in
        Response<String> ownerLogin = userService.loginUser(
            userToken, name, password
        );
        assertTrue(ownerLogin.isOk(), "Owner login should succeed");
        String ownerToken = ownerLogin.getData();
        assertNotNull(ownerToken, "Owner token must not be null");
        return ownerToken;
    }

    public String generateSystemManagerSession(String name, String password) {
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "System manager enterToSystem should succeed");
        String userToken = guestResp.getData();
        assertNotNull(userToken, "System manager guest token must not be null");

        // User registers
        Response<Void> systemManagerReg = userService.registerUser(
            userToken, name, password, LocalDate.now().minusYears(30)
        );
        assertTrue(systemManagerReg.isOk(), "System manager registration should succeed");

        // User logs in
        Response<String> systemManagerLogin = userService.loginUser(
            userToken, name, password
        );
        assertTrue(systemManagerLogin.isOk(), "System manager login should succeed");
        String systemManagerToken = systemManagerLogin.getData();
        assertNotNull(systemManagerToken, "System manager token must not be null");
        return systemManagerToken;
    }

    public ShopDTO generateShopAndItems(String ownerToken) {
        // 1) Owner creates the shop
        Response<ShopDTO> shopResp = shopService.createShop(
            ownerToken, 
            "MyShop", 
            "A shop for tests"
        );
        assertTrue(shopResp.isOk(), "Shop creation should succeed");
        ShopDTO shop = shopResp.getData();
        assertNotNull(shop, "Returned ShopDTO must not be null");
        int shopId = shop.getId();

        // 2) Owner adds three items
        Response<ItemDTO> addA = shopService.addItemToShop(
            ownerToken, shopId,
            "Apple", Category.FOOD, 1.00, "fresh apple"
        );
        Response<ItemDTO> addB = shopService.addItemToShop(
            ownerToken, shopId,
            "Banana", Category.FOOD, 0.50, "ripe banana"
        );
        Response<ItemDTO> addL = shopService.addItemToShop(
            ownerToken, shopId,
            "Laptop", Category.ELECTRONICS, 999.99, "new laptop"
        );
        assertTrue(addA.isOk(), "Adding Apple should succeed");
        assertTrue(addB.isOk(), "Adding Banana should succeed");
        assertTrue(addL.isOk(), "Adding Laptop should succeed");

        // 3) (Optional) bump quantities or prices if you like:
        //    here we just set Apple's stock to 10 as an example
        //    first fetch its ID
        Response<ShopDTO> infoResp = shopService.getShopInfo(ownerToken, shopId);
        assertTrue(infoResp.isOk(), "getShopInfo should succeed");
        Map<Integer,ItemDTO> map = infoResp.getData().getItems();
        assertEquals(3, map.size(), "Shop should contain exactly 3 items");
        int appleId = map.values().stream()
                        .filter(i -> i.getName().equals("Apple"))
                        .findFirst()
                        .get()
                        .getItemID();
        int bananaId = map.values().stream()
                        .filter(i -> i.getName().equals("Banana"))
                        .findFirst()
                        .get()
                        .getItemID();
        int laptopId = map.values().stream()
                        .filter(i -> i.getName().equals("Laptop"))
                        .findFirst()
                        .get()
                        .getItemID();
        Response<Void> chgQty1 = shopService.changeItemQuantityInShop(
            ownerToken, shopId, appleId, 10
        );
        Response<Void> chgQty2 = shopService.changeItemQuantityInShop(
            ownerToken, shopId, bananaId, 1
        );
        Response<Void> chgQty3 = shopService.changeItemQuantityInShop(
            ownerToken, shopId, laptopId, 0
        );
        assertTrue(chgQty1.isOk(), "changeItemQuantityInShop should succeed");
        assertTrue(chgQty2.isOk(), "changeItemQuantityInShop should succeed");
        assertTrue(chgQty3.isOk(), "changeItemQuantityInShop should succeed");

        // 4) Retrieve final list and return it
        Response<List<ItemDTO>> finalResp = shopService.showShopItems(ownerToken,shopId);
        assertTrue(finalResp.isOk(), "showShopItems should succeed");
        List<ItemDTO> items = finalResp.getData();
        assertNotNull(items, "Returned item list must not be null");
        assertEquals(3, items.size(), "There should be 3 items in the shop");

        return shopService.getShopInfo(ownerToken, shopId).getData();
    }

    public Order successfulBuyCartContent(String sessionToken) {
        mockPositivePayment();
        mockPositiveShipment();

        Response<Order> purchaseResp = orderService.buyCartContent(
            sessionToken
        );
        assertTrue(purchaseResp.isOk(), "buyCartContent should succeed");
        Order created = purchaseResp.getData();
        assertNotNull(created, "Returned Order must not be null");
        List<ItemDTO> cartItems = orderService.checkCartContent(sessionToken).getData();
        assertEquals(0, cartItems.size(), "Cart should be empty after purchase");
        return created;
    }

    public void mockPositivePayment() {
        when(payment.validatePaymentDetails()).thenReturn(true);
        when(payment.processPayment(1.0)).thenReturn(true);
    }
    public void mockPositiveShipment() {
        when(shipment.validateShipmentDetails()).thenReturn(true);
        when(shipment.processShipment(0.1)).thenReturn(true);
    }
    public void mockNegativePayment() {
        when(payment.validatePaymentDetails()).thenReturn(false);
        when(payment.processPayment(1.0)).thenReturn(false);
    }
    public void mockNegativeShipment() {
        when(shipment.validateShipmentDetails()).thenReturn(false);
        when(shipment.processShipment(0.1)).thenReturn(false);
    }
}

