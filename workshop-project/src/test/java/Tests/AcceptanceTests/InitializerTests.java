package Tests.AcceptanceTests;



import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.UserDTO;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.init.Initializer;
import com.halilovindustries.backend.Domain.init.StartupConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class InitializerTests extends BaseAcceptanceTests {
    private Initializer initializer;
    private String ST;
    private Path initFile;

    @BeforeEach
    public void setUp() {
        super.setUp();
        ST = userService.enterToSystem().getData();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(initFile);
    }

    private void runInit(String content) throws IOException {
        initFile = Files.createTempFile("initTest", ".txt");
        Files.writeString(initFile, content); // optional content
        StartupConfig startupConfig = new StartupConfig();
        startupConfig.setInitFile(initFile.toString());
        initializer = new Initializer(startupConfig, userService, shopService, orderService, databaseHealthService);
        initializer.init();
    }
    @Test
    public void good_init(){
        String content = """
        // System setup - first registered user is system manager
          register-user(u1, u1, 1999-06-16);
          make-system-manager(u1);

          // Registering users
          register-user(u2, u2, 2000-01-01);
          register-user(u3, u3, 2000-01-01);
          register-user(u4, u4, 2000-01-01);
          register-user(u5, u5, 2000-01-01);
          register-user(u6, u6, 2000-01-01);
          logout-user(u6);

          // User u2 logs in
          login-user(u2, u2);

          // User u2 opens shop "s1"
          create-shop(s1, descs1);

          // User u2 add Bamba to the shop
          add-item(0, Bamba, FOOD, 5.5, fking Bamba, 10);

          // User u2 appoints u3 as manager of the shop with permission to manage inventory
          add-shop-manager(0, u3, UPDATE_ITEM_QUANTITY);

          // u2 appoints u4 and u5 as shop owners
          add-shop-owner(0, u4);
          add-shop-owner(0, u5);

          // u2 logs out
          logout-user(u2);
    """;
        try {
            runInit(content);

            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertTrue(res.isOk(), "u1 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u3", "u3");
            assertTrue(res.isOk(), "u3 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u4", "u4");
            assertTrue(res.isOk(), "u4 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u5", "u5");
            assertTrue(res.isOk(), "u5 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u6", "u6");
            ST = res.getData();
            assertTrue(res.isOk(), "u6 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            ST = userService.loginUser(ST, "u2", "u2").getData();
            Response<ShopDTO> res2 = shopService.getShopInfo(ST, 0);
            assertTrue(res2.isOk(), "s1 should exist");

            List<UserDTO> members = shopService.getShopMembers(ST, 0).getData();
            assertEquals(members.size(),4, "s1 should have 4 members");

            List<Permission> u3Permissions = shopService.getMemberPermissions(ST, 0, "u3").getData();
            assertEquals("UPDATE_ITEM_QUANTITY",u3Permissions.get(0).toString(), "u3 should have 1 permissions");

            List<ItemDTO> items = shopService.showShopItems(ST, 0).getData();
            assertEquals(1, items.size(), "s1 should have 1 item");
            assertEquals(items.get(0).getName(),"Bamba","the only item should be Bamba");

            ST = userService.logoutRegistered(ST).getData();
        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }

    @Test
    public void bad_init(){
        String content = """
        // System setup - first registered user is system manager
          register-user(u1, u1, 1999-06-16);
          make-system-manager(u1);

          // Registering users
          register-user(u2, u2, 2000-01-01);
          register-user(u3, u3, 2000-01-01);
          register--user(u4, u4, 2000-01-01);
          register-user(u5, u5, 2000-01-01);
          register-user(u6, u6, 2000-01-01);

          // User u2 logs in
          login-user(u2, u2);

          // User u2 opens shop "s1"
          create-shop(s1, descs1);

          // User u2 add Bamba to the shop
          add-item(0, Bamba, FOOD, 5.5, fking Bamba, 10);

          // User u2 appoints u3 as manager of the shop with permission to manage inventory
          add-shop-manager(0, u3, UPDATE_ITEM_QUANTITY);

          // u2 appoints u4 and u5 as shop owners
          add-shop-owner(0, u4);
          add-shop-owner(0, u5);

          // u2 logs out
          logout-user(u2);
    """;
        try {
            runInit(content);
            fail("should throw exception");
        }
        catch (Exception E){
            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertFalse(res.isOk(), "u1 shouldn't exist");

            res = userService.loginUser(ST, "u2", "u2");
            assertFalse(res.isOk(), "u2 shouldn't exist");

            res = userService.loginUser(ST, "u3", "u3");
            assertFalse(res.isOk(), "u3 shouldn't exist");

            res = userService.loginUser(ST, "u4", "u4");
            assertFalse(res.isOk(), "u4 shouldn't exist");

            res = userService.loginUser(ST, "u5", "u5");
            assertFalse(res.isOk(), "u5 shouldn't exist");

            res = userService.loginUser(ST, "u6", "u6");
            assertFalse(res.isOk(), "u6 shouldn't exist");

            Response<ShopDTO> res2 = shopService.getShopInfo(ST, 0);
            assertFalse(res2.isOk(), "s1 shouldn't exist");
            }
    }




    @Test
    public void User_Management_register(){
        String content = """
        // System setup - first registered user is system manager
          register-user(u1, u1, 1999-06-16);
          make-system-manager(u1);

          // Registering users
          register-user(u2, u2, 2000-01-01);
          register-user(u3, u3, 2000-01-01);
          register-user(u4, u4, 2000-01-01);
          register-user(u5, u5, 2000-01-01);
          register-user(u6, u6, 2000-01-01);
    """;
        try {
            runInit(content);

            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertTrue(res.isOk(), "u1 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u3", "u3");
            assertTrue(res.isOk(), "u3 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u4", "u4");
            assertTrue(res.isOk(), "u4 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u5", "u5");
            assertTrue(res.isOk(), "u5 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u6", "u6");
            ST = res.getData();
            assertTrue(res.isOk(), "u6 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }



    @Test
    public void shop_creation_test(){
        String content = """
        register-user(u1, u1, 1999-06-16);
        register-user(u2, u2, 2000-01-01);
        register-user(u3, u3, 2000-01-01);
        register-user(u4, u4, 2000-01-01);
        register-user(u5, u5, 2000-01-01);
        register-user(u6, u6, 2000-01-01);
        logout-user(u6);
        login-user(u2, u2);
        create-shop(s1, firstShop);
        create-shop(s2, secondShop);
        close-shop(1);
        add-shop-owner(0, u3);
        add-shop-owner(0, u4);
        remove-appointment(0, u4);
        add-shop-manager(0, u5, UPDATE_ITEM_QUANTITY);
        add-shop-manager(0, u6, ANSWER_BID);
        remove-appointment(0, u6);
        add-permission(0, u5, VIEW);
        add-permission(0, u5, APPOINTMENT);
        remove-permission(0, u5, VIEW);
        logout-user();
    """;
        try {
            runInit(content);

            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertTrue(res.isOk(), "u1 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u3", "u3");
            assertTrue(res.isOk(), "u3 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u4", "u4");
            assertTrue(res.isOk(), "u4 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u5", "u5");
            assertTrue(res.isOk(), "u5 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            res = userService.loginUser(ST, "u6", "u6");
            ST = res.getData();
            assertTrue(res.isOk(), "u6 should exist");
            ST = userService.logoutRegistered(res.getData()).getData();

            ST = userService.loginUser(ST, "u2", "u2").getData();
            Response<ShopDTO> res2 = shopService.getShopInfo(ST, 0);
            assertTrue(res2.isOk(), "s1 should exist");

            Response<List<ShopDTO>> res3 = shopService.showAllShops(ST);
            assertFalse(res3.getData().contains(shopService.getShopInfo(ST,1).getData()), "s2 shouldn't exist");

            List<UserDTO> members = shopService.getShopMembers(ST, 0).getData();
            assertEquals(members.size(),3, "s1 should have 3 members");
            for(UserDTO member : members){
                assertTrue(member.getUsername().equals("u2") || member.getUsername().equals("u3")
                        || member.getUsername().equals("u5"), "only 2 3 and 5 should be in");
            }

            List<Permission> u5Permissions = shopService.getMemberPermissions(ST, 0, "u5").getData();
            assertEquals(u5Permissions.size(),2, "u5 should have 2 permissions");
            for(Permission permission : u5Permissions){
                assertTrue(permission.toString().equals("APPOINTMENT") ||
                        permission.toString().equals("UPDATE_ITEM_QUANTITY"), "only UPDATE_ITEM_QUANTITY and APPOINTMENT should be in u5 permissions.");
            }

            ST = userService.logoutRegistered(ST).getData();
        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }

    @Test
    public void shop_outsider_test(){
        String content = """
           register-user(u1, u1, 1999-06-16);
           register-user(u2, u2, 2000-01-01);
           register-user(u3, u3, 2000-01-01);
           logout-user(u3);
           login-user(u2, u2);
           create-shop(s1, firstShop);
           add-item(0, item1, FOOD, 20 , item1item1 , 10 );
           add-item(0, item2, FOOD, 20 , item2item2 , 10 );
           logout-user(u2);
           login-user(u3, u3);
           add-to-cart(0, 0, 5);
           add-to-cart(0, 1, 6);
           remove-from-cart(0, 1);
           send-message(0, test1 , testing messages);
           logout-user(3);
           login-user(u2,u2);
           respond-to-message(0,  0, test1good, test1 went well);
           logout-user(u2);
    """;
        try {
            runInit(content);





        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }


}
