package com.halilovindustries.frontend.application.presenters;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

public class BidPresenter extends AbstractPresenter {

    // Bid class for the shop side logic 
    public BidPresenter(
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
    
    public void answerBid(int auctionID, double bidAmount, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = shopService.answerBid(token, auctionID, auctionID, false);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Bid answered successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    });
}

    public void submitCounterBid(int auctionID, double bidAmount, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = shopService.submitCounterBid(token, auctionID, auctionID, bidAmount);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Counter bid submitted successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
           }
        });
    });
}
}
