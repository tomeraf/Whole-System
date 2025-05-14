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
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;

import io.jsonwebtoken.lang.Collections;

@Component
public class MyShopPresenter {
    private List<ShopDTO> randomShops = new ArrayList<>();
    private final UserService    userService;
    private final ShopService    shopService;
    private final JWTAdapter     jwtAdapter;
    private final OrderService   orderService;      // ← new

    @Autowired
    public MyShopPresenter(
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
    
    /** Validate the JWT before trusting it. */
    public boolean validateToken(String token) {
        return jwtAdapter.validateToken(token);
    }

    /** Extract the “username” (or user-id) from a valid JWT. */
    public String extractUserId(String token) {
        return jwtAdapter.getUsername(token);
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
}