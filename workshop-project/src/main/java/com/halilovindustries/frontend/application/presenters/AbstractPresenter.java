package com.halilovindustries.frontend.application.presenters;

import java.util.HashMap;
import java.util.function.Consumer;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

public abstract class AbstractPresenter {
    protected UserService    userService;
    protected ShopService    shopService;
    protected JWTAdapter     jwtAdapter;
    protected OrderService   orderService;
    protected DatabaseHealthService databaseHealthService;

    public void getSessionToken(Consumer<String> callback) {
        UI.getCurrent().getPage()
            .executeJs("return localStorage.getItem('token') || sessionStorage.getItem('token');")
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
        return jwtAdapter.validateToken(token);// && !userService.invalidToken(token);
    }
    
    public boolean isLoggedIn(String sessionToken) {
        return userService.isLoggedIn(sessionToken);
    }

    public boolean isInSystem(String sessionToken) {
        return userService.inSystem(sessionToken);
    }

    public String getUsername(String sessionToken) {
        return userService.getUsername(sessionToken);
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


                // call your batch‐add method
                Response<Void> resp = orderService.addItemToCart(token, item.getShopId(),item.getItemID(),item.getQuantity());
                if (resp.isOk()) {
                Notification.show("Added \"" + item.getName() + "\" to cart", 2000, Position.TOP_CENTER);
                } else {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                }
            });
        });
    }
}
