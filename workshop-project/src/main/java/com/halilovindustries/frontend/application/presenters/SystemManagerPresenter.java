package com.halilovindustries.frontend.application.presenters;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class SystemManagerPresenter extends AbstractPresenter {

    @Autowired
    public SystemManagerPresenter(
            UserService userService,
            ShopService shopService,
            JWTAdapter jwtAdapter,
            OrderService orderService
    ) {
        this.userService = userService;
        this.shopService = shopService;
        this.jwtAdapter = jwtAdapter;
        this.orderService = orderService;
    }

    public void suspendUser(String username, Optional<LocalDateTime> startDate, Optional<LocalDateTime> endDate, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(false);
                    return;
                }

                Response<Void> resp = userService.suspendUser(token, username, startDate, endDate);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(false);
                } else {
                    Notification.show("User suspended successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
                }
            });
        });
    }

    public void watchSuspensions(Consumer<String> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(null);
                    return;
                }

                Response<String> resp = userService.watchSuspensions(token);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(null);
                } else {
                    Notification.show("Suspension data retrieved successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(resp.getData());
                }
            });
        });
    }

    public void unsuspendUser(String username, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(false);
                    return;
                }

                Response<Void> resp = userService.unsuspendUser(token, username);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(false);
                } else {
                    Notification.show("User unsuspended successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
                }
            });
        });
    }

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

    public void getShopId(String shopName, Consumer<Integer> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;
            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    onFinish.accept(null);
                    return;
                }

                Response<Integer> resp = shopService.getShopId(token, shopName);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(null);
                    return;
                } else {
                    onFinish.accept(resp.getData());
                }
            });
        });

    }


    public void watchClosedShops(Consumer<List<ShopDTO>> onUpdate) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)){
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    onUpdate.accept(null);
                    return;
                }

                // Suppose you have a shopService.getClosedShops(token)
                Response<List<ShopDTO>> resp = shopService.getClosedShops(token);
                if (!resp.isOk()) {
                    Notification.show("Error loading closed shops: " + resp.getError(), 2000, Position.MIDDLE);
                    onUpdate.accept(null);
                } else {
                    onUpdate.accept(resp.getData());
                }
            });
        });
    }

    /**
     * Reopen a shop by name.
     * Calls back with true if successful.
     */

//    public void reopenShop(String shopName, Consumer<Boolean> onFinish) {
//        getSessionToken(token -> {
//            UI ui = UI.getCurrent();
//            if (ui == null) return;
//
//            ui.access(() -> {
//                if (token == null || !validateToken(token) || !isLoggedIn(token)){
//                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
//                    onFinish.accept(false);
//                    return;
//                }
//
//                // First fetch the ID
//                Response<Integer> idResp = shopService.getShopId(token, shopName);
//                if (!idResp.isOk() || idResp.getData() == null) {
//                    Notification.show("Shop not found: " + shopName, 2000, Position.MIDDLE);
//                    onFinish.accept(false);
//                    return;
//                }
//
//                // Then call the reopen endpoint
//                Response<Void> repResp = shopService.reopenShop(token, idResp.getData());
//                if (!repResp.isOk()) {
//                    Notification.show("Error reopening shop: " + repResp.getError(), 2000, Position.MIDDLE);
//                    onFinish.accept(false);
//                } else {
//                    Notification.show("Shop reopened!", 2000, Position.MIDDLE);
//                    onFinish.accept(true);
//                }
//            });
//        });
//    }

}
