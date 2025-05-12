package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.UI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;

@Component
public class HomePresenter {
    private final UserService userService;
    private final ShopService shopService;
    private final JWTAdapter jwtAdapter;
    private List<ShopDTO> randomShops = new ArrayList<>();
    private Registration broadcastRegistration;

    @Autowired
    public HomePresenter(
        UserService userService,
        ShopService shopService,
        JWTAdapter jwtAdapter
    ) {
        this.userService = userService;
        this.shopService = shopService;
        this.jwtAdapter = jwtAdapter;
        rnd3Shops();
}

    private void rnd3Shops() {
//        List<ShopDTO> shops = shopService.showAllShops().getData();
//        Random rand = new Random();
//        if (shops.isEmpty())
//            return;
//        int rndNum = rand.nextInt(shops.size());
//        RandomShops = new ArrayList<>();
//        for (int i = 0; i < 3; i++)
//            RandomShops.add(shops.get(rndNum++ % shops.size()));

        List<ShopDTO> shops = shopService.showAllShops().getData();
        randomShops.clear();
        if (shops == null || shops.isEmpty()) return;

        Random rand = new Random();
        int start = rand.nextInt(shops.size());
        for (int i = 0; i < 3; i++) {
            randomShops.add(shops.get((start + i) % shops.size()));
        }
    }


    public List<ShopDTO> getRandomShops() {
        return randomShops;
    }


    public List<ItemDTO> get4rndShopItems(ShopDTO shop) {
//        List<ItemDTO> randomItems = new ArrayList<>();
//        Random rand = new Random();
//        if (randomShops.isEmpty())
//            return randomItems;
//        Object[] keys = shop.getItems().keySet().toArray();
//        int rndNum = rand.nextInt(keys.length);
//        for (int i = 0; i < 4; i++)
//            randomItems.add(shop.getItems().get((Integer)(keys[rndNum%keys.length])));
//        return randomItems;

        List<ItemDTO> randomItems = new ArrayList<>();
        if (shop.getItems() == null || shop.getItems().isEmpty())
            return randomItems;

        Object[] keys = shop.getItems().keySet().toArray();
        Random rand = new Random();
        int start = rand.nextInt(keys.length);
        for (int i = 0; i < 4; i++) {
            int idx = (start + i) % keys.length;
            randomItems.add(shop.getItems().get((Integer)keys[idx]));
        }
        return randomItems;
    }

    public void saveSessionToken() {
    Response<String> response = userService.enterToSystem();
    if (!response.isOk()) {
        System.out.println("Error: " + response.getError());
    } else {
        String token = response.getData();
        System.out.println("Session token: " + token);
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

    // public void loginUser(String username, String password) {
    // // grab the current guest‐session token
    // getSessionToken(token -> {
    //   if (token == null) {
    //     // no guest token? probably error
    //     Notification.show("No session token found, please reload.");
    //     return;
    //   }
    //   // call the backend
    //   Response<String> resp = userService.loginUser(token, username, password);
    //   if (!resp.isOk()) {
    //     Notification.show("Login failed: " + resp.getError(), 3000, Position.MIDDLE);
    //   } else {
    //     String newToken = resp.getData();
    //     // overwrite localStorage
    //     UI.getCurrent().getPage()
    //       .executeJs("localStorage.setItem('token', $0);", newToken);
    //     Notification.show("Welcome back!", 2000, Position.TOP_CENTER);
    //   }
    // });
 
    // }



    public void loginUser(String username, String password) {
    getSessionToken(token -> {
        if (token == null) {
            Notification.show("No session token found, please reload.");
            return;
        }
        Response<String> resp = userService.loginUser(token, username, password);
        if (!resp.isOk()) {
            Notification.show("Login failed: " + resp.getError(), 3000, Position.MIDDLE);
        } else {
            String newToken = resp.getData();
            UI ui = UI.getCurrent();
            // overwrite the localStorage
            ui.getPage().executeJs("localStorage.setItem('token', $0);", newToken);

            // now extract user ID and subscribe
            if (validateToken(newToken)) {
                String userId = extractUserId(newToken);
                subscribeToBroadcast(userId, msg -> {
                    ui.access(() ->
                        Notification.show("Server: " + msg, 3000, Position.TOP_CENTER)
                    );
                });
            }

            Notification.show("Welcome back!", 2000, Position.TOP_CENTER);
        }
    });
}


/*    getSessionToken(token -> {
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
        }); */


    public Response<String> loginUser(String sessionToken, String username, String password) {
        return userService.loginUser(sessionToken, username, password);
    }

    public Response<Void> registerUser(String sessionToken, String name, String password, LocalDate dob) {
        return userService.registerUser(sessionToken, name, password, dob);
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
    public void subscribeToBroadcast(String userId, Consumer<String> callback) {
        broadcastRegistration = Broadcaster.register(userId, callback);
    }

    /** Unsubscribe when view is detached. */
    public void unsubscribeFromBroadcast() {
        if (broadcastRegistration != null) {
            broadcastRegistration.remove();
            broadcastRegistration = null;
        }
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

        // 3) clear any subscriptions
        unsubscribeFromBroadcast();

        // 4) overwrite with new guest token
        UI.getCurrent()
          .getPage()
          .executeJs("localStorage.setItem('token', $0);", guestToken);

        Notification.show("Logged out successfully.", 2000, Position.TOP_CENTER);
    });
    }
}