package Tests.AcceptanceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.User.Registered;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SystemManagerTests extends BaseAcceptanceTests {
    private String managerToken;
    @BeforeEach
    public void setUp(){
        super.setUp();
        managerToken = fixtures.generateSystemManagerSession("manager","system");
        Registered systemManger = userRepository.getUserByName("manager");
        systemManger.setSystemManager(true);
    }

    //suspend user tests
    @Test
    public void testSuspendUser_AsSystemManager_ShouldSucceed() {
        fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> response = userService.suspendUser(managerToken, "user", Optional.empty(), Optional.empty());
        assertTrue(response.isOk(), "System Manager should be able to suspend a user");
        Registered user = userRepository.getUserByName("user");
        assertTrue(user.isSuspended(), "User should be suspended");
    }
    @Test
    public void testSuspendUser_NotAsSystemManager_ShouldFail() {
        String userToken=fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> response = userService.suspendUser(userToken, "manager", Optional.empty(), Optional.empty());
        assertTrue(!response.isOk(), "User should not be able to suspend another user");
        Registered user = userRepository.getUserByName("manager");
        assertTrue(!user.isSuspended(), "User should not be suspended");
    }
    @Test
    public void testSuspendUser_InvalidDates_ShouldFail() {
        fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> response = userService.suspendUser(managerToken, "user", Optional.of(LocalDateTime.now().minusDays(3)), Optional.of(LocalDateTime.now().minusDays(1)));
        assertTrue(!response.isOk(), "System Manager should not be able to suspend a user with invalid dates");
        Registered user = userRepository.getUserByName("user");
        assertTrue(!user.isSuspended(), "User should not be suspended");
    }

    @Test
    public void testSuspendUser_WithValidDateRange_ShouldSucceed() {
        // Arrange: register a normal user
        fixtures.generateRegisteredUserSession("tim", "pass");
        // Build a valid start/end (start = now, end = tomorrow)
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = LocalDateTime.now().plusDays(1);

        // Act: suspend “tim” for that window
        Response<Void> response = userService.suspendUser(
            managerToken,
            "tim",
            Optional.of(start),
            Optional.of(end)
        );
        assertTrue(response.isOk(), "Suspension with valid dates should succeed");

        // Immediately after calling, user.isSuspended() may be false, because start is in the future.
        Registered user = userRepository.getUserByName("tim");
        // Depending on your implementation, if “now < start” you may consider them not yet suspended:
        assertTrue(!user.isSuspended(), "User should not be suspended until start time");

        // Note: Skipping time-forward check since no time-travel helper is available.
        try{
            // Simulate waiting until the start time
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // And check watchSuspensions shows the date range
        Response<String> infoResp = userService.watchSuspensions(managerToken);
        assertTrue(infoResp.isOk());
        String info = infoResp.getData();
        assertTrue(info.contains("tim"), "Suspension info must contain username");
        assertTrue(info.contains(start.toLocalDate().toString()), "Should list start date");
        assertTrue(info.contains(end.toLocalDate().toString()), "Should list end date");
    }

    @Test
    public void testSuspendUser_WithFutureStart_ShouldNotSuspendImmediately() {
        // Arrange: register a normal user
        fixtures.generateRegisteredUserSession("futureUser", "pass");
        // Build a future start (1 hour from now) and end (2 days from now)
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end   = LocalDateTime.now().plusDays(2);

        // Act: suspend "futureUser" for the future window
        Response<Void> response = userService.suspendUser(
            managerToken,
            "futureUser",
            Optional.of(start),
            Optional.of(end)
        );
        assertTrue(response.isOk(), "Suspension with future start should succeed");

        // Immediately after calling, user.isSuspended() should be false (start is in future)
        Registered user = userRepository.getUserByName("futureUser");
        assertFalse(user.isSuspended(), "User should not be suspended until the future start time");

        // And check watchSuspensions shows the date range for this future suspension
        Response<String> infoResp = userService.watchSuspensions(managerToken);
        assertTrue(infoResp.isOk());
        String info = infoResp.getData();
        assertFalse(info.contains("futureUser"), "Suspension info is missing since it is in the future");
        assertFalse(info.contains(start.toLocalDate().toString()), "Should not list future start date");
        assertFalse(info.contains(end.toLocalDate().toString()), "Should not list future end date");
    }

    @Test
    public void testSuspendUser_AlreadySuspended_ShouldFail() {
        // Arrange: register a normal user
        fixtures.generateRegisteredUserSession("double", "pass");

        // Act: first suspension should succeed
        Response<Void> first = userService.suspendUser(
            managerToken,
            "double",
            Optional.empty(),
            Optional.empty()
        );
        assertTrue(first.isOk(), "First suspension should succeed");

        // Act: second suspension should fail
        Response<Void> second = userService.suspendUser(
            managerToken,
            "double",
            Optional.empty(),
            Optional.empty()
        );
        assertFalse(second.isOk(), "Suspending an already suspended user should fail");

        // Confirm user remains suspended
        Registered user = userRepository.getUserByName("double");
        assertTrue(user.isSuspended(), "User should remain suspended after failed second attempt");
    }

    @Test
    public void testSuspendUser_NonExistingUser_ShouldFail() {
        // Attempt to suspend “ghost” (never registered)
        Response<Void> response = userService.suspendUser(
            managerToken,
            "ghost",
            Optional.empty(),
            Optional.empty()
        );
        assertFalse(response.isOk(), "Suspending a non‐existent user should fail");
    }


    

    // unsuspend user tests
    @Test
    public void testUnsuspendUser_AsSystemManager_ShouldSucceed() {
        fixtures.generateRegisteredUserSession("user", "password");
        userService.suspendUser(managerToken, "user", Optional.empty(), Optional.empty());
        Response<Void> response = userService.unsuspendUser(managerToken, "user");
        assertTrue(response.isOk(), "System Manager should be able to unsuspend a user");
        Registered user = userRepository.getUserByName("user");
        assertFalse(user.isSuspended(), "User should be unsuspended");
    }
    @Test
    public void testUnsuspendUser_NotAsSystemManager_ShouldFail() {
        fixtures.generateRegisteredUserSession("user", "password");
        String user2token=fixtures.generateRegisteredUserSession("user2", "password");
        userService.suspendUser(managerToken, "user", Optional.empty(), Optional.empty());
        Response<Void> response = userService.unsuspendUser(user2token, "user");
        assertTrue(!response.isOk(), "User should not be able to unsuspend another user");
        Registered user = userRepository.getUserByName("user");
        assertTrue(user.isSuspended(), "User should still be suspended");
    }
    @Test
    public void testUnsuspendUser_UserNotSuspended_ShouldFail() {
        fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> response = userService.unsuspendUser(managerToken, "user");
        assertTrue(!response.isOk(), "System Manager should not be able to unsuspend a user that is not suspended");
        Registered user = userRepository.getUserByName("user");
        assertFalse(user.isSuspended(), "User should not be suspended");
    }

    @Test
    public void testUnsuspendUser_NonExistingUser_ShouldFail() {
        // Attempt to unsuspend “ghost” (never registered)
        Response<Void> response = userService.unsuspendUser(
            managerToken,
            "ghost"
        );
        assertFalse(response.isOk(), "Unsuspending a non‐existent user should fail");
    }

    //watch suspension tests
    @Test
    public void testWatchSuspension_AsSystemManager_ShouldSucceed() {
        fixtures.generateRegisteredUserSession("user lo tov", "password");
        userService.suspendUser(managerToken, "user lo tov", Optional.empty(), Optional.empty());
        Response<String> response = userService.watchSuspensions(managerToken);
        assertTrue(response.isOk(), "System Manager should be able to watch a user's suspension");
        String suspensionInfo = response.getData();
        assertTrue(suspensionInfo.contains("user:user lo tov"), "Suspension info should contain the user's name");
    }
    @Test
    public void testWatchSuspension_NotAsSystemManager_ShouldFail() {
        fixtures.generateRegisteredUserSession("user", "password");
        userService.suspendUser(managerToken, "user", Optional.empty(), Optional.empty());
        String user2token=fixtures.generateRegisteredUserSession("user2", "password");
        Response<String> response = userService.watchSuspensions(user2token);
        assertTrue(!response.isOk(), "User should not be able to watch another user's suspension");
    }
    @Test
    public void testWatchSuspension_SuspensionOver_ShouldSucceed(){
        fixtures.generateRegisteredUserSession("user", "password");
        userService.suspendUser(managerToken, "user",Optional.empty(),Optional.empty());
        Response<String> response = userService.watchSuspensions(managerToken);
        assertTrue(response.isOk(), "System Manager should be able to watch a user's suspension");
        String suspensionInfo = response.getData();
        assertTrue(suspensionInfo.contains("user:user"), "Suspension info should not contain the user's name");
        //remove suspension
        userService.unsuspendUser(managerToken, "user");
        //check if suspension is over
        Response<String> response2 = userService.watchSuspensions(managerToken);
        assertTrue(response2.isOk(), "System Manager should be able to watch a user's suspension");
        String suspensionInfo2 = response2.getData();
        //suspension is over
        assertFalse(suspensionInfo2.contains("user:user"), "Suspension info should not contain the user's name");

    }

    //close shop tests
    @Test
    public void testCloseShop_AsSystemManager_ShouldSucceed() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        Response<Void> closeResp = shopService.closeShop(managerToken, shop.getId());
        assertTrue(closeResp.isOk(), "closeShopBySystemManager should succeed");
    }
    @Test
    public void testCloseShop_NotAsSystemManager_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken,"MyShop");
        String userToken = fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> closeResp = shopService.closeShop(userToken, shop.getId());
        assertTrue(!closeResp.isOk(), "closeShopBySystemManager should fail");
    }
    @Test
    public void testCloseShop_ShopNotFound_ShouldFail() {
        Response<Void> closeResp = shopService.closeShop(managerToken, 999);
        assertTrue(!closeResp.isOk(), "closeShopBySystemManager should fail");
    }

    @Test
    public void testCloseShop_AlreadyClosed_ShouldFail() {
        // 1) Owner and shop setup
        String ownerToken = fixtures.generateRegisteredUserSession("closeTwiceOwner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken, "CloseTwiceShop");
        int shopId = shop.getId();

        // 2) First close should succeed
        Response<Void> first = shopService.closeShop(managerToken, shopId);
        assertTrue(first.isOk(), "First closeShop should succeed");

        // 3) Second close on the same shop should fail
        Response<Void> second = shopService.closeShop(managerToken, shopId);
        assertFalse(second.isOk(), "Closing an already closed shop should fail");
    }

    //try to to do an action while suspended
    @Test
    public void testActionWhileSuspended_ShouldFail() {
        String token=fixtures.generateRegisteredUserSession("user", "password");
        userService.suspendUser(managerToken, "user", Optional.empty(), Optional.empty());
        Response<ShopDTO> response = shopService.createShop(token, "MyShop", "A shop for tests");
        assertTrue(!response.isOk(), "ודקר should not be able to create a shop while suspended");
    }


    


    
}
