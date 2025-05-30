package Tests.AcceptanceTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Acceptance tests for user authentication functionality.
 * Tests guest operations, registration, login, and logout processes.
 */
@SpringBootTest(classes = com.halilovindustries.Application.class)
public class UserAuthenticationTests extends BaseAcceptanceTests {

    // Guest operations tests
    
    @Test
    public void testEnterSystemAsGuest_ShouldCreateTokenAndEmptyCart() {
        // Act
        Response<String> guestToken = userService.enterToSystem();
        
        // Assert
        assertNotNull(guestToken.getData(), "Guest login failed: token is null.");
        
        Response<List<ItemDTO>> items = orderService.checkCartContent(guestToken.getData());
        assertNotNull(items.getData(), "Guest login failed: cart content is null.");
        assertTrue(items.getData().isEmpty(), "Guest login failed: cart is not empty.");
    }

    @Test
    public void testExitSystemAsGuest_ShouldInvalidateSession() {
        // Arrange
        Response<String> guestToken = userService.enterToSystem();
        assertNotNull(orderService.checkCartContent(guestToken.getData()).getData(), "Guest login failed: cart content is null.");
        
        // Act
        Response<Void> exitResponse = userService.exitAsGuest(guestToken.getData());
        
        // Assert
        assertTrue(exitResponse.isOk(), "Guest exit failed");
        assertNull(orderService.checkCartContent(guestToken.getData()).getData(), "Session should be invalidated after exit - the cart should be null");
    }

    // Registration tests
    
    @Test
    public void testRegisterNewUser_WithValidDetails_ShouldSucceed() {
        // Arrange
        Response<String> guestToken = userService.enterToSystem();
        
        // Act
        Response<Void> registrationResponse = userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        
        // Assert
        assertTrue(registrationResponse.isOk(), "Registration should succeed with valid details");
        
        Response<String> userToken = userService.loginUser(guestToken.getData(), "user123", "password");
        assertTrue(userToken.isOk(), "Login should succeed after registration");
        assertNotNull(userToken.getData(), "Login should provide valid token after registration");
        
        Response<List<ItemDTO>> cart = orderService.checkCartContent(userToken.getData());
        assertNotNull(cart.getData(), "Cart should exist after registration");
        assertTrue(cart.getData().isEmpty(), "Cart should be empty after registration");
    }

    @Test
    public void testRegisterDuplicateUsername_ShouldFail() {
        // Arrange
        Response<String> guestToken1 = userService.enterToSystem();
        Response<Void> firstRegistration = userService.registerUser(
            guestToken1.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        assertTrue(firstRegistration.isOk(), "First registration should succeed");
        
        // Act
        Response<String> guestToken2 = userService.enterToSystem();
        Response<Void> duplicateRegistration = userService.registerUser(
            guestToken2.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        
        // Assert
        assertFalse(duplicateRegistration.isOk(), "Registration with duplicate username should fail");
    }

    @Test
    public void testRegisterAsLoggedInUser_ShouldFail() {
        // Arrange - Register and login user
        Response<String> guestToken = userService.enterToSystem();
        userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        Response<String> userToken = userService.loginUser(guestToken.getData(), "user123", "password");
        assertTrue(userToken.isOk(), "Login should succeed");
        
        // Act - Try to register again while logged in
        Response<Void> registrationResponse = userService.registerUser(
            userToken.getData(), 
            "anotherUser", 
            "pwd2", 
            LocalDate.now().minusYears(30)
        );
        
        // Assert
        assertFalse(registrationResponse.isOk(), "Registration as logged-in user should fail");
        
        // Verify original session still active
        Response<List<ItemDTO>> cart = orderService.checkCartContent(userToken.getData());
        assertNotNull(cart.getData(), "Original user session should remain active");
        
        // Verify new user wasn't created
        Response<String> newLoginAttempt = userService.loginUser(userToken.getData(), "anotherUser", "pwd2");
        assertFalse(newLoginAttempt.isOk(), "Should not be able to login with unauthorized registration credentials");
    }

    // Login tests
    
    @Test
    public void testLoginUser_WithValidCredentials_ShouldSucceed() {
        // Arrange - Register user
        Response<String> guestToken = userService.enterToSystem();
        userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        
        // Act
        Response<String> loginResponse = userService.loginUser(guestToken.getData(), "user123", "password");
        
        // Assert
        assertTrue(loginResponse.isOk(), "Login should succeed with valid credentials");
        assertNotNull(loginResponse.getData(), "Login should return valid token");
    }

    @Test
    public void testLoginUser_WithInvalidUsername_ShouldFail() {
        // Arrange - Register user
        Response<String> guestToken = userService.enterToSystem();
        userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        
        // Act
        Response<String> loginResponse = userService.loginUser(guestToken.getData(), "wrongUsername", "password");
        
        // Assert
        assertFalse(loginResponse.isOk(), "Login should fail with invalid username");
        assertNull(loginResponse.getData(), "No token should be returned for failed login");
    }

    @Test
    public void testLoginUser_WithInvalidPassword_ShouldFail() {
        // Arrange - Register user
        Response<String> guestToken = userService.enterToSystem();
        userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        
        // Act
        Response<String> loginResponse = userService.loginUser(guestToken.getData(), "user123", "wrongPassword");
        
        // Assert
        assertFalse(loginResponse.isOk(), "Login should fail with invalid password");
        assertNull(loginResponse.getData(), "No token should be returned for failed login");
    }

    // Logout tests
    
    @Test
    public void testLogout_RegisteredUser_ShouldReturnToGuestState() {
        // Arrange - Register and login user
        Response<String> guestToken = userService.enterToSystem();
        userService.registerUser(
            guestToken.getData(), 
            "user123", 
            "password", 
            LocalDate.now().minusYears(20)
        );
        Response<String> userToken = userService.loginUser(guestToken.getData(), "user123", "password");
        assertTrue(userToken.isOk(), "Login should succeed");
        
        // Act - Logout
        Response<String> logoutResponse = userService.logoutRegistered(userToken.getData());
        
        // Assert
        assertTrue(logoutResponse.isOk(), "Logout should succeed");
        
        // Verify user is back to guest state
        Response<Void> guestExitResponse = userService.exitAsGuest(logoutResponse.getData());
        assertTrue(guestExitResponse.isOk(), "Should be able to exit as guest after logout");
    }

    @Test
    public void testConcurrentRegistrationWithSameUsername_ShouldAllowOnlyOneSuccess() throws InterruptedException {
        PaymentDetailsDTO p = new PaymentDetailsDTO(
            "1234567890123456", "Some Name", "1","12/25", "123"
        );
        ShipmentDetailsDTO s = new ShipmentDetailsDTO("1", "Some Name", "", "123456789", "Some Country", "Some City", "Some Address", "12345");
        
        // Arrange - Mock external services
        when(payment.validatePaymentDetails(p)).thenReturn(true);
        when(shipment.validateShipmentDetails(s)).thenReturn(true);

        // Run multiple iterations to increase chance of catching race conditions
        for (int i = 0; i < 10; i++) {
            // Create test environment
            fixtures.generateRegisteredUserSession("owner"+i, "pwdO"+i);
            String guest1 = userService.enterToSystem().getData();
            String guest2 = userService.enterToSystem().getData();
            String desiredUsername = "dupUser"+i;

            // Act - Create two concurrent registration tasks with the same username
            List<Callable<Response<Void>>> registrationTasks = List.of(
                () -> userService.registerUser(guest1, desiredUsername, "pw", LocalDate.now().minusYears(20)),
                () -> userService.registerUser(guest2, desiredUsername, "pw", LocalDate.now().minusYears(20))
            );

            // Execute both tasks concurrently
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            List<Future<Response<Void>>> futures = executorService.invokeAll(registrationTasks);
            executorService.shutdown();

            // Count successful registrations
            long successCount = futures.stream()
                .map(future -> {
                    try {
                        return future.get().isOk();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .filter(ok -> ok)
                .count();

            // Assert - Exactly one registration should succeed
            assertEquals(1, successCount, 
                "Iteration " + i + ": Exactly one registration should succeed when two users attempt to register with the same username");
        }
    }
}