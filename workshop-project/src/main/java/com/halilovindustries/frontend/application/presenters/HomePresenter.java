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
        Response<String> response = userService.enterToSystem();
        if (!response.isOk()) {
            System.out.println("Error: " + response.getError());
        } else {
            String token = response.getData();
            // Store in localStorage using JS
            UI.getCurrent().getPage().executeJs("localStorage.setItem('token', $0);", token);
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

    /** Subscribe to server pushes for this user. */
    public Registration subscribeToBroadcast(String userId, Consumer<String> callback) {
        return Broadcaster.register(userId, callback);
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
            String guestToken = resp.getData();

            // 4) overwrite with new guest token
            ui
            .getPage()
            .executeJs("localStorage.setItem('token', $0);", guestToken);

            Notification.show("Logged out successfully.", 2000, Position.TOP_CENTER);
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
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Response<String> guestResp = userService.enterToSystem();
                if (guestResp.isOk()) {
                    token = guestResp.getData();
                    // update localStorage in browser
                    ui.getPage().executeJs("localStorage.setItem('token', $0);", token);
                } else {
                    onFinish.accept(new ArrayList<>());
                    return;
                }
            }

            // (Optional) debug
            System.out.println("[getRandomItems] using token = " + token);

            // Step 3: now safely call your shopService
            List<ItemDTO> picked = new ArrayList<>();
            Response<List<ShopDTO>> shopsResp = shopService.showAllShops(token);
            if (shopsResp.isOk()) {
                List<ShopDTO> shops = shopsResp.getData();
                if (!shops.isEmpty()) {
                    Random rnd = new Random();
                    for (int i = 0; i < count; i++) {
                        ShopDTO shop = shops.get(rnd.nextInt(shops.size()));
                        Response<List<ItemDTO>> itemsResp = shopService.showShopItems(token, shop.getId());
                        if (itemsResp.isOk() && !itemsResp.getData().isEmpty()) {
                            List<ItemDTO> items = itemsResp.getData();
                            if (items.size() > 0)
                                picked.add(items.get(rnd.nextInt(items.size())));
                        }
                    }
                }
            }

            onFinish.accept(picked);
        });
    });
}


    /**
     * Add a single item by wrapping it in the
     * addItemsToCart(batch…) call.
     */
    public void saveInCart(ItemDTO item) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token)) {
                Notification.show("No token was found.", 2000, Position.MIDDLE);
                return;
                }

                // build the nested map: { shopId → { itemId → qty } }
                HashMap<Integer, HashMap<Integer, Integer>> userItems = new HashMap<>();
                HashMap<Integer, Integer> itemsForShop = new HashMap<>();
                itemsForShop.put(item.getItemID(), 1);
                userItems.put(item.getShopId(), itemsForShop);

                // call your batch‐add method
                Response<Void> resp = orderService.addItemsToCart(token, userItems);
                if (resp.isOk()) {
                Notification.show("Added \"" + item.getName() + "\" to cart", 2000, Position.TOP_CENTER);
                } else {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                }
            });
        });
    }
}