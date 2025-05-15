package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ShopsPresenter extends AbstractPresenter {
    @Autowired
    public ShopsPresenter(
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

    public void getNRandomShops(int n, Consumer<List<ShopDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            // 2) Call the backend
            Response<List<ShopDTO>> response = shopService.showAllShops(token);

            if (!response.isOk()) {
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(null);
                return;
            }
            List<ShopDTO> randomShops = response.getData();
            Collections.shuffle(randomShops);
            if (randomShops.size() > n) {
                randomShops = randomShops.subList(0, n);
            }
            onFinish.accept(randomShops);
        });
        });
    }

    
    public void getShopInfo(int shopID, Consumer<ShopDTO> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<ShopDTO> resp = shopService.getShopInfo(token, shopID);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                Notification.show("Shop retrieved successfully!", 2000, Position.MIDDLE);
                onFinish.accept(resp.getData());
            }
            });
        });        
    }

    public void showAllShops(Consumer<List<ShopDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<List<ShopDTO>> resp = shopService.showAllShops(token);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                Notification.show("Shops retrieved successfully!", 2000, Position.MIDDLE);
                onFinish.accept(resp.getData());
            }
            });
        });        
    }
}