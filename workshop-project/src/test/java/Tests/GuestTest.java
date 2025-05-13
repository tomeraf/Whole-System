package Tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.halilovindustries.backend.Domain.User.Guest;

public class GuestTest {
    private Guest guest;

    @BeforeEach
    public void setUp() {
        guest = new Guest(); // Use any dummy role
    }

    @Test
    public void testSuccessfulLogin() {
        boolean result = guest.enterToSystem("1", 0);
        assertTrue(result);
        assertTrue(guest.isInSession());
        assertEquals(0, guest.getUserID());
    }

    @Test
    public void testLoginTwiceShouldNotOverride() {
        boolean result1 = guest.enterToSystem("1", 0);
        try {
            guest.enterToSystem("1", 1); // Should not take effect
            fail("Should have thrown an exception");
        }catch (IllegalArgumentException e){
            assertTrue(result1); // First login should succeed
            assertTrue(guest.isInSession());
            assertEquals(0, guest.getUserID());
        }
    }

    @Test
    public void testSuccessfulLogout() {
        boolean result1 = guest.enterToSystem("1", 0);
        assertTrue(result1); // First login should succeed
        assertTrue(guest.isInSession());
        assertEquals(0, guest.getUserID());
        assertNotNull(guest.getCart()); // Cart should be initialized on login
        boolean result2 = guest.logout();
        assertTrue(result2); // Logout should succeed
        assertFalse(guest.isInSession());
        assertEquals(-1, guest.getUserID());
        assertNull(guest.getCart());
    }

    @Test
    public void testLogoutWithoutLogin() {
        boolean result = guest.logout();

        assertFalse(result);
    }

    @Test
    public void testDoubleLogout() {
        guest.enterToSystem("1", 0);
        assertTrue(guest.logout());
        assertFalse(guest.logout()); // Second one should fail

        assertFalse(guest.isInSession());
        assertEquals(-1, guest.getUserID());
        assertNull(guest.getCart());
    }

    @Test
    public void testLoginWithNegativeID() {
        try {
            guest.enterToSystem("1", -1);
            fail("Should have thrown an exception");
        }catch (IllegalArgumentException e){
            assertTrue(true);
        }

    }
}
