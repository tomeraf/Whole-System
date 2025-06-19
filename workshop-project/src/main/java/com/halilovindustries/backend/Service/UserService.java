package com.halilovindustries.backend.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import com.halilovindustries.backend.Domain.User.*;

import jakarta.transaction.Transactional;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ConcurrencyHandler;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IAuthentication;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.MaintenanceModeException;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import com.halilovindustries.backend.Domain.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends DatabaseAwareService {

    private IUserRepository userRepository;
    private IAuthentication jwtAdapter;
    private final ConcurrencyHandler concurrencyHandler;
    private NotificationHandler notificationHandler;

    ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserService(IUserRepository userRepository, IAuthentication jwtAdapter, ConcurrencyHandler concurrencyHandler,
            NotificationHandler notificationHandler) {
        this.userRepository = userRepository;
        this.jwtAdapter = jwtAdapter;
        this.concurrencyHandler = concurrencyHandler;
        this.notificationHandler = notificationHandler;
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
    @Transactional
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
    @Transactional
    public Response<Void> exitAsGuest(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            userRepository.removeGuestById(userID); // Adds to the "reuse" list
            logger.info(() -> "User exited the system");
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
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
    @Transactional
    public Response<String> logoutRegistered(String sessionToken) {
    // After logout - the user remains in the system, as guest
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            // THIS IS REGISTERED USER - NOT GUEST
            Guest user = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            user.logout();
            // Nothing to do, everything is saved in DB
            Response<String> newToken = enterToSystem();
            return Response.ok(newToken.getData());
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
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
    @Transactional
    public Response<Void> registerUser(String sessionToken, String username, String password, LocalDate dateOfBirth) {
        ReentrantLock usernameLock = concurrencyHandler.getUsernameLock(username);

        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            usernameLock.lockInterruptibly();  // lock specifically for that username
        
            try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
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
                
                if (userRepository.getUserByName(username) != null)
                    throw new Exception("Username already exists");
                userRepository.removeGuestById(userID); // Remove the guest from the repository
                userRepository.saveUser(registered);
                // should be actually inside guest.register..
                return Response.ok();
            } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
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
    @Transactional
    public Response<String> loginUser(String sessionToken, String username, String password) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            if (userRepository.getUserByName(username) == null)
                throw new Exception("Username: " + username + " does not exist");

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
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error logging in user: " + e.getMessage());
            return Response.error("Error logging in user: " + e.getMessage());
        }
    }
    //@Transactional
    public Response<Void> loginNotify(String sessionToken){
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            notificationHandler.notifyUser(userID+"");// delayed notifications
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error notifying login: " + e.getMessage());
            return Response.error("Error notifying login: " + e.getMessage());
        }
    }
    

    //SystemManager only(need to decide how to implement system manager)
    //requirement:2.6.6
    @Transactional
    public Response<Void> suspendUser(String sessionToken,String username,Optional<LocalDateTime> startDate, Optional<LocalDateTime> endDate) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            Guest guest = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            if(!guest.isSystemManager()) {
                throw new Exception("User is not a system manager");
            }
            Registered user = userRepository.getUserByName(username);
            user.addSuspension(startDate, endDate);
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error suspending user: " + e.getMessage());
            return Response.error("Error suspending user: " + e.getMessage());
        }
    }
    //requirement:2.6.7
    @Transactional
    public Response<Void> unsuspendUser(String sessionToken,String username) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            Guest guest = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            if(!guest.isSystemManager()) {
                throw new Exception("User is not a system manager");
            }
            Registered user = userRepository.getUserByName(username);
            user.removeSuspension();
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error unsuspending user: " + e.getMessage());
            return Response.error("Error unsuspending user: " + e.getMessage());
        }
    }
    //requirement:2.6.8
    @Transactional
    public Response<String> watchSuspensions(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            Guest guest = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            if(!guest.isSystemManager()) {
                throw new Exception("User is not a system manager");
            }
            List<Registered> users = userRepository.getAllRegisteredUsers();
            StringBuilder sb = new StringBuilder();
            for (Registered user : users) {
                sb.append(user.showSuspension());
            }
            return Response.ok(sb.toString());
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error watching suspensions: " + e.getMessage());
            return Response.error("Error watching suspensions: " + e.getMessage());
        }
    }

    @Transactional
    public boolean isLoggedIn(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            return userRepository.getUserById(userID) != null && 
                   userRepository.getUserById(userID).getUsername() != null;
        } 
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error checking login status: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public Response<List<Message>> getInbox(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            Registered user = (Registered) this.userRepository.getUserById(userID);
            List<Message> inbox = user.getInbox();
            return Response.ok(inbox);
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error getting inbox: " + e.getMessage());
            return Response.error("Error: " + e.getMessage());
        }
    }
    @Transactional
    public boolean inSystem(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            return userRepository.getUserById(userID) != null;
        } 
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error checking invalid tokens: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public String getUsername(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            int userID = Integer.parseInt(jwtAdapter.getUsername(sessionToken));
            return userRepository.getUserById(userID).getUsername();
        } 
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error getting username: " + e.getMessage());
            return null;
        }
    }

    @Transactional
    public Response<Void> isSystemManager(String sessionToken) {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            if (!jwtAdapter.validateToken(sessionToken)) {
                throw new Exception("User is not logged in");
            }
            Guest guest = userRepository.getUserById(Integer.parseInt(jwtAdapter.getUsername(sessionToken)));
            if(!guest.isSystemManager()) {
                throw new Exception("User is not a system manager");
            }
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    /**
     * Checks if at least one system manager exists in the system
     * 
     * @return true if at least one system manager exists, false otherwise
     */
    public boolean hasSystemManager() {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            List<Registered> users = userRepository.getAllRegisteredUsers();
            for (Registered user : users) {
                if (user.isSystemManager()) {
                    return true;
                }
            }
            return false;
        } 
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error checking system manager existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Promotes a registered user to system manager
     * 
     * @param username the username of the user to promote
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<Void> makeSystemManager(String username) 
    {
        try {
            // Check database health before proceeding
            checkDatabaseHealth("current method");
            Registered user = userRepository.getUserByName(username);
            if (user == null) {
                throw new Exception("User not found");
            }
            if (user.isSystemManager()) {
                throw new Exception("User is already a system manager");
            }
            user.setSystemManager(true);
            userRepository.saveUser(user);
            return Response.ok();
        } 
        catch (MaintenanceModeException e) {
            // Special handling for maintenance mode
            return Response.error(e.getMessage());
        }
        catch (Exception e) {
            handleDatabaseException(e);
            logger.error(() -> "Error making system manager: " + e.getMessage());
            return Response.error("Error making system manager: " + e.getMessage());
        }
    }
}
