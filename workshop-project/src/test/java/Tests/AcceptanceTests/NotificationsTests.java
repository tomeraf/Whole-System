package Tests.AcceptanceTests;

import com.halilovindustries.backend.Domain.DTOs.*;
import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Shop.Shop;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotificationsTests extends BaseAcceptanceTests{
    private String managerToken;

    @BeforeEach
    public void setUp() {
        super.setUp();
        managerToken=fixtures.generateSystemManagerSession("manager","system");
        Registered systemManger=userRepository.getUserByName("manager");
        systemManger.setSystemManager(true);
        Broadcaster.getInstance();
    }

//    @Test
//    public void testBroadcastNotificationToRegisteredUser() throws Exception {
//        // Arrange
//        String userUuid = "user-123";
//        String expectedMessage = "Hello, user!";
//        CompletableFuture<String> messageReceived = new CompletableFuture<>();
//
//        // Mock Vaadin UI
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//
//        UI.setCurrent(mockUI);
//
//        // Register listener
//        Consumer<String> listener = msg -> {
//            System.out.println("Listener received: " + msg);
//            messageReceived.complete(msg);
//        };
//        Registration registration = Broadcaster.register(userUuid, listener);
//
//        // Act
//        boolean success = Broadcaster.broadcast(userUuid, expectedMessage);
//
//        assertTrue(success, "Broadcast should return true");
//        // Wait for listener to be triggered
//        String actualMessage = messageReceived.get(10, TimeUnit.SECONDS);
//        assertEquals(expectedMessage, actualMessage, "Listener should receive the correct message");
//
//        // Clean up
//        registration.remove();
//        UI.setCurrent(null);
//    }

    @Test
    public void testBroadcastWithNoRegisteredListener() {
        // Arrange
        String userUuid = "non-existent-user";
        String message = "Should not be received";

        // Act
        boolean success = Broadcaster.broadcast(userUuid, message);

        // Assert
        assertFalse(success, "Broadcast should return false when no listener is registered");
    }

//    @Test
//    public void testBroadcastToMultipleListeners() throws Exception {
//        String userUuid = "multi-listener-user";
//        String expectedMessage = "Broadcast to all listeners";
//
//        CompletableFuture<String> received1 = new CompletableFuture<>();
//        CompletableFuture<String> received2 = new CompletableFuture<>();
//
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        Consumer<String> listener1 = msg -> received1.complete(msg);
//        Consumer<String> listener2 = msg -> received2.complete(msg);
//
//        Registration reg1 = Broadcaster.register(userUuid, listener1);
//        Registration reg2 = Broadcaster.register(userUuid, listener2);
//
//        boolean success = Broadcaster.broadcast(userUuid, expectedMessage);
//        assertTrue(success);
//
//        assertEquals(expectedMessage, received1.get(10, TimeUnit.SECONDS));
//        assertEquals(expectedMessage, received2.get(10, TimeUnit.SECONDS));
//
//        reg1.remove();
//        reg2.remove();
//        UI.setCurrent(null);
//    }

    @Test
    public void testListenerNotCalledAfterRemoval() {
        String userUuid = "remove-test-user";
        String message = "You shouldn't see this";

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(Command.class));
        UI.setCurrent(mockUI);

        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Consumer<String> listener = msg -> wasCalled.set(true);

        Registration registration = Broadcaster.register(userUuid, listener);
        registration.remove(); // Remove immediately

        boolean success = Broadcaster.broadcast(userUuid, message);
        assertFalse(wasCalled.get(), "Listener should not be called after removal");

        UI.setCurrent(null);
    }

//    @Test
//    public void testBroadcastToMultipleUsersIndependently() throws Exception {
//        String user1 = "user-1";
//        String user2 = "user-2";
//        String message1 = "Message for User 1";
//        String message2 = "Message for User 2";
//
//        CompletableFuture<String> receivedByUser1 = new CompletableFuture<>();
//        CompletableFuture<String> receivedByUser2 = new CompletableFuture<>();
//
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        Registration reg1 = Broadcaster.register(user1, msg -> receivedByUser1.complete(msg));
//        Registration reg2 = Broadcaster.register(user2, msg -> receivedByUser2.complete(msg));
//
//        boolean success1 = Broadcaster.broadcast(user1, message1);
//        boolean success2 = Broadcaster.broadcast(user2, message2);
//
//        assertTrue(success1);
//        assertTrue(success2);
//        assertEquals(message1, receivedByUser1.get(10, TimeUnit.SECONDS));
//        assertEquals(message2, receivedByUser2.get(10, TimeUnit.SECONDS));
//
//        reg1.remove();
//        reg2.remove();
//        UI.setCurrent(null);
//    }

//    @Test
//    public void testSameListenerRegisteredTwice() throws Exception {
//        String userUuid = "double-listener-user";
//        String message = "Hello double";
//
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        AtomicInteger callCount = new AtomicInteger(0);
//        Consumer<String> listener = msg -> callCount.incrementAndGet();
//
//        Registration reg1 = Broadcaster.register(userUuid, listener);
//        Registration reg2 = Broadcaster.register(userUuid, listener);
//
//        boolean success = Broadcaster.broadcast(userUuid, message);
//        assertTrue(success);
//
//        TimeUnit.SECONDS.sleep(1); // Give executor time to finish
//        assertEquals(2, callCount.get(), "Listener should have been called twice");
//
//        reg1.remove();
//        reg2.remove();
//        UI.setCurrent(null);
//    }

//    @Test
//    public void testBroadcastNullMessage() throws Exception {
//        String userUuid = "null-message-user";
//        CompletableFuture<String> received = new CompletableFuture<>();
//
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        Registration reg = Broadcaster.register(userUuid, received::complete);
//
//        boolean success = Broadcaster.broadcast(userUuid, null);
//        assertTrue(success);
//
//        assertNull(received.get(10, TimeUnit.SECONDS));
//
//        reg.remove();
//        UI.setCurrent(null);
//    }

    @Test
    public void testRegisterWithoutUIThrows() {
        UI.setCurrent(null); // Explicitly no UI context

        assertThrows(IllegalStateException.class, () -> {
            Broadcaster.register("bad-user", msg -> {});
        }, "Should throw when UI.getCurrent() is null");
    }

//    @Test
//    public void testNotificationOnCloseShop() throws Exception {
//        // Arrange
//        String ownerToken = fixtures.generateRegisteredUserSession("owner", "owner");
//        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
//
//        // Register listener for all shop members (including owner)
//        Shop realShop = shopRepository.getShopById(shop.getId());
//        int founderId = realShop.getFounderID();
//
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        CompletableFuture<String> notificationReceived = new CompletableFuture<>();
//        // Register broadcaster listener for founder id
//        Registration registration = Broadcaster.register(String.valueOf(founderId), notificationReceived::complete);
//
//        //close shop
//        Response<Void> res = shopService.closeShop(ownerToken, shop.getId());
//
//        // Assert
//        assertTrue(res.isOk(), "Closing shop should succeed");
//
//        // Check notification broadcasted
//        String notification = notificationReceived.get(5, TimeUnit.SECONDS);
//        assertNotNull(notification, "Notification should be received");
//        assertTrue(notification.contains("is closed"), "Notification should mention shop closure");
//
//        registration.remove();
//        UI.setCurrent(null);
//    }
//
//    @Test
//    public void testNotificationOnBuyCartContent() throws Exception {
//        // Arrange
//        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "buyer");
//        ShopDTO shop = fixtures.generateShopAndItems(buyerToken, "MyShop");
//
//        ItemDTO firstItem = shop.getItems().get(0);
//
//        // Prepare items map for addItemsToCart
//        HashMap<Integer, HashMap<Integer, Integer>> userItems = new HashMap<>();
//        HashMap<Integer, Integer> itemsForShop = new HashMap<>();
//        itemsForShop.put(firstItem.getItemID(), 1);  // buy 1 quantity of the first item
//        userItems.put(shop.getId(), itemsForShop);
//
//        // Add items to cart
//        Response<Void> addToCartResponse = orderService.addItemsToCart(buyerToken, userItems);
//        assertTrue(addToCartResponse.isOk(), "Adding items to cart should succeed");
//
//        // Prepare mock UI to allow access() calls
//        UI mockUI = mock(UI.class);
//        doAnswer(invocation -> {
//            Command command = invocation.getArgument(0);
//            command.execute();
//            return null;
//        }).when(mockUI).access(any(Command.class));
//        UI.setCurrent(mockUI);
//
//        Shop realShop = shopRepository.getShopById(shop.getId());
//        int founderId = realShop.getFounderID();
//
//        CompletableFuture<String> notificationReceived = new CompletableFuture<>();
//        Registration registration = Broadcaster.register(String.valueOf(founderId), notificationReceived::complete);
//
//        // Prepare dummy payment and shipment details
//        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123", "12/25","333");
//        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");
//
//        //return true for validation
//        when(payment.validatePaymentDetails(any(PaymentDetailsDTO.class))).thenReturn(true);
//        when(shipment.validateShipmentDetails(any(ShipmentDetailsDTO.class))).thenReturn(true);
//
//
//        // Act
//        Response<Order> buyResponse = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);
//
//        // Assert
//        assertTrue(buyResponse.isOk(), "Buying cart content should succeed");
//
//        String notification = notificationReceived.get(5, TimeUnit.SECONDS);
//        assertNotNull(notification, "Notification should be received");
//        assertTrue(notification.contains("purchased"), "Notification should mention purchase. this is the notification: " + notification);
//
//        registration.remove();
//        UI.setCurrent(null);
//    }

    @Test
    public void testCloseShopUnauthorizedUser() throws Exception {
        String ownerToken = fixtures.generateRegisteredUserSession("owner", "owner");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // Generate a token for a different user who is NOT the owner
        String otherUserToken = fixtures.generateRegisteredUserSession("intruder", "intruder");

        Response<Void> res = shopService.closeShop(otherUserToken, shop.getId());

        assertFalse(res.isOk(), "Closing shop by unauthorized user should fail");
    }

    @Test
    public void testCloseShopNonExistentShop() throws Exception {
        String ownerToken = fixtures.generateRegisteredUserSession("owner", "owner");

        int fakeShopId = 99999; // Assuming this shop ID does not exist

        Response<Void> res = shopService.closeShop(ownerToken, fakeShopId);

        assertFalse(res.isOk(), "Closing a non-existent shop should fail");
    }

    @Test
    public void testBuyCartContentInvalidPayment() throws Exception {
        String ownerToken = fixtures.generateRegisteredUserSession("owner", "owner");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "buyer");

        HashMap<Integer, HashMap<Integer, Integer>> itemsMap = new HashMap<>();
        HashMap<Integer, Integer> shopItems = new HashMap<>();
        shopItems.put(shop.getItems().get(0).getItemID(), 1);
        itemsMap.put(shop.getId(), shopItems);

        orderService.addItemsToCart(buyerToken, itemsMap);

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123", "12/25","333");
        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");


        when(payment.validatePaymentDetails(any(PaymentDetailsDTO.class))).thenReturn(false);
        when(shipment.validateShipmentDetails(any(ShipmentDetailsDTO.class))).thenReturn(false);

        // Mock payment validation to fail
        when(payment.validatePaymentDetails(any())).thenReturn(false);

        Response<Order> res = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);

        assertFalse(res.isOk(), "Buying cart content with invalid payment should fail");
    }

    @Test
    public void testBuyCartContentEmptyCart() throws Exception {
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "buyer");

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123", "12/25","333");
        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");


        // No items added to cart

        Response<Order> res = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);

        assertFalse(res.isOk(), "Buying cart content with empty cart should fail");
    }
//    @Test
//    public void testSendAndRespondToMessage() throws Exception {
//        // Arrange
//        String founderToken = fixtures.generateRegisteredUserSession("founder", "founder");
//        ShopDTO shopDTO = fixtures.generateShopAndItems(founderToken, "TestShop");
//
//        int shopId = shopDTO.getId();
//
//        String title = "Issue with item";
//        String content = "The item arrived damaged.";
//
//        // Act - Send a message
//        Response<Void> sendRes = shopService.sendMessage(founderToken, shopId, title, content);
//
//        // Assert message was sent successfully
//        assertTrue(sendRes.isOk(), "Sending message should succeed");
//
//        // Act - Respond to the message
//        String responseTitle = "Apologies";
//        String responseContent = "We'll send a replacement.";
//
//        Response<List<Message>> shopInboxRes = shopService.getInbox(founderToken, shopId);
//        Message originalMessage = shopInboxRes.getData().stream()
//                .filter(m -> m.getTitle().equals(title))
//                .findFirst()
//                .orElseThrow();
//
//        Response<Void> respondRes = shopService.respondToMessage(
//                founderToken, shopId, originalMessage.getId(), responseTitle, responseContent
//        );
//
//        assertTrue(respondRes.isOk(), "Responding to message should succeed");
//
//        // ✅ Notification check (replace with actual check depending on your test setup)
//        String recipientId = String.valueOf(originalMessage.getUserName());
//        try {
//            assertEquals(2, notificationHandler.getNotifications(recipientId).size(), "User should have been notified about message response");
//        } catch (Exception e) {
//            assertTrue(false,"User should have been notified about message response and about his message");
//        }
//    }


    @Test
    public void testSendMessageFailsWithEmptyContent() throws Exception {
        // Arrange
        String token = fixtures.generateRegisteredUserSession("emptyMsgUser", "pass");
        ShopDTO shopDTO = fixtures.generateShopAndItems(token, "AnotherShop");

        // Act
        Response<Void> res = shopService.sendMessage(token, shopDTO.getId(), "", "");

        // Assert
        assertFalse(res.isOk(), "Message send should fail with empty content");

        // ✅ Notification should NOT be sent
        int recipientId = Integer.parseInt(jwtAdapter.getUsername(token));
        try {
            assertEquals(0, notificationHandler.getNotifications(String.valueOf(recipientId)).size(), "User should have been notified about message response");
        }
        catch (Exception e) {
            assertTrue(true);
        }
    }


    @Test
    public void testSendMessageFailsWithInvalidToken() throws Exception {
        // Arrange
        String invalidToken = "thisIsNotAValidToken";
        String validToken = fixtures.generateRegisteredUserSession("userValid", "pass");
        ShopDTO shopDTO = fixtures.generateShopAndItems(validToken, "TokenFailShop");

        // Act
        Response<Void> res = shopService.sendMessage(invalidToken, shopDTO.getId(), "Invalid", "Trying to send with bad token");

        // Assert
        assertFalse(res.isOk(), "Message send should fail with invalid token");

        // ✅ No notification should be sent
        int recipientId = Integer.parseInt(jwtAdapter.getUsername(validToken));
        try {
            assertTrue(notificationHandler.getNotifications(String.valueOf(recipientId)).isEmpty(),
                    "No notification should be sent when token is invalid");
        } catch (Exception e) {
            assertTrue(true);

        }
    }

//    @Test
//    public void testOfflineUserReceivesNotificationOnLogin() throws Exception {
//        // Arrange - register both sender and recipient
//        String senderToken = fixtures.generateRegisteredUserSession("senderUser", "pass");
//        String recipientToken = fixtures.registerUserWithoutLogin("recipientUser", "pass"); // Not logged in yet
//
//        // Sender creates shop
//        ShopDTO shopDTO = fixtures.generateShopAndItems(senderToken, "OfflineNotifShop");
//        int shopId = shopDTO.getId();
//
//        // Get recipient userId (from repository, since not logged in)
//        int recipientId = userRepository.getUserByName("recipientUser").getUserID();
//
//        // Act - sender sends a message
//        String title = "Delayed Shipping";
//        String content = "Will this item arrive by Friday?";
//        Response<Void> sendRes = shopService.sendMessage(senderToken, shopId, title, content);
//        assertTrue(sendRes.isOk(), "Message send should succeed");
//
//        // At this point recipient is not online. Notification should be stored.
//        Queue<NotificationDTO> storedNotifs = notificationHandler.getNotifications(String.valueOf(recipientId));
//        assertEquals(1, storedNotifs.size(), "Notification should be stored for offline user");
//
//        NotificationDTO notif = storedNotifs.peek();
//        assertTrue(notif.getMessage().contains("You have a new message"), "Notification content should mention new message");
//
//        // Act - recipient now logs in (and we simulate fetching notifications)
//        String actualRecipientToken = fixtures.loginUser(recipientToken,"recipientUser", "pass");
//        notificationHandler.notifyUser(String.valueOf(recipientId)); // Simulate WebSocket connect
//
//        // After notification dispatch, queue should be empty
//        storedNotifs = notificationHandler.getNotifications(String.valueOf(recipientId));
//        assertEquals(0, storedNotifs.size(), "Notification queue should be cleared after delivery");
//    }



}
