package com.halilovindustries.frontend.application.presenters;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;

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

    // need to implement this (after the backend is done)
    // TODO: implement this class
    public void getInbox(BiConsumer<String, String> onFinish) {
    }
}
