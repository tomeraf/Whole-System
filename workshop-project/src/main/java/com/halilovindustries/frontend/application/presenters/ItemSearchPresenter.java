package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.shared.Registration;

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
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
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
    }

    // Method to search for items in all shops with filters (with shop rate filter)
    public void filterItemsAllShops(HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
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
    }
}
