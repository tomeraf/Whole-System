package Tests.AcceptanceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Domain.Response;
import Domain.DTOs.ShopDTO;
import Domain.User.Guest;
import Domain.User.Registered;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SystemManagerTests extends BaseAcceptanceTests {
    private String managerToken;
    @BeforeEach
    public void setUp(){
        super.setUp();
        managerToken=fixtures.generateSystemManagerSession("manager","system");
        Registered systemManger=userRepository.getUserByName("manager");
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
        userService.suspendUser(managerToken, "user", Optional.of(LocalDateTime.now()), Optional.of(LocalDateTime.now().plusSeconds(3)));
        Response<String> response = userService.watchSuspensions(managerToken);
        assertTrue(response.isOk(), "System Manager should be able to watch a user's suspension");
        String suspensionInfo = response.getData();
        assertTrue(suspensionInfo.contains("user:user"), "Suspension info should not contain the user's name");
        try{
        Thread.sleep(4000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
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
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        Response<Void> closeResp = shopService.closeShop(managerToken, shop.getId());
        assertTrue(closeResp.isOk(), "closeShopBySystemManager should succeed");
    }
    @Test
    public void testCloseShop_NotAsSystemManager_ShouldFail() {
        String ownerToken = fixtures.generateRegisteredUserSession("Owner", "Pwd0");
        ShopDTO shop = fixtures.generateShopAndItems(ownerToken);
        String userToken = fixtures.generateRegisteredUserSession("user", "password");
        Response<Void> closeResp = shopService.closeShop(userToken, shop.getId());
        assertTrue(!closeResp.isOk(), "closeShopBySystemManager should fail");
    }
    @Test
    public void testCloseShop_ShopNotFound_ShouldFail() {
        Response<Void> closeResp = shopService.closeShop(managerToken, 999);
        assertTrue(!closeResp.isOk(), "closeShopBySystemManager should fail");
    }
}
