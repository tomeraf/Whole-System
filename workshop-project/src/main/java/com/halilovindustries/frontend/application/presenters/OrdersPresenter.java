package com.halilovindustries.frontend.application.presenters;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class OrdersPresenter extends AbstractPresenter {
    public OrdersPresenter(
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

    public void viewPersonalOrderHistory(Consumer<List<Order>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<List<Order>> resp = orderService.viewPersonalOrderHistory(token);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                Notification.show("Orders retrieved successfully!", 2000, Position.MIDDLE);
                onFinish.accept(resp.getData());
            }
            });
        });        
    }
    
    public void rateShop(int shopID, int rating, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = shopService.rateShop(token, shopID, rating);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Shop rated successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
            });
        });        
    } 

    public void rateItem(int shopID, int itemID, int rating, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = shopService.rateItem(token, shopID, itemID,rating);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Item rated successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
            });
        });        
    }
}
