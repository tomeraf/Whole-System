package Tests;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import com.halilovindustries.backend.Domain.User.IRole;
import com.halilovindustries.backend.Domain.User.Manager;
import com.halilovindustries.backend.Domain.User.Owner;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.User.Registered;

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
        Registered registered = new Registered("bob", "hunter2", LocalDate.of(1990, 1, 1));
        Manager manager=new Manager(2, shopID, new HashSet<Permission>());
        manager.setUser(registered);
        owner.addManager(manager);
        assertTrue(owner.getAppointments().size() == 1);
    }
    @Test
    public void testRemoveAppointment() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;    
        Registered registered = new Registered("bob", "hunter2", LocalDate.of(1990, 1, 1));
        owner = new Owner(appointerID, shopID);
        Manager appointeeRole = new Manager(2, shopID, new HashSet<>());
        appointeeRole.setUser(registered);
        owner.addManager(appointeeRole);
        registered.setRoleToShop(101, appointeeRole);
        owner.removeAppointment(2);
        assertTrue(owner.getAppointments().size() == 0);
    }
    @Test
    public void testGetAppointer() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        owner.addManager(new Manager(2, shopID, new HashSet<Permission>()));
        assertEquals(-1, owner.getAppointer());
    }
    @Test
    public void testGetShopID() {
        int appointerID = -1; // -1 for founder
        int shopID = 101;
        owner = new Owner(appointerID, shopID);
        Registered registered = new Registered("bob", "hunter2", LocalDate.of(1990, 1, 1));
        Manager manager=new Manager(2, shopID, new HashSet<Permission>());
        manager.setUser(registered);
        owner.addManager(manager);
        assertEquals(101, owner.getShopID());
    }
}
