package com.halilovindustries.frontend.application.presenters;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.BasketDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ShopHistoryPresenter extends AbstractPresenter{
    @Autowired
    public ShopHistoryPresenter(
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


    public void getShopOrderHistory(int shopId,Consumer<List<BasketDTO>> onFinish){
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    onFinish.accept(List.of());
                    return;
                }

                Response<List<BasketDTO>> res = shopService.getShopOrderHistory(token, shopId);
                if (!res.isOk()) {
                    Notification.show("Error: " + res.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(List.of());
                } else {
                    onFinish.accept(res.getData());
                }
            });
        });
        
    }

}
