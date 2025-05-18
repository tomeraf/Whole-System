package Tests.AcceptanceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.backend.Service.UserService;

public class NotificationsTests extends BaseAcceptanceTests{
    private String managerToken;

    @BeforeEach
    public void setUp() {
        super.setUp();
        managerToken=fixtures.generateSystemManagerSession("manager","system");
        Registered systemManger=userRepository.getUserByName("manager");
        systemManger.setSystemManager(true);
    }

    @Test
    public void testDelyedNotification_CloseShop(){
        String ownerToken=fixtures.generateRegisteredUserSession("owner", "owner");
        ShopDTO shop=fixtures.generateShopAndItems(ownerToken, "shop");
        userService.logoutRegistered(ownerToken);
        Response<Void> res=shopService.closeShop(managerToken, shop.getId());
        assertTrue(res.isOk(), "Shop should be closed successfully");
        // Check if the notification was delayed
        String ownerId=Integer.parseInt(jwtAdapter.getUsername(ownerToken))+"";
        assertTrue(notificationHandler.getNotifications(ownerId).size() > 0, "Notification should be delayed");
        Response<String> res2=userService.loginUser(ownerToken, "owner", "owner");
        assertTrue(res2.isOk(), "User should be able to login");
        userService.loginNotify(res2.getData());
        assertTrue(notificationHandler.getNotifications(ownerId).size() == 0, "Notification should be delayed");



    }

}
