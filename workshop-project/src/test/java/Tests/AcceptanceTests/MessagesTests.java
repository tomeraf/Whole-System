package Tests.AcceptanceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.User.Permission;

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
            .filter(m -> m.getContent().equals(responseText))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Reply message was not found"));
        assertEquals(responseText, replyMsg.getContent(), "The reply content must match what the manager sent");

        // Find the original message in the updated inbox
        Message updatedOriginalMsg = updatedInbox.stream()
            .filter(m -> m.getId() == messageId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Original message was not found"));

        assertEquals(replyMsg.getId(), updatedOriginalMsg.getRespondId(), 
            "Original message should reference the reply message ID");
    }
}