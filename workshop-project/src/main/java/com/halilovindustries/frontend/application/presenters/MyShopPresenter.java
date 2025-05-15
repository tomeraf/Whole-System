package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class MyShopPresenter extends AbstractPresenter {
    
    // this class is used to manage the shop details and operations; i own/manage the shop 
    @Autowired
    public MyShopPresenter(
        UserService userService,
        ShopService shopService,
        JWTAdapter jwtAdapter,
        OrderService orderService
    ) {
        this.userService   = userService;
        this.shopService   = shopService;
        this.jwtAdapter    = jwtAdapter;
        this.orderService  = orderService;
    }

    public void getShopInfo(int shopId, Consumer<ShopDTO> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<ShopDTO> resp = shopService.getShopInfo(token, shopId);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                ShopDTO shopInfo = resp.getData();
                if (shopInfo == null) {
                    Notification.show("No shop info found.", 2000, Position.MIDDLE);
                } else {
                    onFinish.accept(shopInfo);
                }
            }
        });
    });
}

    // Method to to close my shop; im founder
    public void closeShop(int shopId, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = shopService.closeShop(token, shopId);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Shop closed successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    });
    } 
}