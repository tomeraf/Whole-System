package Tests;

import com.halilovindustries.backend.Domain.*;
import com.halilovindustries.backend.Domain.User.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class RegisteredTest {
    private static final int APPOINTER_ID = 1;
    private static final int APPOINTEE_ID = 2;
    private static final int SHOP_ID = 10;
    private Registered user;


    @BeforeEach
    public void setUp() {
        Guest guest = new Guest();
        guest.enterToSystem("guestSessionToken", APPOINTER_ID); // Assuming a valid session token and cart ID
        user = guest.register("bob", "hunter2", LocalDate.of(1990, 1, 1));
        user.setSessionToken("userSessionToken"); // login
    }

    @Test
    void testAddPermission() {
        Manager mgrRole = new Manager(APPOINTER_ID, SHOP_ID, new HashSet<>());
        user.setRoleToShop(SHOP_ID, mgrRole);

        assertTrue(user.addPermission(SHOP_ID, Permission.VIEW));
        assertTrue(mgrRole.hasPermission(Permission.VIEW));
    }

    @Test
    void testAddPermissionNoRole() {
        try{
             user.addPermission(SHOP_ID, Permission.VIEW);
            fail("didnt catch");
        }catch (IllegalArgumentException e){
            assertFalse(false);
        }
    }

    @Test
    public void testRemovePermission() {
        Manager mgrRole = new Manager(APPOINTER_ID, SHOP_ID, new HashSet<>(Collections.singleton(Permission.VIEW)));
        user.setRoleToShop(SHOP_ID, mgrRole);

        assertTrue(user.removePermission(SHOP_ID, Permission.VIEW));
        assertFalse(mgrRole.hasPermission(Permission.VIEW));
    }

    @Test
    public void testRemovePermissionNoRole() {
        try{
            user.removePermission(SHOP_ID, Permission.VIEW);
            fail("didnt catch");
        }catch (IllegalArgumentException e){
            assertFalse(false);
        }
    }

    @Test
    public void testAddAppointmentSuccess() {
        Registered appointer = user;
        // Owner always has APPOINTMENT permission
        Owner owner = new Owner(APPOINTER_ID, SHOP_ID);
        appointer.setRoleToShop(SHOP_ID, owner);
        Registered registered = new Registered("alice", "eve", LocalDate.of(2000, 1, 1));
        Manager appointeeRole = new Manager(registered.getUserID(), SHOP_ID, new HashSet<>());
        appointeeRole.setUser(registered);
        assertTrue(appointer.addManager(SHOP_ID, APPOINTEE_ID, appointeeRole));

        Map<Integer, IRole> apps = appointer.getAppointments(SHOP_ID);
        assertNotNull(apps);
        assertTrue(apps.containsKey(registered.getUserID()));
        assertEquals(APPOINTER_ID, appointer.getAppointer(SHOP_ID));
    }

    @Test
    public void testAddAppointmentNoPermission() {
        Registered appointer = user;

        // Manager without APPOINTMENT permission
        Manager manager = new Manager(APPOINTER_ID, SHOP_ID, new HashSet<>());
        appointer.setRoleToShop(SHOP_ID, manager);

        Guest guest = new Guest();
        guest.enterToSystem("guestSessionToken", APPOINTEE_ID); // Assuming a valid session token and cart ID
        Registered alice = guest.register("Alice", "password", LocalDate.of(2000, 1, 1));
        alice.setSessionToken("aliceSessionToken"); // login
    
        Registered appointee = alice;
        Manager appointeeRole = new Manager(APPOINTEE_ID, SHOP_ID, new HashSet<>());
        
        assertThrows(IllegalArgumentException.class, () -> appointer.addManager(SHOP_ID, APPOINTEE_ID, appointeeRole));

        assertTrue(appointer.getAppointments(SHOP_ID).isEmpty());             // no appointments recorded
        assertThrows(IllegalArgumentException.class, ()->appointee.getAppointer(SHOP_ID));         // no appointer
    }

    @Test
    public void testRemoveAppointmentSuccess() {
        Registered appointer = user;
        appointer.setCart(new ShoppingCart(APPOINTER_ID));
        Owner owner = new Owner(APPOINTER_ID, SHOP_ID);
        appointer.setRoleToShop(SHOP_ID, owner);

        Registered registered = new Registered("alice", "eve", LocalDate.of(2000, 1, 1));
        registered.setCart(new ShoppingCart(APPOINTEE_ID));
        Owner appointeeRole = new Owner(registered.getUserID(), SHOP_ID);
        registered.setRoleToShop(SHOP_ID, appointeeRole);
        appointeeRole.setUser(registered);

        assertTrue(appointer.addOwner(SHOP_ID, registered.getUserID(), appointeeRole));

        List<Integer> ids = appointer.removeAppointment(SHOP_ID, registered.getUserID());
        assertTrue(ids.size() == 1);
        assertTrue(appointer.getAppointments(SHOP_ID).isEmpty());
    }

    @Test
    public void testRemoveAppointmentNoRole() {
        Registered appointer = user;
        try{
            appointer.removeAppointment(SHOP_ID, APPOINTEE_ID);
            fail("didnt catch");
        }catch (IllegalArgumentException e){
            assertFalse(false);
        }
    }

    @Test
    public void testGetAppointmentsNoRole() {
        try{
            user.getAppointments(SHOP_ID);
            fail("didnt catch");
        }catch (IllegalArgumentException e){
            assertFalse(false);
        }
    }

    @Test
    public void testGetAppointerNoRole() {
        assertThrows(IllegalArgumentException.class,()-> user.getAppointer(SHOP_ID));
    }

    @Test
    public void testGetAgeAlwaysZero() {
        Registered user = new Registered("d", "d", LocalDate.of(LocalDate.now().getYear()-10, 5, 5));
        assertEquals(10, user.getAge());
    }

    @Test
    public void testRemoveShopRoleSuccess() {
        Owner owner = new Owner(APPOINTER_ID, SHOP_ID);
        user.setRoleToShop(SHOP_ID, owner);

        assertTrue(user.removeRoleFromShop(SHOP_ID));
        assertNull(user.getRoleInShop(SHOP_ID));
    }

    @Test
    public void testRemoveShopRoleNoRole() {
        Registered user = new Registered("bob", "hunter2", LocalDate.of(1990, 1, 1));
            assertFalse(user.removeRoleFromShop(SHOP_ID));
    }
}
