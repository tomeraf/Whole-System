package com.halilovindustries.frontend.application.presenters;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;

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
    // TODO: implement this class

    public void sendMessege(int shopID, String title, String content, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                UI.getCurrent().access(() ->
                    Notification.show("User is not logged in - guest cannot watch shop details", 2000, Position.MIDDLE)
                    );
                return;
            }
            Response<Void> response = shopService.sendMessage(token, shopID, title, content);
            UI.getCurrent().access(() -> {
                if (response.isOk()) {
                    Notification success = Notification.show("Message Sent Successfully");
                    success.setPosition(Position.MIDDLE);
                    success.setDuration(3000);
                } else {
                    Notification failure = Notification.show("Failed to send message");
                    failure.setPosition(Position.MIDDLE);
                    failure.setDuration(3000);
                }
            });
        });
    }
    
}
