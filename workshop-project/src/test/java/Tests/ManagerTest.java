package Tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

import org.junit.Test;

import com.halilovindustries.backend.Domain.Manager;
import com.halilovindustries.backend.Domain.Permission;

public class ManagerTest {
    private Manager manager;

    @Test
    public void testHasPermission() {
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        assertTrue(manager.hasPermission(Permission.UPDATE_ITEM_QUANTITY));
    }
    @Test
    public void testHasNoPermission() {
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        assertFalse(manager.hasPermission(Permission.VIEW));
    }

    @Test
    public void testAddPermission() {
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        manager.addPermission(Permission.VIEW);
        assertTrue(manager.hasPermission(Permission.VIEW));
    }
    @Test
    public void testAddDuplicatePermission() {
        // Adding the same permission again should not change anything
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        manager.addPermission(Permission.UPDATE_ITEM_QUANTITY);
        assertTrue(manager.hasPermission(Permission.UPDATE_ITEM_QUANTITY));
    }
    @Test
    public void testRemovePermission() {
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        manager.removePermission(Permission.UPDATE_ITEM_QUANTITY);
        assertFalse(manager.hasPermission(Permission.UPDATE_ITEM_QUANTITY));
    }
    @Test
    public void testRemoveNonExistentPermission() {
        // Trying to remove a permission that doesn't exist should not change anything
        int appointerID = 1;
        int shopID = 101;
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.UPDATE_ITEM_QUANTITY);
        manager = new Manager(appointerID, shopID, permissions);
        manager.removePermission(Permission.VIEW);
        assertFalse(manager.hasPermission(Permission.VIEW));
    }
}
