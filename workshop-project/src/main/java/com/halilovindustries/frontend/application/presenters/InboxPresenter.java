package com.halilovindustries.frontend.application.presenters;
import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class InboxPresenter extends AbstractPresenter {

    // each registered user have his own inbox - this class
    public InboxPresenter(
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

    public void sendMessege(int shopID, String title, String content, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = shopService.sendMessage(token, shopID, title, content);
            UI.getCurrent().access(() -> {
                if (response.isOk()) {
                    Notification success = Notification.show("Message Sent Successfully");
                    success.setPosition(Position.MIDDLE);
                    success.setDuration(3000);
                    onFinish.accept(true);
                } else {
                    Notification failure = Notification.show("Failed to send message");
                    failure.setPosition(Position.MIDDLE);
                    failure.setDuration(3000);
                    onFinish.accept(false);
                }
            });
        });
        });
    }
    
    public void getInbox(Consumer<List<Message>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }
            Response<List<Message>> response = userService.getInbox(token);
            UI.getCurrent().access(() -> {
                if (response.isOk()) {
                    Notification success = Notification.show("Inbox retrieved successfully!");
                    success.setPosition(Position.MIDDLE);
                    success.setDuration(3000);
                    onFinish.accept(response.getData());
                } else {
                    Notification failure = Notification.show("Failed to retrieve inbox");
                    failure.setPosition(Position.MIDDLE);
                    failure.setDuration(3000);
                    onFinish.accept(null);
                }
            });
        });
        });
    }
}
