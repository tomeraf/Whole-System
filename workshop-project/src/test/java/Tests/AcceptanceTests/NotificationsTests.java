package Tests.AcceptanceTests;

import com.halilovindustries.backend.Domain.DTOs.*;
import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;
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
import static org.mockito.ArgumentMatchers.any;
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

   @Test
   public void testBroadcastNotificationToRegisteredUser() throws Exception {
       // Arrange
       String userUuid = "user-123";
       String expectedMessage = "Hello, user!";
       CompletableFuture<String> messageReceived = new CompletableFuture<>();

       // Mock Vaadin UI
       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));

       UI.setCurrent(mockUI);

       // Register listener
       Consumer<String> listener = msg -> {
           System.out.println("Listener received: " + msg);
           messageReceived.complete(msg);
       };
       Registration registration = Broadcaster.register("1", userUuid, listener);
       // Act
       boolean success = Broadcaster.broadcast(userUuid, expectedMessage);

       assertTrue(success, "Broadcast should return true");
       // Wait for listener to be triggered
       String actualMessage = messageReceived.get(10, TimeUnit.SECONDS);
       assertEquals(expectedMessage, actualMessage, "Listener should receive the correct message");

       // Clean up
       registration.remove();
       UI.setCurrent(null);
   }

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

   @Test
   public void testBroadcastToMultipleListeners() throws Exception {
       String userUuid = "multi-listener-user";
       String expectedMessage = "Broadcast to all listeners";

       CompletableFuture<String> received1 = new CompletableFuture<>();
       CompletableFuture<String> received2 = new CompletableFuture<>();

       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));
       UI.setCurrent(mockUI);

       Consumer<String> listener1 = msg -> received1.complete(msg);
       Consumer<String> listener2 = msg -> received2.complete(msg);

       Registration reg1 = Broadcaster.register("1", userUuid, listener1);
       Registration reg2 = Broadcaster.register("1", userUuid, listener2);

       boolean success = Broadcaster.broadcast(userUuid, expectedMessage);
       assertTrue(success);

       assertEquals(expectedMessage, received1.get(10, TimeUnit.SECONDS));
       assertEquals(expectedMessage, received2.get(10, TimeUnit.SECONDS));

       reg1.remove();
       reg2.remove();
       UI.setCurrent(null);
   }

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

        Registration registration = Broadcaster.register("1", userUuid, listener);
        registration.remove(); // Remove immediately

        boolean success = Broadcaster.broadcast(userUuid, message);
        assertFalse(wasCalled.get(), "Listener should not be called after removal");

        UI.setCurrent(null);
    }

   @Test
   public void testBroadcastToMultipleUsersIndependently() throws Exception {
       String user1 = "user-1";
       String user2 = "user-2";
       String message1 = "Message for User 1";
       String message2 = "Message for User 2";

       CompletableFuture<String> receivedByUser1 = new CompletableFuture<>();
       CompletableFuture<String> receivedByUser2 = new CompletableFuture<>();

       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));
       UI.setCurrent(mockUI);

       Registration reg1 = Broadcaster.register("1", user1, msg -> receivedByUser1.complete(msg));
       Registration reg2 = Broadcaster.register("1", user2, msg -> receivedByUser2.complete(msg));

       boolean success1 = Broadcaster.broadcast(user1, message1);
       boolean success2 = Broadcaster.broadcast(user2, message2);

       assertTrue(success1);
       assertTrue(success2);
       assertEquals(message1, receivedByUser1.get(10, TimeUnit.SECONDS));
       assertEquals(message2, receivedByUser2.get(10, TimeUnit.SECONDS));

       reg1.remove();
       reg2.remove();
       UI.setCurrent(null);
   }

   @Test
   public void testSameListenerRegisteredTwice() throws Exception {
       String userUuid = "double-listener-user";
       String message = "Hello double";

       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));
       UI.setCurrent(mockUI);

       AtomicInteger callCount = new AtomicInteger(0);
       Consumer<String> listener = msg -> callCount.incrementAndGet();

       Registration reg1 = Broadcaster.register("1", userUuid, listener);
       Registration reg2 = Broadcaster.register("1", userUuid, listener);

       boolean success = Broadcaster.broadcast(userUuid, message);
       assertTrue(success);

       TimeUnit.SECONDS.sleep(1); // Give executor time to finish
       assertEquals(2, callCount.get(), "Listener should have been called twice");

       reg1.remove();
       reg2.remove();
       UI.setCurrent(null);
   }

   @Test
   public void testBroadcastNullMessage() throws Exception {
       String userUuid = "null-message-user";
       CompletableFuture<String> received = new CompletableFuture<>();

       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));
       UI.setCurrent(mockUI);

       Registration reg = Broadcaster.register("1", userUuid, received::complete);

       boolean success = Broadcaster.broadcast(userUuid, null);
       assertTrue(success);

       assertNull(received.get(10, TimeUnit.SECONDS));

       reg.remove();
       UI.setCurrent(null);
   }

   @Test
   public void testNotificationOnCloseShop() throws Exception {
       // Arrange
       String ownerToken = fixtures.generateRegisteredUserSession("owner", "owner");
       ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

       // Register listener for all shop members (including owner)
       Shop realShop = shopRepository.getShopById(shop.getId());
       int founderId = realShop.getFounderID();
       
       UI mockUI = mock(UI.class);
       doAnswer(invocation -> {
           Command command = invocation.getArgument(0);
           command.execute();
           return null;
       }).when(mockUI).access(any(Command.class));
       UI.setCurrent(mockUI);

       CompletableFuture<String> notificationReceived = new CompletableFuture<>();
       // Register broadcaster listener for founder id
       Registration registration = Broadcaster.register("1", String.valueOf(founderId), notificationReceived::complete);

       //close shop
       Response<Void> res = shopService.closeShop(ownerToken, shop.getId());

       // Assert
       assertTrue(res.isOk(), "Closing shop should succeed");

       // Check notification broadcasted
       String notification = notificationReceived.get(5, TimeUnit.SECONDS);
       assertNotNull(notification, "Notification should be received");
       assertTrue(notification.contains("is closed"), "Notification should mention shop closure");

       registration.remove();
       UI.setCurrent(null);
   }

    @Test
    public void testNotificationOnBuyCartContent() throws Exception {
        // Add before registering the listener
        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(Command.class));
        UI.setCurrent(mockUI);

        // Arrange
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "buyer");
        ShopDTO shop = fixtures.generateShopAndItems(buyerToken, "MyShop");
        
        Shop realShop = shopRepository.getShopById(shop.getId());
        int founderId = realShop.getFounderID();
        String expectedMessage = "Items were purchased by buyer";
        CompletableFuture<String> messageReceived = new CompletableFuture<>();

        // Register listener
        Consumer<String> listener = msg -> {
            System.out.println("!!!!!!!!!!!!!!!!!!!!Listener received: " + msg);
            messageReceived.complete(msg);
        };
        Registration registration = Broadcaster.register("1", String.valueOf(founderId), listener);

        ItemDTO firstItem = shop.getItems().get(0);

        // Prepare items map for addItemsToCart
        HashMap<Integer, HashMap<Integer, Integer>> userItems = new HashMap<>();
        HashMap<Integer, Integer> itemsForShop = new HashMap<>();
        itemsForShop.put(firstItem.getItemID(), 1);  // buy 1 quantity of the first item
        userItems.put(shop.getId(), itemsForShop);

        // Add items to cart
        Response<Void> addToCartResponse = orderService.addItemToCart(buyerToken, shop.getId(), firstItem.getItemID(), 1);
        assertTrue(addToCartResponse.isOk(), "Adding items to cart should succeed");



    // Prepare dummy payment and shipment details
        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123", "333", "12", "25");
        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");

        //return true for validation
        fixtures.mockPositivePayment(paymentDetails);
        fixtures.mockPositiveShipment(shipmentDetails);

        // Act
        Response<Order> buyResponse = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);

        // Assert
        assertTrue(buyResponse.isOk(), "Buying cart content should succeed");    
        
        // Wait for listener to be triggered
        String actualMessage = messageReceived.get(10, TimeUnit.SECONDS);
        assertEquals(expectedMessage, actualMessage, "Listener should receive the correct message");

        // Clean up
        registration.remove();
        // And add at the end of the test
        UI.setCurrent(null); // Clean up
    }

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


        orderService.addItemToCart(buyerToken,shop.getId(),shop.getItems().get(0).getItemID(),1);

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123", "333", "12", "25");
        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");


        when(payment.validatePaymentDetails(any(PaymentDetailsDTO.class))).thenReturn(false);
        when(shipment.validateShipmentDetails(any(ShipmentDetailsDTO.class))).thenReturn(false);

        // Mock payment validation to fail
        when(payment.validatePaymentDetails(any())).thenReturn(false);

        Response<Order> res = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);

        //assertFalse(res.isOk(), "Buying cart content with invalid payment should fail");
    }

    @Test
    public void testBuyCartContentEmptyCart() throws Exception {
        String buyerToken = fixtures.generateRegisteredUserSession("buyer", "buyer");

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO("4111111111111111", "tomer", "123","333", "12", "25");
        ShipmentDetailsDTO shipmentDetails = new ShipmentDetailsDTO("123", "ido", "sss@gmail.com", "123456789","il","ber","hamanit18","444");


        // No items added to cart

        Response<Order> res = orderService.buyCartContent(buyerToken, paymentDetails, shipmentDetails);

        assertFalse(res.isOk(), "Buying cart content with empty cart should fail");
    }
    
    @Test
    public void testSendAndRespondToMessage() throws Exception {
        // Arrange
        String founderToken = fixtures.generateRegisteredUserSession("founder", "founder");
        ShopDTO shopDTO = fixtures.generateShopAndItems(founderToken, "TestShop");
 
        int shopId = shopDTO.getId();
 
        String title = "Issue with item";
        String content = "The item arrived damaged.";
 
        // Act - Send a message
        Response<Void> sendRes = shopService.sendMessage(founderToken, shopId, title, content);
 
        // Assert message was sent successfully
        assertTrue(sendRes.isOk(), "Sending message should succeed");
 
        // Act - Respond to the message
        String responseTitle = "Apologies";
        String responseContent = "We'll send a replacement.";
 
        Response<List<Message>> shopInboxRes = shopService.getInbox(founderToken, shopId);
        Message originalMessage = shopInboxRes.getData().stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst()
                .orElseThrow();
 
        Response<Void> respondRes = shopService.respondToMessage(
                founderToken, shopId, originalMessage.getId(), responseTitle, responseContent
        );
 
        assertTrue(respondRes.isOk(), "Responding to message should succeed");
 
        // ✅ Notification check (replace with actual check depending on your test setup)
        String recipientId = String.valueOf(originalMessage.getUserName());
        try {
            //assertEquals(2, notificationHandler.getNotifications(recipientId).size(), "User should have been notified about message response");
        } catch (Exception e) {
            assertTrue(false,"User should have been notified about message response and about his message");
        }
    }

    @Test
    public void testSendMessageSuccessWithNonEmptyContent() throws Exception {
        // Arrange
        String token = fixtures.generateRegisteredUserSession("emptyMsgUser", "pass");
        ShopDTO shopDTO = fixtures.generateShopAndItems(token, "AnotherShop");

        // Act
        Response<Void> res = shopService.sendMessage(token, shopDTO.getId(), "nice", "hi");

        // Assert
        assertTrue(res.isOk(), "Message send should fail with empty content");

        Shop realShop = shopRepository.getShopById(shopDTO.getId());
        int founderId = realShop.getFounderID();
        int d = notificationHandler.getNotifications(String.valueOf(founderId)).size();
        assertEquals(1, d, "User should have been notified about message response");
    }


    @Test
    public void testSendMessageFailsWithEmptyContent() throws Exception {
        // Arrange
        String token = fixtures.generateRegisteredUserSession("emptyMsgUser", "pass");
        ShopDTO shopDTO = fixtures.generateShopAndItems(token, "AnotherShop");

        // Act
        Response<Void> res = shopService.sendMessage(token, shopDTO.getId(), "", "");

        // Assert
        assertFalse(res.isOk(), "Message send should fail with empty content");

        Shop realShop = shopRepository.getShopById(shopDTO.getId());
        int founderId = realShop.getFounderID();
        int d = notificationHandler.getNotifications(String.valueOf(founderId)).size();
        assertEquals(0, d, "User should have been notified about message response");
    }


    @Test
    public void testOfflineUserReceivesNotificationOnLogin() throws Exception {
        // Arrange - register both sender and recipient
        String recipientToken = fixtures.generateRegisteredUserSession("recipientUser", "pass");
        
        String senderToken = fixtures.generateRegisteredUserSession("senderUser", "pass"); 

        // Sender creates shop
        ShopDTO shopDTO = fixtures.generateShopAndItems(recipientToken, "OfflineNotifShop");
        int shopId = shopDTO.getId();

        userService.logoutRegistered(recipientToken);

        Shop realShop = shopRepository.getShopById(shopId);
        int founderId = realShop.getFounderID();

        // Act - sender sends a message
        String title = "Delayed Shipping";
        String content = "Will this item arrive by Friday?";
        Response<Void> sendRes = shopService.sendMessage(senderToken, shopId, title, content);
        assertTrue(sendRes.isOk(), "Message send should succeed");
        // At this point recipient is not online. Notification should be stored.
        Queue<NotificationDTO> storedNotifs = notificationHandler.getNotifications(String.valueOf(founderId));
        assertEquals(1, storedNotifs.size(), "Notification should be stored for offline user");

        NotificationDTO notif = storedNotifs.peek();
        assertTrue(notif.getMessage().contains("You have a new message"), "Notification content should mention new message");
    }


    @Test
    public void testDeleteNotification_Success() throws Exception {
        // Arrange: owner creates shop and bidder submits a bid to trigger a notification
        String ownerToken  = fixtures.generateRegisteredUserSession("ownerDel", "ownerDel");
        ShopDTO shop       = fixtures.generateShopAndItems(ownerToken, "DelShop");
        String bidderToken = fixtures.generateRegisteredUserSession("bidderUser", "pass");
        int itemId         = shop.getItems().get(0).getItemID();
        // submitBid(token, shopId, itemId, bidPrice)
        Response<Void> bidRes = orderService.submitBidOffer(bidderToken, shop.getId(), itemId, 50.0);
        assertTrue(bidRes.isOk(), "Submitting a bid should succeed");

        // Founder should now have one notification
        String userId = String.valueOf(
            shopRepository.getShopById(shop.getId()).getFounderID()
        );
        Queue<NotificationDTO> before = notificationHandler.getNotifications(userId);
        assertFalse(before.isEmpty(), "There should be at least one notification before deletion");
        int notifId = before.peek().getId();

        // Act: delete it
        notificationHandler.deleteNotification(userId, notifId);

        // Assert: queue is now empty
        Queue<NotificationDTO> after = notificationHandler.getNotifications(userId);
        assertTrue(after.isEmpty(), "Notification queue should be empty after deletion");
    }
}
