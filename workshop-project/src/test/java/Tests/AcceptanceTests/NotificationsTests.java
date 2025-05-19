package Tests.AcceptanceTests;

import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
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
        Registration registration = Broadcaster.register(userUuid, listener);

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

        Registration reg1 = Broadcaster.register(userUuid, listener1);
        Registration reg2 = Broadcaster.register(userUuid, listener2);

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

        Registration registration = Broadcaster.register(userUuid, listener);
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

        Registration reg1 = Broadcaster.register(user1, msg -> receivedByUser1.complete(msg));
        Registration reg2 = Broadcaster.register(user2, msg -> receivedByUser2.complete(msg));

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

        Registration reg1 = Broadcaster.register(userUuid, listener);
        Registration reg2 = Broadcaster.register(userUuid, listener);

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

        Registration reg = Broadcaster.register(userUuid, received::complete);

        boolean success = Broadcaster.broadcast(userUuid, null);
        assertTrue(success);

        assertNull(received.get(10, TimeUnit.SECONDS));

        reg.remove();
        UI.setCurrent(null);
    }

    @Test
    public void testRegisterWithoutUIThrows() {
        UI.setCurrent(null); // Explicitly no UI context

        assertThrows(IllegalStateException.class, () -> {
            Broadcaster.register("bad-user", msg -> {});
        }, "Should throw when UI.getCurrent() is null");
    }









    // @Test
    // public void testDelyedNotification_CloseShop(){
    //     String ownerToken=fixtures.generateRegisteredUserSession("owner", "owner");
    //     ShopDTO shop=fixtures.generateShopAndItems(ownerToken, "shop");
    //     userService.logoutRegistered(ownerToken);
    //     Response<Void> res=shopService.closeShop(managerToken, shop.getId());
    //     assertTrue(res.isOk(), "Shop should be closed successfully");
    //     // Check if the notification was delayed
    //     String ownerId=Integer.parseInt(jwtAdapter.getUsername(ownerToken))+"";
    //     assertTrue(notificationHandler.getNotifications(ownerId).size() > 0, "Notification should be delayed");
    //     Response<String> res2=userService.loginUser(ownerToken, "owner", "owner");
    //     Broadcaster.register(ownerId, msg -> {});
    //     assertTrue(res2.isOk(), "User should be able to login");
    //     userService.loginNotify(res2.getData());
    //     assertTrue(notificationHandler.getNotifications(ownerId).size() == 0, "Notification should be delayed");
    // }

}
