package com.halilovindustries.frontend.application.presenters;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

public class MyShopsPresenter extends AbstractPresenter {
    // This class is currently empty, but you can add methods and properties as needed.
    // It extends AbstractPresenter, which likely contains common functionality for presenters.
    // You can also add any specific logic or data handling related to "My Shops" here.

    public MyShopsPresenter(
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
    public void fetchMyShops(BiConsumer<List<ShopDTO>, Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                onFinish.accept(Collections.emptyList(), false);
                return;
            }
            // Response<List<ShopDTO>> resp = shopService.showUserShops(token);
            // if (!resp.isOk()) {
            //     onFinish.accept(Collections.emptyList(), false);
            // } else {
            //     onFinish.accept(resp.getData(), true);
            // }
        });
    }
    /**
     * Create a new shop with the given name and description.
     *
     * @param name        the shop’s name
     * @param description the shop’s description
     * @param onFinish    callback(shopDto, success) invoked when done
     */
    public void createShop(String name,
                        String description,
                        BiConsumer<ShopDTO, Boolean> onFinish) {
        // 1) get the current JWT/session token
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                Notification.show("Please log in to create a shop.", 2000, Position.MIDDLE);
                onFinish.accept(null, false);
                return;
            }

            // 2) call the backend
            Response<ShopDTO> resp = shopService.createShop(token, name, description);

            if (!resp.isOk()) {
                Notification.show("Error creating shop: " + resp.getError(), 3000, Position.MIDDLE);
                onFinish.accept(null, false);
            } else {
                ShopDTO shop = resp.getData();
                Notification.show("Shop \"" + shop.getName() + "\" created!", 2000, Position.TOP_CENTER);
                onFinish.accept(shop, true);
            }
        });
    }
}
