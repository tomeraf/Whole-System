package com.halilovindustries.backend.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import com.halilovindustries.backend.Domain.Guest;
import com.halilovindustries.backend.Domain.Registered;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IAuthentication;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private IUserRepository userRepository;
    private IAuthentication jwtAdapter;
    private final ConcurrencyHandler concurrencyHandler;

    ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(IUserRepository userRepository, IAuthentication jwtAdapter, ConcurrencyHandler concurrencyHandler) {
        this.userRepository = userRepository;
        this.jwtAdapter = jwtAdapter;
        this.concurrencyHandler = concurrencyHandler;
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
    public boolean verifyPassword(String password, String hashedPassword) {
        return passwordEncoder.matches(password, hashedPassword);
    }

    /**
     * Enters the system as a guest, generates a session token, and persists the user.
     *
     * @return the newly generated session token for the guest
     */
    public Response<String> enterToSystem() {
        logger.info(() -> "User entered the system");
        int guestUserID = userRepository.getIdToAssign(); // Get a unique ID for the guest user
        Guest guest = new Guest();

        String sessionToken = jwtAdapter.generateToken(guestUserID+"");
        
        guest.enterToSystem(sessionToken, guestUserID);
        userRepository.saveUser(guest); // Save the guest user in the repository
        return Response.ok(sessionToken);
    }

    /**
     * Exits a guest session by validating and removing the guest from the repository.
     *
     * @param sessionToken the token of the guest session to terminate
     */
    public Response<Void> exitAsGuest(String sessionToken) {
        try {
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            userRepository.removeGuestById(userID); // Adds to the "reuse" list
            logger.info(() -> "User exited the system");
            return Response.ok();
        } catch (Exception e) {
            logger.error(() -> "Error exiting the system: " + e.getMessage());
            return Response.error("Error exiting the system: " + e.getMessage());
        }
    }

    /**
     * Logs out a registered user, converts back to a guest session, and returns a new token.
     *
     * @param sessionToken the current token of the registered user
     * @return a new session token as a guest, or empty string on failure
     */
    public Response<String> logoutRegistered(String sessionToken) {
    // After logout - the user remains in the system, as guest
        try {
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            // THIS IS REGISTERED USER - NOT GUEST
            Guest user = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            user.logout();
            // Nothing to do, everything is saved in DB
            Response<String> newToken = enterToSystem();
            return Response.ok(newToken.getData());
        } catch (Exception e) {
            logger.error(() -> "Logout Error: " + e.getMessage());
            return Response.error("Logout Error: " + e.getMessage());
        }
    }

    /**
     * Registers a new user using the provided credentials and date of birth.
     * The guest keeps the same session token and is upgraded to Registered.
     *
     * @param sessionToken the current guest session token
     * @param username desired username
     * @param password desired password
     * @param dateOfBirth user's date of birth
     */
    public Response<Void> registerUser(String sessionToken, String username, String password, LocalDate dateOfBirth) {
        ReentrantLock usernameLock = concurrencyHandler.getUsernameLock(username);

        try {
            usernameLock.lockInterruptibly();  // lock specifically for that username
        
            try {
                if (!jwtAdapter.validateToken(sessionToken)) {
                    throw new Exception("User is not logged in");
                }
    
                int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
                Guest guest = userRepository.getUserById(userID);
                // if getUsername() is non-null, they’re already registered
                if (guest.getUsername() != null) {
                    throw new Exception("Unauthorized register attempt for ID=" + userID);
                }
                
                Registered registered = guest.register(username, encodePassword(password), dateOfBirth);
                userRepository.removeGuestById(userID); // Remove the guest from the repository
                userRepository.saveUser(registered);
                return Response.ok();
            } catch (Exception e) {
                logger.error(() -> "Error registering user: " + e.getMessage());
                return Response.error("Error registering user: " + e.getMessage());
            }

            finally {
                usernameLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(() -> "Registration interrupted for username: " + username);
            return Response.error("Registration interrupted for username: " + username);
        }
    }

    /**
     * Authenticates and logs in a registered user, issuing a new session token.
     *
     * @param sessionToken current guest session token
     * @param username registered user's username
     * @param password registered user's password
     * @return the new session token if login succeeds, or null on failure
     */
    public Response<String> loginUser(String sessionToken, String username, String password) {
        try {
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }

            Registered registered = userRepository.getUserByName(username);
            
            //should throw exception if user not found in the repository
            if (!verifyPassword(password, registered.getPassword())) {
                throw new Exception("Username and password do not match");
            }
            
            String newSessionToken = jwtAdapter.generateToken(registered.getUserID()+"");
            registered.setSessionToken(newSessionToken); // Set the session token for the registered user
            
            int guestUserID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            Guest maybeOld = userRepository.getUserById(guestUserID);
            if (registered.getUserID() != maybeOld.getUserID())    // only true Guests return null
                userRepository.removeGuestById(guestUserID);
            
            logger.info(() -> "User logged in successfully");
            return Response.ok(newSessionToken);
        } catch (Exception e) {
            logger.error(() -> "Error logging in user: " + e.getMessage());
            return Response.error("Error logging in user: " + e.getMessage());
        }
    }

}
