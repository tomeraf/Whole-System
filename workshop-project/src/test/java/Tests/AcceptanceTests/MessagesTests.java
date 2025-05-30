package Tests.AcceptanceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = com.halilovindustries.Application.class)
public class MessagesTests extends BaseAcceptanceTests {
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testSendMessage_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        Response<Void> resp = shopService.sendMessage(ownerToken, shop.getId(), "Hello", "Welcome to the shop!");
        assertTrue(resp.isOk(), "Sending message should succeed");
    }

    @Test
    public void testRespondToMessage_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd1");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // User sends a message to the shop
        Response<Void> sendResp = shopService.sendMessage(userToken, shop.getId(), "Question", "Is this item available?");
        assertTrue(sendResp.isOk(), "User should be able to send a message");

        // Get the message ID from the shop's inbox
        List<Message> shopInbox = shopService.getInbox(ownerToken, shop.getId()).getData();
        assertFalse(shopInbox.isEmpty(), "Shop inbox should not be empty");
        int messageId = shopInbox.get(0).getId();

        // Owner responds to the message
        Response<Void> respondResp = shopService.respondToMessage(ownerToken, shop.getId(), messageId, "Question", "Yes, it is available.");
        assertTrue(respondResp.isOk(), "Owner should be able to respond to a message");
    }

    @Test
    public void testGetShopInbox_ShouldReturnMessages() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd1");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        shopService.sendMessage(userToken, shop.getId(), "Hello", "Hi Shop!");
        Response<List<Message>> inboxResp = shopService.getInbox(ownerToken, shop.getId());
        assertTrue(inboxResp.isOk(), "Should be able to get shop inbox");
        List<Message> messages = inboxResp.getData();
        assertFalse(messages.isEmpty(), "Shop inbox should contain messages");
    }

    @Test
    public void testGetUserInbox_ShouldReturnMessages() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd1");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // User sends a message, owner responds
        shopService.sendMessage(userToken, shop.getId(), "Question", "Do you have discounts?");
        List<Message> shopInbox = shopService.getInbox(ownerToken, shop.getId()).getData();
        int messageId = shopInbox.get(0).getId();
        shopService.respondToMessage(ownerToken, shop.getId(), messageId, "Re: Question", "Yes, we do!");

        // Now check user's inbox
        Response<List<Message>> userInboxResp = userService.getInbox(userToken);
        assertTrue(userInboxResp.isOk(), "User should be able to get their inbox");
        List<Message> userMessages = userInboxResp.getData();
        assertFalse(userMessages.isEmpty(), "User inbox should contain messages");
    }

    @Test
    public void testSendMessage_EmptyTitle_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        Response<Void> resp = shopService.sendMessage(ownerToken, shop.getId(), "", "Valid content");
        assertFalse(resp.isOk(), "Sending message with empty title should fail");
    }

    @Test
    public void testSendMessage_EmptyContent_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        Response<Void> resp = shopService.sendMessage(ownerToken, shop.getId(), "Valid title", "");
        assertFalse(resp.isOk(), "Sending message with empty content should fail");
    }

    @Test
    public void testRespondToMessage_AlreadyResponded_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        String userToken = fixtures.generateRegisteredUserSession("User", "Pwd1");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");

        // User sends a message to the shop
        shopService.sendMessage(userToken, shop.getId(), "Question", "Is this item available?");
        List<Message> shopInbox = shopService.getInbox(ownerToken, shop.getId()).getData();
        int messageId = shopInbox.get(0).getId();

        // Owner responds to the message
        shopService.respondToMessage(ownerToken, shop.getId(), messageId, "Re: Question", "Yes, it is available.");

        // Owner tries to respond again
        Response<Void> secondResponse = shopService.respondToMessage(ownerToken, shop.getId(), messageId, "Re: Question", "Answered again.");
        assertFalse(secondResponse.isOk(), "Should not be able to respond to a message twice");
    }

    @Test
    public void testRespondToMessage_NonExistentMessage_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "MyShop");
        int nonExistentMessageId = 99999;
        Response<Void> resp = shopService.respondToMessage(ownerToken, shop.getId(), nonExistentMessageId, "Re: Question", "This should fail");
        assertFalse(resp.isOk(), "Responding to a non-existent message should fail");
    }
}
