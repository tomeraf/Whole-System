package com.halilovindustries.frontend.application.presenters;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ShopInboxPresenter extends AbstractPresenter {

    public ShopInboxPresenter(
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

    public void getInbox(int shopId, Consumer<List<Message>> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(null);
                    return;
                }
                Response<List<Message>> response = shopService.getInbox(token, shopId);
                if (!response.isOk()) {
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(null);
                } else {
                    Notification.show("Inbox retrieved successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(response.getData());
                }
            });
        });
    }

    public void respondToMessage(int shopId, int messageId, String title, String content, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(false);
                    return;
                }
                Response<Void> responseResult = shopService.respondToMessage(token, shopId, messageId, title, content);
                if (!responseResult.isOk()) {
                    Notification.show("Error: " + responseResult.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(false);
                } else {
                    Notification.show("Response sent successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
                }
            });
        });
    }
}
