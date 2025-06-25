package com.halilovindustries.backend.Domain.init;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.halilovindustries.backend.Service.NotificationHandler;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class Initializer {

    private final UserService userService;
    private final ShopService shopService;
    private final OrderService orderService;
    private String initST;
    private final StartupConfig initConfig;
    private final DatabaseHealthService databaseHealthService;

    @Autowired
    public Initializer( StartupConfig initConfig, UserService userService, ShopService shopService, OrderService orderService, DatabaseHealthService databaseHealthService) {
        this.userService = userService;
        this.shopService = shopService;
        this.orderService = orderService;
        this.initConfig = initConfig;
        this.databaseHealthService = databaseHealthService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Check if database is available
        boolean dbAvailable = databaseHealthService.isDatabaseConnected();
        
        // Check if init file is valid for maintenance mode
        boolean validForMaintenance = isInitFileValidForMaintenanceMode();
        
        if (dbAvailable) {
            // Database is available, proceed with initialization
            System.out.println("Database is available. Initializing system...");
            init();
        } else if (validForMaintenance) {
            // Database is down but the init file is empty or just has comments
            System.out.println("Database is unavailable but init file is valid for maintenance mode.");
        } else {
            // Database is down and init file has commands
            System.err.println("ERROR: Database is unavailable and init file contains commands. System cannot start.");
            // You could throw an exception here to stop the application
            throw new RuntimeException("Cannot start system: Database unavailable and init file requires database access");
        }
    }

    public void init() {
        System.out.println("Initializing application data...");
        
        // Get a session token first
        initST = userService.enterToSystem().getData();

        // Then proceed with other initialization
        readAndApply();
        
        // check if a system manager exists and create one if not
        ensureSystemManagerExists();
    }

    private void readAndApply() {
        /*try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(initConfig.getInitFile())), StandardCharsets.UTF_8))) {




        try(BufferedReader reader = Files.newBufferedReader(Path.of(initConfig.getInitFile()), StandardCharsets.UTF_8);
        ){



         */

        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(initConfig.getInitFile());
            BufferedReader reader;

            if (stream != null) {
                // Read from classpath
                reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } else {
                // Read from file system
                reader = Files.newBufferedReader(Path.of(initConfig.getInitFile()), StandardCharsets.UTF_8);
            }


            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {

                    boolean success = checkingSyntax(line);
                    if (!success) {
                        throw new RuntimeException("Syntax checking failed at line " + lineNumber + ": " + line);
                    }
                }

                lineNumber++;
            }

            System.out.println("All Syntax checking commands completed successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to read init file: " + e.getMessage(), e);
        }

        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(initConfig.getInitFile());
            BufferedReader reader;

            if (stream != null) {
                // Read from classpath
                reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } else {
                // Read from file system
                reader = Files.newBufferedReader(Path.of(initConfig.getInitFile()), StandardCharsets.UTF_8);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {

                    boolean success = processCommand(line);
                    if (!success) {
                        throw new RuntimeException("Processing commands failed at line " + lineNumber + ": " + line);
                    }
                }

                lineNumber++;
            }

            System.out.println("All init commands completed successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to process init file: " + e.getMessage(), e);
        }
    }

    private String[][] lineParsed(String line) {
        String[][] ret = new String[2][];
        ret[0]=new String[1];
        ret[0][0]="";
        ret[1]= new String[0];

        // Ignore empty lines or comments
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) return ret;

        // Basic syntax check
        if (!line.endsWith(";") || !line.contains("(")) {
            System.err.println("Invalid command format: " + line);
            return null;
        }

        // Extract command name
        int parenIndex = line.indexOf('(');
        String command = line.substring(0, parenIndex).trim();

        // Extract arguments inside the parentheses by:
        String argsPart = line.substring(parenIndex + 1, line.lastIndexOf(')')).trim();

        // Split arguments by comma and trim each
        String[] args = Arrays.stream(argsPart.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        ret[0][0] = command;
        ret[1] = args;
        return ret;
    }

    private boolean checkingSyntax(String line) {
            String[][] parts = lineParsed(line);
            if(parts==null)
                return false;

            String command =parts[0][0];
            String[] args = parts[1].clone();

            try{

            switch (command) {
                case "":{
                    return true;
                }
                case "register-user": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("register-user should have 3 args");
                    return true;
                }

                case "make-system-manager": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("make-system-manager should have 1 arg: username");
                    return true;
                }

                case "login-user": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("login-user should have 2 args");
                    return true;
                }

                case "logout-user": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("logout-user should have 1 arg: username");
                    return true;
                }
                case "exit-as-guest": {
                    if (args.length != 0)
                        throw new IllegalArgumentException("logout-guest should have 0 args");
                    return true;
                }

                case "create-shop": {
                    if (args.length < 2)
                        throw new IllegalArgumentException("create-shop should have 2 args");
                    return true;
                }

                case "add-shop-manager": {
                    if (args.length < 3)
                        throw new IllegalArgumentException("add-shop-manager should have at least 3 args");
                    return true;
                }
                case "add-shop-owner": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("add-shop-owner should have 2 args");
                    return true;
                }

                case "add-item": {
                    if (args.length != 6)
                        throw new IllegalArgumentException("add-item should have 6 args (the last is quantity)");
                    return true;
                }

                case "remove-item": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-item should have 2 args");
                    return true;
                }


                case "change-item-price": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-price should have 3 args");
                    return true;
                }

                case "change-item-quantity": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("set-item-quantity requires 3 args");
                    return true;
                }


                case "change-item-name": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-name should have 3 args");
                    return true;
                }

                case "change-item-description": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-description should have 3 args");
                    return true;
                }

                case "rate-shop": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("rate-shop should have 2 args");
                    return true;
                }

                case "rate-item": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("rate-item should have 3 args");
                    return true;
                }

                case "close-shop": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("close-shop should have 1 arg");
                    return true;
                }

                case "send-message": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("send-message should have 3 args");
                    return true;
                }

                case "respond-to-message": {
                    if (args.length != 4)
                        throw new IllegalArgumentException("respond-to-message should have 4 args");
                    return true;
                }

                case "open-auction": {
                    if (args.length != 5)
                        throw new IllegalArgumentException("open-auction requires 5 args");
                    return true;
                }

                case "answer-bid": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("answer-bid requires 3 args: shopId, bidId, accept(true/false)");
                    return true;
                }

                case "submit-counter-bid": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("submit-counter-bid requires 3 args");
                    return true;
                }

                case "update-discount-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-discount-type requires 2 args");
                    return true;
                }

                case "update-purchase-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-purchase-type requires 2 args");
                    return true;
                }

                case "add-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("add-permission requires 3 args");
                    return true;
                }

                case "remove-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("remove-permission requires 3 args");
                    return true;
                }

                case "remove-appointment": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-appointment requires 2 args");
                    return true;
                }

                case "add-to-cart": {
                    if (args.length != 3)
                       throw new IllegalArgumentException("add-to-cart should have 3 args");
                    return true;
                }

                case "remove-from-cart": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-from-cart should have 2 args");
                    return true;
                }

                case "suspend": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("suspend should have 3 args: username, startDate, endDate");
                    return true;
                }

                case "unsuspend": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("unsuspend should have 1 arg: username");
                    return true;
                }

                default:
                    System.err.println("Unknown checking command: " + command);
                    return false;
            }

        } catch (Exception e) {
            System.err.println("Error checking syntax of command: " + line);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if the init file is valid for starting in maintenance mode.
     * When database is offline, only empty init files (or those with just comments) are valid.
     * 
     * @return true if the init file is valid for maintenance mode, false otherwise
     */
    public boolean isInitFileValidForMaintenanceMode() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(initConfig.getInitFile())), 
                StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }
                
                // If we found at least one command, the file is not valid for maintenance mode
                return false;
            }
            
            // If we reach here, the file only contains comments or is empty
            return true;
        } catch (Exception e) {
            System.err.println("Error validating init file: " + e.getMessage());
            return false; // If we can't read the file, consider it invalid
        }
    }

    private boolean processCommand(String line) {
        String[][] parts = lineParsed(line);
        if(parts==null)
            return false;
        String command =parts[0][0];
        String[] args = parts[1].clone();

        try{

            switch (command) {
                case "":{
                    return true;
                }

                case "register-user": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("register-user should have 3 args");
                    initST = userService.enterToSystem().getData();
                    Response<Void> res = userService.registerUser(initST, args[0], args[1], LocalDate.parse(args[2]));
                    return res.isOk();
                }

                case "make-system-manager": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("make-system-manager should have 1 arg: username");
                    Response<Void> res = userService.makeSystemManager(args[0]);
                    return res.isOk();
                }

                case "login-user": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("login-user should have 2 args");
                    Response<String> res = userService.loginUser(initST,args[0], args[1]);
                    initST= res.getData();
                    return res.isOk();
                }

                case "logout-user": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("logout-user should have 1 arg: username");
                    Response<String> res = userService.logoutRegistered(initST);
                    initST = res.getData();
                    return res.isOk();
                }
                case "exit-as-guest": {
                    if (args.length != 0)
                        throw new IllegalArgumentException("logout-guest should have 0 args");
                    Response<Void> res = userService.exitAsGuest(initST);
                    return res.isOk();
                }

                case "create-shop": {
                    if (args.length < 2)
                        throw new IllegalArgumentException("create-shop should have 2 args");
                    Response<ShopDTO> res = shopService.createShop(initST, args[0], args[1]);
                    return res.isOk();
                }

                case "add-shop-manager": {
                    if (args.length < 3)
                        throw new IllegalArgumentException("add-shop-manager should have at least 3 args");
                    Set<Permission> permissions = new HashSet<>();
                    for (int i = 2; i < args.length; i++)
                        permissions.add(Permission.valueOf(args[i]));
                    Response<Void> res = shopService.addShopManager(initST, Integer.parseInt(args[0]), args[1], permissions);
                    return res.isOk();
                }
                case "add-shop-owner": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("add-shop-owner should have 2 args");
                    Response<Void> res = shopService.addShopOwner(initST, Integer.parseInt(args[0]), args[1]);
                    return res.isOk();
                }

                case "add-item": {
                    if (args.length != 6)
                        throw new IllegalArgumentException("add-item should have 6 args (the last is quantity)");
                    int shopId = Integer.parseInt(args[0]);
                    String itemName = args[1];
                    Category category = Category.valueOf(args[2].toUpperCase()); // adjust enum case if needed
                    double price = Double.parseDouble(args[3]);
                    String description = args[4];
                    int quantity = Integer.parseInt(args[5]);

                    Response<ItemDTO> res = shopService.addItemToShop(initST, shopId, itemName, category, price, description);

                    if (!res.isOk()) return false;

                    Response<Void> quantityRes = shopService.changeItemQuantityInShop(initST, shopId, res.getData().getItemID(), quantity);

                    return quantityRes.isOk();
                }

                case "remove-item": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-item should have 2 args");
                    Response<Void> res = shopService.removeItemFromShop(initST, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                    return res.isOk();
                }


                case "change-item-price": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-price should have 3 args");
                    Response<Void> res = shopService.changeItemPriceInShop(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Double.parseDouble(args[2]));
                    return res.isOk();
                }

                case "change-item-quantity": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("set-item-quantity requires 3 args");

                    Response<Void> res = shopService.changeItemQuantityInShop(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]));
                    return res.isOk();
                }


                case "change-item-name": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-name should have 3 args");
                    Response<Void> res = shopService.changeItemName(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2]);
                    return res.isOk();
                }

                case "change-item-description": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-description should have 3 args");
                    Response<Void> res = shopService.changeItemDescriptionInShop(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2]);
                    return res.isOk();
                }

                case "rate-shop": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("rate-shop should have 2 args");
                    Response<Void> res = shopService.rateShop(initST, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                    return res.isOk();
                }

                case "rate-item": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("rate-item should have 3 args");
                    Response<Void> res = shopService.rateItem(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]));
                    return res.isOk();
                }

                case "close-shop": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("close-shop should have 1 arg");
                    Response<Void> res = shopService.closeShop(initST, Integer.parseInt(args[0]));
                    return res.isOk();
                }

                case "send-message": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("send-message should have 3 args");
                    Response<Void> res = shopService.sendMessage(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            args[2]);
                    return res.isOk();
                }

                case "respond-to-message": {
                    if (args.length != 4)
                        throw new IllegalArgumentException("respond-to-message should have 4 args");
                    Response<Void> res = shopService.respondToMessage(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2],
                            args[3]);
                    return res.isOk();
                }

                case "open-auction": {
                    if (args.length != 5)
                        throw new IllegalArgumentException("open-auction requires 5 args");

                    int shopId = Integer.parseInt(args[0]);
                    int itemId = Integer.parseInt(args[1]);
                    double startingPrice = Double.parseDouble(args[2]);
                    LocalDate start = LocalDate.parse(args[3]);
                    LocalDate end = LocalDate.parse(args[4]);

                    Response<Void> res = shopService.openAuction(initST, shopId, itemId, startingPrice, start.atStartOfDay(), end.atStartOfDay());
                    return res.isOk();
                }

                case "answer-bid": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("answer-bid requires 3 args: shopId, bidId, accept(true/false)");

                    int shopId = Integer.parseInt(args[0]);
                    int bidId = Integer.parseInt(args[1]);
                    boolean accept = Boolean.parseBoolean(args[2]);

                    Response<Void> res = shopService.answerBid(initST, shopId, bidId, accept);
                    return res.isOk();
                }

                case "submit-counter-bid": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("submit-counter-bid requires 3 args");

                    Response<Void> res = shopService.submitCounterBid(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Double.parseDouble(args[2]));
                    return res.isOk();
                }

                case "update-discount-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-discount-type requires 2 args");

                    Response<Void> res = shopService.updateDiscountType(initST, Integer.parseInt(args[0]), DiscountType.valueOf(args[1]));
                    return res.isOk();
                }

                case "update-purchase-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-purchase-type requires 2 args");

                    Response<Void> res = shopService.updatePurchaseType(initST, Integer.parseInt(args[0]), PurchaseType.valueOf(args[1]));
                    return res.isOk();
                }

                case "add-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("add-permission requires 3 args");

                    Response<Void> res = shopService.addShopManagerPermission(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            Permission.valueOf(args[2]));
                    return res.isOk();
                }

                case "remove-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("remove-permission requires 3 args");

                    Response<Void> res = shopService.removeShopManagerPermission(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            Permission.valueOf(args[2]));
                    return res.isOk();
                }

                case "remove-appointment": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-appointment requires 2 args");

                    Response<Void> res = shopService.removeAppointment(initST, Integer.parseInt(args[0]), args[1]);
                    return res.isOk();
                }

                case "add-to-cart": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("add-to-cart should have 3 args");

                    Response<Void> res = orderService.addItemToCart(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]));
                    return res.isOk();
                }

                case "remove-from-cart": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-from-cart should have 2 args");

                    Response<Void> res = orderService.removeItemFromCart(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]));
                    return res.isOk();
                }

                case "suspend": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("suspend should have 3 args: username, startDate, endDate");
                    String username = args[0];
                    Optional<LocalDateTime> startDate = Optional.of(LocalDateTime.parse(args[1]));
                    Optional<LocalDateTime> endDate = Optional.of(LocalDateTime.parse(args[2]));
                    Response<Void> res = userService.suspendUser(initST, username, startDate, endDate);
                    return res.isOk();
                }

                case "unsuspend": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("unsuspend should have 1 arg: username");
                    Response<Void> res = userService.unsuspendUser(initST, args[0]);
                    return res.isOk();
                }

                default:
                    System.err.println("Unknown processing command: " + command);
                    return false;
            }

        } catch (Exception e) {
            System.err.println("Error processing command: " + line);
            return false;
        }
    }

    private void ensureSystemManagerExists() {
        System.out.println("Checking for system manager existence...");
        if (!userService.hasSystemManager()) {
            System.out.println("No system manager found. Creating default system manager...");
            createDefaultSystemManager();
        } else {
            System.out.println("System manager already exists.");
        }
    }

    private void createDefaultSystemManager() {
        try {
            // Get credentials from config
            String defaultUsername = initConfig.getDefaultSystemManager() != null ? 
                                    initConfig.getDefaultSystemManager().getName() : "admin";
            String defaultPassword = initConfig.getDefaultSystemManager() != null ? 
                                    initConfig.getDefaultSystemManager().getPassword() : "Admin123!";
            LocalDate defaultDob = LocalDate.of(1990, 1, 1);
            
            // Register the user
            String guestToken = userService.enterToSystem().getData();
            Response<Void> registerResponse = userService.registerUser(guestToken, defaultUsername, defaultPassword, defaultDob);
            
            if (registerResponse.isOk()) {
                // Make the user a system manager
                Response<Void> makeManagerResponse = userService.makeSystemManager(defaultUsername);
                if (makeManagerResponse.isOk()) {
                    System.out.println("Default system manager created successfully: " + defaultUsername);
                } else {
                    System.err.println("Failed to promote user to system manager: " + 
                        (makeManagerResponse.getError() != null ? makeManagerResponse.getError() : "Unknown error"));
                }
            } else {
                System.err.println("Failed to register default system manager: " + 
                    (registerResponse.getError() != null ? registerResponse.getError() : "Unknown error"));
            }
        } catch (Exception e) {
            System.err.println("Error creating default system manager: " + e.getMessage());
        }
    }
}
