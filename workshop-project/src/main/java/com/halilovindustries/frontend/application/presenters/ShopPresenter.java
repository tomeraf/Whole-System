package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ShopPresenter extends AbstractPresenter {
    @Autowired
    public ShopPresenter(
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
    public void getItems(int shopId, String itemName, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                UI.getCurrent().access(() ->
                    Notification.show("Token is invalid, can't proceed the process.", 2000, Position.MIDDLE)
                );
                onFinish.accept(null);
                return;
            }
            // 2) Call the backend
            Response<List<ItemDTO>> response = shopService.showShopItems(token, shopId);

            if (!response.isOk()) {
                UI.getCurrent().access(() ->
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE)
                );
                onFinish.accept(null);
                return;
            }
            onFinish.accept(response.getData());
        });
    }

    public void getItemByFilter(int shopId, String itemName, HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                UI.getCurrent().access(() ->
                    Notification.show("Token is invalid, can't proceed the process.", 2000, Position.MIDDLE)
                );
                onFinish.accept(null);
                return;
            }
            Response<List<ItemDTO>> response = shopService.filterItemsInShop(token, 0, filters);
            if (!response.isOk()) {
                UI.getCurrent().access(() ->
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE)
                );
                onFinish.accept(null);
                return;
            }
            onFinish.accept(response.getData());
        });
    }
}