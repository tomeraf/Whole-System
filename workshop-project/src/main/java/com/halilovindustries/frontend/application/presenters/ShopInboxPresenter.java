package com.halilovindustries.frontend.application.presenters;

import java.util.function.Consumer;

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

    // public void getShopInbox(Consumer<List<MessageDTO>> onFinish) {
    //     getSessionToken(token -> {
    //         if (token == null && !validateToken(token) && !isLoggedIn(token)) {
    //             UI.getCurrent().access(() ->
    //                 Notification.show("User is not logged in - guest cannot watch shop details", 2000, Position.MIDDLE)
    //             );
    //             return;
    //         }
    //         // 2) Call the backend
    //         //Response<List<IMessage>> messages = shopService.getInbox(0);

    //         // 3) Notify the user, on the UI thread
    //         // UI.getCurrent().access(() -> {
    //         //     if (response.isOk()) {
    //         //         Notification success = Notification.show("Purchase successful");
    //         //         success.setPosition(Position.MIDDLE);
    //         //         success.setDuration(3000);
    //         //     } else {
    //         //         Notification failure = Notification.show("Failed to purchase items");
    //         //         failure.setPosition(Position.MIDDLE);
    //         //         failure.setDuration(3000);
    //         //     }
    //         // });
    //     });
    // }

    public void respondToMessage(int shopId, int messageId, String title, String content, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                UI.getCurrent().access(() ->
                    Notification.show("User is not logged in - guest cannot watch shop details", 2000, Position.MIDDLE)
                    );
                return;
            }
            Response<Void> response = shopService.respondToMessage(token, shopId, messageId, title, content);
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

    // public void respondToMessage(MessageDTO m,) {

    // }
}
