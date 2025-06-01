package com.halilovindustries.backend.Service;

import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.backend.Domain.User.Permission;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class Initializer {

    private final UserService userService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final NotificationHandler notificationHandler;
    private String initST;

    @Autowired
    public Initializer(UserService userService, ShopService shopService, OrderService orderService, NotificationHandler notificationHandler) {
        this.userService = userService;
        this.shopService = shopService;
        this.orderService = orderService;
        this.notificationHandler = notificationHandler;
        initST = userService.enterToSystem().getData();
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing application data...");
            readAndApply();
    }

    private void readAndApply() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("init.txt")), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Removes all leading and trailing whitespace characters from a string.
                if (line.isEmpty()) continue; //for spaces

                boolean success = processCommand(line);
                if (!success) {
                    throw new RuntimeException("Initialization failed at line " + lineNumber + ": " + line);
                }

                lineNumber++;
            }

            System.out.println("All initialization commands completed successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to read or process init file: " + e.getMessage(), e);
        }
    }

    private boolean processCommand(String line) {
        line = line.trim();

        // Ignore empty lines or comments
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) return true;

        try {
            // Basic syntax check. can add more if needed
            if (!line.endsWith(");") || !line.contains("(")) {
                System.err.println("Invalid command format: " + line);
                return false;
            }

            // Extract command name
            int parenIndex = line.indexOf('(');
            String command = line.substring(0, parenIndex).trim();

            // Extract arguments inside the parentheses
            String argsPart = line.substring(parenIndex + 1, line.length() - 2).trim();
            String[] args = argsPart.split(",");

            // Trim each argument
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }

            // Dispatch to actual logic
            switch (command) {
                case "register-user": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("register-user should have 3 args");
                    Response<Void> res = userService.registerUser(initST, args[0], args[1], LocalDate.parse(args[2]));
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
                        throw new IllegalArgumentException("remove-item should have 2 args: shopId, itemId");
                    Response<Void> res = shopService.removeItemFromShop(initST, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                    return res.isOk();
                }


                case "change-item-price": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-price should have 3 args: shopId, itemId, newPrice");
                    Response<Void> res = shopService.changeItemPriceInShop(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Double.parseDouble(args[2]));
                    return res.isOk();
                }

                case "change-item-name": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-name should have 3 args: shopId, itemId, newName");
                    Response<Void> res = shopService.changeItemName(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2]);
                    return res.isOk();
                }

                case "change-item-description": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("change-item-description should have 3 args: shopId, itemId, newDescription");
                    Response<Void> res = shopService.changeItemDescriptionInShop(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2]);
                    return res.isOk();
                }

                case "rate-shop": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("rate-shop should have 2 args: shopId, rating");
                    Response<Void> res = shopService.rateShop(initST, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                    return res.isOk();
                }

                case "rate-item": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("rate-item should have 3 args: shopId, itemId, rating");
                    Response<Void> res = shopService.rateItem(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]));
                    return res.isOk();
                }

                case "close-shop": {
                    if (args.length != 1)
                        throw new IllegalArgumentException("close-shop should have 1 arg: shopId");
                    Response<Void> res = shopService.closeShop(initST, Integer.parseInt(args[0]));
                    return res.isOk();
                }

                case "send-message": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("send-message should have 3 args: shopId, title, content");
                    Response<Void> res = shopService.sendMessage(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            args[2]);
                    return res.isOk();
                }

                case "respond-to-message": {
                    if (args.length != 4)
                        throw new IllegalArgumentException("respond-to-message should have 4 args: shopId, messageId, title, content");
                    Response<Void> res = shopService.respondToMessage(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            args[2],
                            args[3]);
                    return res.isOk();
                }

                case "open-auction": {
                    if (args.length != 5)
                        throw new IllegalArgumentException("open-auction requires 5 args: shopId, itemId, startingPrice, startDate, endDate");

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
                        throw new IllegalArgumentException("submit-counter-bid requires 3 args: shopId, bidId, newPrice");

                    Response<Void> res = shopService.submitCounterBid(initST,
                            Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]),
                            Double.parseDouble(args[2]));
                    return res.isOk();
                }

                /*
                case "add-discount": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("add-discount requires 2 args: shopId, discountType");

                    DiscountDTO dto = new DiscountDTO(); // set more if needed
                    dto.setType(DiscountType.valueOf(args[1]));

                    Response<Void> res = shopService.addDiscount(initST, Integer.parseInt(args[0]), dto);
                    return res.isOk();
                }


                 */
                case "update-discount-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-discount-type requires 2 args: shopId, type");

                    Response<Void> res = shopService.updateDiscountType(initST, Integer.parseInt(args[0]), DiscountType.valueOf(args[1]));
                    return res.isOk();
                }

                case "update-purchase-type": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("update-purchase-type requires 2 args: shopId, type");

                    Response<Void> res = shopService.updatePurchaseType(initST, Integer.parseInt(args[0]), PurchaseType.valueOf(args[1]));
                    return res.isOk();
                }

                case "add-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("add-permission requires 3 args: shopId, username, permission");

                    Response<Void> res = shopService.addShopManagerPermission(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            Permission.valueOf(args[2]));
                    return res.isOk();
                }

                case "remove-permission": {
                    if (args.length != 3)
                        throw new IllegalArgumentException("remove-permission requires 3 args: shopId, username, permission");

                    Response<Void> res = shopService.removeShopManagerPermission(initST,
                            Integer.parseInt(args[0]),
                            args[1],
                            Permission.valueOf(args[2]));
                    return res.isOk();
                }

                case "remove-appointment": {
                    if (args.length != 2)
                        throw new IllegalArgumentException("remove-appointment requires 2 args: shopId, username");

                    Response<Void> res = shopService.removeAppointment(initST, Integer.parseInt(args[0]), args[1]);
                    return res.isOk();
                }




                default:
                    System.err.println("Unknown command: " + command);
                    return false;
            }

        } catch (Exception e) {
            System.err.println("Error processing command: " + line);
            e.printStackTrace();
            return false;
        }
    }


}
