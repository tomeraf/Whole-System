package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.UI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;

import io.jsonwebtoken.lang.Collections;

@Component
public class HomePresenter {
    private List<ShopDTO> randomShops = new ArrayList<>();
    private final UserService    userService;
    private final ShopService    shopService;
    private final JWTAdapter     jwtAdapter;
    private final OrderService   orderService;      // ← new

    @Autowired
    public HomePresenter(
        UserService userService,
        ShopService shopService,
        JWTAdapter jwtAdapter,
        OrderService orderService     // ← add here
    ) {
        this.userService   = userService;
        this.shopService   = shopService;
        this.jwtAdapter    = jwtAdapter;
        this.orderService  = orderService;           // ← assign
    }

    public void saveSessionToken() {
    Response<String> response = userService.enterToSystem();
    if (!response.isOk()) {
        System.out.println("Error: " + response.getError());
    } else {
        String token = response.getData();
        // Store in localStorage using JS
        UI.getCurrent().getPage().executeJs("localStorage.setItem('token', $0);", token);
        }
    }

    public void getSessionToken(Consumer<String> callback) {
    UI.getCurrent().getPage()
        .executeJs("return localStorage.getItem('token');")
        .then(String.class, token -> {
            if (token != null) {
                callback.accept(token); // Pass it back to whoever called
            } else {
                callback.accept(null);
            }
        });
    }

    // in HomePresenter
    public void registerUser(String name,
                            String password,
                            LocalDate dateOfBirth,
                            BiConsumer<String, Boolean> onFinish) {
        // 1) grab the current token
        getSessionToken(oldToken -> {
            if (oldToken == null) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(null, false);
                return;
            }

            // 2) attempt registration
            Response<Void> res = userService.registerUser(oldToken, name, password, dateOfBirth);
            if (!res.isOk()) {
                Notification.show("Registration failed: " + res.getError(), 3000, Position.MIDDLE);
                onFinish.accept(null, false);
                return;
            }
            // Persist it in localStorage
            UI.getCurrent().getPage().executeJs("localStorage.setItem('token',$0);", oldToken);
            Notification.show("Welcome, " + name + "!", 2000, Position.TOP_CENTER);
            onFinish.accept(oldToken, true);
        });
    }


    public void registerUser(String name, String password, LocalDate dateOfBirth) {
        getSessionToken(token -> {
            if (token != null) {
                Response<Void> res = userService.registerUser(token, name, password, dateOfBirth);
                if (res.isOk()) {
                    Notification.show("Registration successful!", 2000, Position.TOP_CENTER);
                } else {
                    Notification.show("Registration failed: " + res.getError(), 3000, Position.MIDDLE);
                }
            } else {
                // no token? probably error
                Notification.show("No session token found, please reload.");
            }
        });
    }

    public void loginUser(String username, String password, BiConsumer<String, Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(null, false);
                return;
            }

            Response<String> resp = userService.loginUser(token, username, password);
            if (!resp.isOk()) {
                Notification.show("Login failed: " + resp.getError(), 3000, Position.MIDDLE);
                onFinish.accept(null, false);
            } else {
                String newToken = resp.getData();

                // Save the new token in localStorage
                UI.getCurrent().getPage()
                    .executeJs("localStorage.setItem('token', $0);", newToken);

                Notification.show("Welcome back!", 2000, Position.TOP_CENTER);

                onFinish.accept(newToken, true);
            }
        });
    }
    
    
    /** Validate the JWT before trusting it. */
    public boolean validateToken(String token) {
        return jwtAdapter.validateToken(token);
    }

    /** Extract the “username” (or user-id) from a valid JWT. */
    public String extractUserId(String token) {
        return jwtAdapter.getUsername(token);
    }

    /** Subscribe to server pushes for this user. */
    public Registration subscribeToBroadcast(String userId, Consumer<String> callback) {
        return Broadcaster.register(userId, callback);
    }

    

    public void logoutUser() {
        // 1) get current token
        getSessionToken(oldToken -> {
            if (oldToken == null || !validateToken(oldToken)) {
                Notification.show("No valid session to log out from.", 2000, Position.MIDDLE);
                return;
            }
            // 2) call logoutRegistered on the backend
            Response<String> resp = userService.logoutRegistered(oldToken);
            if (!resp.isOk()) {
                Notification.show("Logout failed: " + resp.getError(), 3000, Position.MIDDLE);
                return;
            }
            String guestToken = resp.getData();

            // 4) overwrite with new guest token
            UI.getCurrent()
            .getPage()
            .executeJs("localStorage.setItem('token', $0);", guestToken);

            Notification.show("Logged out successfully.", 2000, Position.TOP_CENTER);
        });
    }

    // at the bottom of HomePresenter.java, add:
    /**
     * Create a new shop with the given name and description.
     *
     * @param name        the shop’s name
     * @param description the shop’s description
     * @param onFinish    callback(shopDto, success) invoked when done
     */
    public void createShop(String name,
                        String description,
                        BiConsumer<ShopDTO, Boolean> onFinish) {
        // 1) get the current JWT/session token
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                Notification.show("Please log in to create a shop.", 2000, Position.MIDDLE);
                onFinish.accept(null, false);
                return;
            }

            // 2) call the backend
            Response<ShopDTO> resp = shopService.createShop(token, name, description);

            if (!resp.isOk()) {
                Notification.show("Error creating shop: " + resp.getError(), 3000, Position.MIDDLE);
                onFinish.accept(null, false);
            } else {
                ShopDTO shop = resp.getData();
                Notification.show("Shop \"" + shop.getName() + "\" created!", 2000, Position.TOP_CENTER);
                onFinish.accept(shop, true);
            }
        });
    }
    /**
     * Fetch all shops for the current user.
     */
    public void fetchMyShops(BiConsumer<List<ShopDTO>, Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                onFinish.accept(Collections.emptyList(), false);
                return;
            }

            // ------------- should not be removed! -------------
            // Response<List<ShopDTO>> resp = shopService.showUserShops(token);
            // if (!resp.isOk()) {
            //     onFinish.accept(Collections.emptyList(), false);
            // } else {
            //     onFinish.accept(resp.getData(), true);
            // }
        });
    }

    public List<ItemDTO> getCartContent(String sessionToken) {
        Response<List<ItemDTO>> resp = orderService.checkCartContent(sessionToken);
        if (resp.isOk() && resp.getData() != null) {
            return resp.getData();
        }
        return Collections.emptyList();
    }
    public boolean isLoggedIn(String sessionToken) {
        return userService.isLoggedIn(sessionToken);
    }

    public void addItemToCart(int shopId, int itemId, int quantity, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                onFinish.accept(false);
                return;
            }
            // build the nested map: shopId → (itemId→quantity)
            HashMap<Integer, HashMap<Integer, Integer>> userItems = new HashMap<>();
            HashMap<Integer, Integer> itemsMap = new HashMap<>();
            itemsMap.put(itemId, quantity);
            userItems.put(shopId, itemsMap);

            Response<Void> resp = orderService.addItemsToCart(token, userItems);
            onFinish.accept(resp.isOk());
        });
    }

    /**
     * Fetch all open shops from the backend.
     */
    public void showAllShops(BiConsumer<List<ShopDTO>, Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                onFinish.accept(Collections.emptyList(), false);
                return;
            }
            Response<List<ShopDTO>> resp = shopService.showAllShops(token);
            if (!resp.isOk()) {
                onFinish.accept(Collections.emptyList(), false);
            } else {
                onFinish.accept(resp.getData(), true);
            }
        });
    }
}