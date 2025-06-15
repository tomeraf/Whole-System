package Tests.AcceptanceTests;



import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Service.init.Initializer;
import com.halilovindustries.backend.Service.init.StartupConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class InitializerTests extends BaseAcceptanceTests {
    private Initializer initializer;
    private String ST;

    @BeforeEach
    public void setUp() {
        super.setUp();
        StartupConfig startupConfig = new StartupConfig();
        startupConfig.setInitFile("initTest.txt");
        initializer = new Initializer(startupConfig, userService, shopService, orderService);
        ST = userService.enterToSystem().getData();
    }

    private void runInit(String content) throws IOException {
        Path path = Path.of("src/test/resources/initTest.txt");
        Files.writeString(path, content);
        initializer.init();
    }



    @Test
    public void successfullyInitialized(){
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

// User u2 logs in
        login-user(u2, u2);

// User u2 opens shop "s1"
        create-shop(u2, s1);

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
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            userService.logoutRegistered(res.getData());

             res = userService.loginUser(ST, "u3", "u3");
            assertTrue(res.isOk(), "u3 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u4", "u4");
            assertTrue(res.isOk(), "u4 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u5", "u5");
            assertTrue(res.isOk(), "u5 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u6", "u6");
            assertTrue(res.isOk(), "u6 should exist");
            userService.logoutRegistered(res.getData());

            ST = userService.loginUser(ST, "u2", "u2").getData();
            Response<ShopDTO> res2 = shopService.getShopInfo(ST, 0);
            assertTrue(res.isOk(), "s1 should exist");



        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }

    @Test
    public void User_Management_register(){
        String content = """
        register-user(u1, u1, 1999-06-16);
        register-user(u2, u2, 2000-01-01);
    """;
        try {
            runInit(content);

            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertTrue(res.isOk(), "u1 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            userService.logoutRegistered(res.getData());
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
        login-user(u2, u2);
        createShop(s1, firstShop);
        createShop(s2, secondShop);
        closeShop(s2);
        addShopOwner(0, u3);
        addShopOwner(0, u4);
        removeAppointment(0 u4);
        addShopManager(0, u5, UPDATE_ITEM_QUANTITY);
        addShopManager(0, u6, ANSWER_BID);
        removeAppointment(0 u6);
        addUserPermission(0, u5, VIEW);
        addUserPermission(0, u5, APPOINTMENT);
        removeUserPermission(0, u5, VIEW);
        logoutUser();
    """;
        try {
            runInit(content);

            Response<String> res = userService.loginUser(ST, "u1", "u1");
            assertTrue(res.isOk(), "u1 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u2", "u2");
            assertTrue(res.isOk(), "u2 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u3", "u3");
            assertTrue(res.isOk(), "u3 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u4", "u4");
            assertTrue(res.isOk(), "u4 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u5", "u5");
            assertTrue(res.isOk(), "u5 should exist");
            userService.logoutRegistered(res.getData());

            res = userService.loginUser(ST, "u6", "u6");
            assertTrue(res.isOk(), "u6 should exist");
            userService.logoutRegistered(res.getData());

            ST = userService.loginUser(ST, "u2", "u2").getData();
            Response<ShopDTO> res2 = shopService.getShopInfo(ST, 0);
            assertTrue(res.isOk(), "s1 should exist");




        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }



    @Test
    public void name(){
        String content = """
                register-user(u1, u1, 1999-06-16);
                register-user(u2, u2, 2000-01-01);
                register-user(u3, u3, 2000-01-01);
                register-user(u4, u4, 2000-01-01);
                login-user(u2, u2);
                createShop(s1, firstShop);
                addShopOwner(0, u3);
                addShopManager(0, u4, UPDATE_ITEM_QUANTITY);
                addItem(0, item1, FOOD, 20 , item1item1 , 10 );
                addItem(0, item2, FOOD, 20 , item2item2 , 10 );
                addItem(0, item3, FOOD, 20 , item3item3 , 10 );
                removeItem(0, 1);
                changeItemPrice(0, 0, 15);
                changeItemQuantity(0, 0, 3);
                changeItemName(0, 0, item0);
                changeItemDescription(0, 0, item0item0);
                logoutUser();
                login-User(u4,u4)
                changeItemQuantity(0, 2, 5);
                logoutUser()
    """;
            try {
                runInit(content);





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
           login-user(u2, u2);
           createShop(s1, firstShop);
           addItem(0, item1, FOOD, 20 , item1item1 , 10 );
           addItem(0, item2, FOOD, 20 , item2item2 , 10 );
           logoutUser();
           login-user(u3, u3);
           addToCart(0, 0, 5);
           addToCart(0, 1, 6)
           removeFromCart(0, 1);
           rateShop(0, 3);
           rateItem(0, 0, 2);
           sendMessage(0, test1 , testing messages);
           logoutUser();
           login-user(u2,u2);
           respondToMessage(0,  0, test1good, test1 went well);
           logoutUser();
    """;
        try {
            runInit(content);





        } catch (Exception E){
            fail("something went wrong: "+ E.getMessage());
        }

    }

    @Test
    public void FailInitialized(){


    }
}
