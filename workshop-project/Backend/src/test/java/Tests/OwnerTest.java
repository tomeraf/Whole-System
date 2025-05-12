package Tests;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import Domain.User.*;

public class OwnerTest {
    private Owner owner;       
     Map<Integer,IRole> appointments;

    @Test
    public void testHasPermission() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        appointments = owner.getAppointments();
        assertTrue(owner.hasPermission(Permission.UPDATE_ITEM_QUANTITY));
    }

    @Test
    public void testAddAppointment() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        assertTrue(owner.getAppointments().size() == 0);
        owner.addManager(2, new Manager(2, shopID, new HashSet<Permission>()));
        assertTrue(owner.getAppointments().size() == 1);
    }
    @Test
    public void testRemoveAppointment() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;    
        Registered registered = new Registered("bob", "hunter2", LocalDate.of(1990, 1, 1));
        owner = new Owner(appointerID, shopID);
        Manager appointeeRole = new Manager(2, shopID, new HashSet<>());
        owner.addManager(2, appointeeRole);
        registered.setRoleToShop(101, appointeeRole);
        owner.removeAppointment(2);
        assertTrue(owner.getAppointments().size() == 0);
    }
    @Test
    public void testGetAppointer() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        owner.addManager(2, new Manager(2, shopID, new HashSet<Permission>()));
        assertEquals(-1, owner.getAppointer());
    }
    @Test
    public void testGetShopID() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        owner.addManager(2, new Manager(2, shopID, new HashSet<Permission>()));
        assertEquals(101, owner.getShopID());
    }
}
