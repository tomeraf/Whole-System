package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;

import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class HomePresenter extends AbstractPresenter {

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
        System.out.println("Saving session token...");
        Response<String> response = userService.enterToSystem();
        if (!response.isOk()) {
            System.out.println("Error: " + response.getError());
        } else {
            String token = response.getData();
            // Store in localStorage using JS
            UI.getCurrent().getPage()
                .executeJs("sessionStorage.setItem('token', $0);", token);
            System.out.println("Token saved: " + token);
        }
    }

    // in HomePresenter
    public void registerUser(String name,
                            String password,
                            LocalDate dateOfBirth,
                            BiConsumer<String, Boolean> onFinish) {
        // 1) grab the current token
        getSessionToken(oldToken -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;
            ui.access(() -> {
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
        });
    }

    public void loginUser(String username, String password, BiConsumer<String, Boolean> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;
            ui.access(() -> {
                if (token == null || !validateToken(token)) {
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
                        .executeJs("sessionStorage.setItem('token', $0);", newToken);
                    UI.getCurrent().getPage()
                        .executeJs("localStorage.setItem('token', $0);", newToken);

                    Notification.show("Welcome back!", 2000, Position.TOP_CENTER);

                    onFinish.accept(newToken, true);
                }
                });
        });
    }
    
    /** Extract the “username” (or user-id) from a valid JWT. */
    public String extractUserId(String token) {
        return jwtAdapter.getUsername(token);
    }

    public void logoutUser() {
        // 1) get current token
        getSessionToken(oldToken -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
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
            UI.getCurrent().getPage()
                .executeJs("localStorage.removeItem('token');");

            String guestToken = resp.getData();

            // 4) overwrite with new guest token
            ui
            .getPage()
            .executeJs("sessionStorage.setItem('token', $0);", guestToken);

            Notification.show("Logged out successfully.", 2000, Position.TOP_CENTER);
        });
        });
    }

    public void exitAsGuest() {
        // 1) get current token
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No valid session to exit from.", 2000, Position.MIDDLE);
                return;
            }
            // 2) call logoutRegistered on the backend
            Response<Void> resp = userService.exitAsGuest(token);
            if (!resp.isOk()) {
                Notification.show("Logout failed: " + resp.getError(), 3000, Position.MIDDLE);
                return;
            }
            //UI.getCurrent().getPage()
            //    .executeJs("localStorage.removeItem('token');");
        });
        });
    }

    
    public void getRandomItems(int count, Consumer<List<ItemDTO>> onFinish) {
        // Step 1: pull whatever token is in localStorage
        getSessionToken(stored -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                String token = stored;

                // Step 2: if it’s null/invalid/expired → grab a fresh **guest** token synchronously
                if (token == null || !validateToken(token)) {
                    Notification.show("Please reload the page", 2000, Position.MIDDLE);
                }
                Response<List<ItemDTO>> itemsResp = shopService.filterItemsAllShops(token, new HashMap<String, String>());
                if (itemsResp.isOk() && !itemsResp.getData().isEmpty()) {
                    List<ItemDTO> available = itemsResp.getData().stream()
                        .filter(i -> i.getQuantity() > 0)
                        .collect(Collectors.toList());

                    // 2) Shuffle & trim
                    Collections.shuffle(available);
                    int take = Math.min(count, available.size());
                    onFinish.accept(available.subList(0, take));
                    return;
                }
            });
        });
    }

    public void showItemsByFilter(HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token)) {
                    Notification.show("No token was found.", 2000, Position.MIDDLE);
                    return;
                }

                Response<List<ItemDTO>> itemsResp = shopService.filterItemsAllShops(token, filters);
                if (itemsResp.isOk()) {
                    List<ItemDTO> available = itemsResp.getData().stream()
                        .filter(i -> i.getQuantity() > 0)
                        .collect(Collectors.toList());
                        // 1) Sort descending by rating, then descending by # of raters:
                        available.sort(Comparator
                            .comparingDouble(ItemDTO::getRating).reversed()
                            //.thenComparingInt(ItemDTO::getRatingsCount).reversed()
                        );

                        // 2) Return that sorted list
                        onFinish.accept(available);
                } else {
                    Notification.show("Error: " + itemsResp.getError(), 2000, Position.MIDDLE);
                }
            });
        });

    }
}