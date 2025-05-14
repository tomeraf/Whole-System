package com.halilovindustries.frontend.application.presenters;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;

public abstract class AbstractPresenter {
    protected UserService    userService;
    protected ShopService    shopService;
    protected JWTAdapter     jwtAdapter;
    protected OrderService   orderService;      // ← new

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
    
    public boolean isLoggedIn(String sessionToken) {
        return userService.isLoggedIn(sessionToken);
    }
}
