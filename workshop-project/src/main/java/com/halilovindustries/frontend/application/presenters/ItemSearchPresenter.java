package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ItemSearchPresenter extends AbstractPresenter {

    public ItemSearchPresenter(
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

    // Method to search for items in a specific shop with filters (no shop rate filter)
    public void filterItemsInShop(int shopId, HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<List<ItemDTO>> resp = shopService.filterItemsInShop(token, shopId, filters);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                List<ItemDTO> items = resp.getData();
                if (items == null) {
                    Notification.show("No items found.", 2000, Position.MIDDLE);
                } else {
                    onFinish.accept(items);
                }
            }
        });
    });
}

    // Method to search for items in all shops with filters (with shop rate filter)
    public void filterItemsAllShops(HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<List<ItemDTO>> resp = shopService.filterItemsAllShops(token, filters);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                List<ItemDTO> items = resp.getData();
                if (items == null) {
                    Notification.show("No items found.", 2000, Position.MIDDLE);
                } else {
                    onFinish.accept(items);
                }
            }
        });
    });
}
}
