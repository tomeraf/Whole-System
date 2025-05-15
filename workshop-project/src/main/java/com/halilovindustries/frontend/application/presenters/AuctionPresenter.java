package com.halilovindustries.frontend.application.presenters;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;
public class AuctionPresenter extends AbstractPresenter {

    public AuctionPresenter(
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

    public void addAuctioneer(String sessionToken, int shopID, int itemID, double startingPrice, LocalDateTime startDate, LocalDateTime endDate, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> resp = shopService.openAuction(token, shopID, itemID, startingPrice, startDate, endDate);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Auctioneer added successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    }
}
