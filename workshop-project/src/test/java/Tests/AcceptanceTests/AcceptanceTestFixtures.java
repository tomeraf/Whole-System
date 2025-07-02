package Tests.AcceptanceTests;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IExternalSystems;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;

import com.halilovindustries.backend.Domain.Shop.*;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.OfferMessage;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
public class AcceptanceTestFixtures {
    private final UserService userService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final IPayment payment;
    private final IShipment shipment;
    private final IExternalSystems externalSystems;

    public AcceptanceTestFixtures(UserService userService,
                                  ShopService shopService,
                                  OrderService orderService,
                                  IPayment payment,
                                  IShipment shipment, IExternalSystems externalSystems) {
        this.externalSystems = externalSystems;
        this.userService  = userService;
        this.shopService  = shopService;
        this.orderService = orderService;
        this.payment      = payment;
        this.shipment     = shipment;
    }

    public String generateRegisteredUserSession(String name, String password) {
        // Create a unique username for each test run to avoid conflicts
        String uniqueName = name;// + "_" + System.currentTimeMillis();
        
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest entry should succeed");
        String guestToken = guestResp.getData();
        
        Response<Void> regRes = userService.registerUser(guestToken, uniqueName, password, LocalDate.now().minusYears(25));
        assertTrue(regRes.isOk(), "User registration should succeed");
        
        Response<String> loginRes = userService.loginUser(guestToken, uniqueName, password);
        assertTrue(loginRes.isOk(), "Login should succeed");
        
        return loginRes.getData();
    }

    public String registerUserWithoutLogin(String name, String password) {
        // Create a unique username for each test run
        String uniqueName = name;// + "_" + System.currentTimeMillis();
        Response<String> guestResp = userService.enterToSystem();
        assertTrue(guestResp.isOk(), "Guest entry should succeed");
        String guestToken = guestResp.getData();
        assertNotNull(guestToken, "Guest token must not be null");

        Response<Void> regRes = userService.registerUser(
                guestToken, uniqueName, password, LocalDate.now().minusYears(25)
        );
        assertTrue(regRes.isOk(), "User registration should succeed");

        return guestToken; // Still a guest token, since user is not logged in
    }

    public String loginUser(String guestToken, String username, String password) {
        Response<String> loginRes = userService.loginUser(guestToken, username, password);
        assertTrue(loginRes.isOk(), "Login should succeed");
        String userToken = loginRes.getData();
        assertNotNull(userToken, "User token must not be null after login");
        return userToken;
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

    public ShopDTO generateShopAndItems(String ownerToken,String name) {
        // 1) Owner creates the shop
        Response<ShopDTO> shopResp = shopService.createShop(
            ownerToken, 
            name, 
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

    public Order successfulBuyCartContent(String sessionToken, PaymentDetailsDTO p, ShipmentDetailsDTO s) {
        mockPositivePayment(p);
        mockPositiveShipment(s);

        Response<Order> purchaseResp = orderService.buyCartContent(
            sessionToken, p, s
        );
        assertTrue(purchaseResp.isOk(), "buyCartContent should succeed");
        Order created = purchaseResp.getData();
        assertNotNull(created, "Returned Order must not be null");
        List<ItemDTO> cartItems = orderService.checkCartContent(sessionToken).getData();
        assertEquals(0, cartItems.size(), "Cart should be empty after purchase");
        return created;
    }

    public void mockPositivePayment(PaymentDetailsDTO details) {
        when(payment.validatePaymentDetails(details)).thenReturn(true);
        when(payment.processPayment(anyDouble(), eq(details))).thenReturn(12345);
        when(externalSystems.handshake()).thenReturn(true);
    }

    public void mockPositiveShipment(ShipmentDetailsDTO details) {
        when(shipment.validateShipmentDetails(details)).thenReturn(true);
        when(shipment.processShipment(details)).thenReturn(67890); // or any valid transaction ID
    }

    public void mockNegativePayment(PaymentDetailsDTO details) {
        when(payment.validatePaymentDetails(details)).thenReturn(false);
        when(payment.processPayment(1.0, details)).thenReturn(null); // null = failure
        when(externalSystems.handshake()).thenReturn(false);
    }

    public void mockNegativeShipment(ShipmentDetailsDTO details) {
        when(shipment.validateShipmentDetails(details)).thenReturn(false);
        when(shipment.processShipment(details)).thenReturn(null); // null = failure
    }

    public void successfulAddOwner(String appointerToken, String appointeeToken, int shopID, String shopName) {
        Response<Void> offer = shopService.sendOwnershipOffer(appointerToken, shopID, userService.getUsername(appointeeToken));
        assertTrue(offer.isOk(), "Ownership offer should succeed");
        Response<List<Message>> messages = userService.getInbox(appointeeToken);
        assertTrue(messages.isOk(), "Inbox retrieval should succeed");
        List<Message> inbox = messages.getData();
        assertNotNull(inbox, "Inbox must not be null");
        assertTrue(inbox.size() > 0, "Inbox should contain at least one message");
        List<Message> msges = inbox.stream().filter(m -> m.isOffer()).toList();
        assertTrue(msges.size() > 0, "Inbox should contain at least one ownership offer message");
        OfferMessage offerMessage = (OfferMessage)msges.get(msges.size() - 1);
        assertTrue(offerMessage.getAppointerName().equals(userService.getUsername(appointerToken)), "Offer message should have correct appointer name");
        assertTrue(offerMessage.getAppointeeName().equals(userService.getUsername(appointeeToken)), "Offer message should have correct appointee name");
        assertTrue(offerMessage.getShopName().equals(shopName), "Offer message should have correct shop name");
        int msgId = offerMessage.getShopName().equals(shopName) ? offerMessage.getId() : -1;
        Response<Void> accept = shopService.answerAppointmentOffer(appointeeToken, shopName, msgId, true);
        assertTrue(accept.isOk(), "Ownership acceptance should succeed");
        boolean isInShop = shopService.getShopMembers(appointerToken, shopID).getData().stream().filter(user -> user.getUsername().equals(userService.getUsername(appointeeToken))).toList().size() == 1;
        assertTrue(isInShop, "Appointee should be in the shop members list after successful appointment");   
    }

    public void successfulAddManager(String appointerToken, String appointeeToken, int shopID, String shopName, Set<Permission> perms) {
        Response<Void> offer = shopService.sendManagementOffer(appointerToken, shopID, userService.getUsername(appointeeToken), perms);
        assertTrue(offer.isOk(), "Ownership offer should succeed");
        Response<List<Message>> messages = userService.getInbox(appointeeToken);
        assertTrue(messages.isOk(), "Inbox retrieval should succeed");
        List<Message> inbox = messages.getData();
        assertNotNull(inbox, "Inbox must not be null");
        assertTrue(inbox.size() > 0, "Inbox should contain at least one message");
        List<Message> msges = inbox.stream().filter(m -> m.isOffer()).toList();
        assertTrue(msges.size() > 0, "Inbox should contain at least one ownership offer message");
        OfferMessage offerMessage = (OfferMessage)msges.get(msges.size() - 1);
        assertTrue(offerMessage.getAppointerName().equals(userService.getUsername(appointerToken)), "Offer message should have correct appointer name");
        assertTrue(offerMessage.getAppointeeName().equals(userService.getUsername(appointeeToken)), "Offer message should have correct appointee name");
        assertTrue(offerMessage.getShopName().equals(shopName), "Offer message should have correct shop name");
        int msgId = offerMessage.getShopName().equals(shopName) ? offerMessage.getId() : -1;
        Response<Void> accept = shopService.answerAppointmentOffer(appointeeToken, shopName, msgId, true);
        assertTrue(accept.isOk(), "Ownership acceptance should succeed");
        boolean isInShop = shopService.getShopMembers(appointerToken, shopID).getData().stream().filter(user -> user.getUsername().equals(userService.getUsername(appointeeToken))).toList().size() == 1;
        assertTrue(isInShop, "Appointee should be in the shop members list after successful appointment");
    }
}

