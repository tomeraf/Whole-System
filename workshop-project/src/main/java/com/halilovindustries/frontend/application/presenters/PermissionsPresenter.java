package com.halilovindustries.frontend.application.presenters;
import java.util.List;
import java.util.function.Consumer;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

public class PermissionsPresenter extends AbstractPresenter {

    public PermissionsPresenter(
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
    // public void checkMemberPermissions(int userId, Consumer<List<Permission>> onFinish) {
    //     getSessionToken(token -> {
    //         if (token == null || !validateToken(token) || !isLoggedIn(token)) {
    //             UI.getCurrent().access(() ->
    //                 Notification.show("Token is invalid, can't proceed the process.", 2000, Position.MIDDLE)
    //             );
    //             onFinish.accept(null);
    //             return;
    //         }
    //         // 2) Call the backend
    //         Response<List<Permission>> response = orderService.getMemberPermissions(token, userId);
    //         if (!response.isOk()) {
    //             UI.getCurrent().access(() ->
    //                 Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE)
    //             );
    //             onFinish.accept(null);
    //             return;
    //         }
    //         onFinish.accept(response.getData());
    //     });
    // }
}
