package com.halilovindustries.frontend.application.presenters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import io.jsonwebtoken.lang.Collections;

public class CartPresenter extends AbstractPresenter {
    private List<ShopDTO> randomShops = new ArrayList<>();
    @Autowired
    public CartPresenter(
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

    /** Extract the “username” (or user-id) from a valid JWT. */
    public String extractUserId(String token) {
        return jwtAdapter.getUsername(token);
    }

    public void removeFromCart(String sessionToken, ItemDTO item) {
        // Convert ItemDTO to HashMap<Integer, List<Integer>> as required by removeItemsFromCart
        java.util.HashMap<Integer, java.util.List<Integer>> itemsMap = new java.util.HashMap<>();
        // Assuming ItemDTO has getId() and getQuantity() methods
        java.util.List<Integer> quantities = new java.util.ArrayList<>();
        quantities.add(item.getQuantity());
        itemsMap.put(item.getItemID(), quantities);

        Response<Void> resp = orderService.removeItemsFromCart(sessionToken, itemsMap);
        if (resp.isOk()) {
            Notification notification = Notification.show("Item removed from cart");
            notification.setPosition(Position.MIDDLE);
            notification.setDuration(3000);
        } else {
            Notification notification = Notification.show("Failed to remove item from cart");
            notification.setPosition(Position.MIDDLE);
            notification.setDuration(3000);
        }
    }
    public void buyCartContent(String sessionToken, PaymentDetailsDTO p, ShipmentDetailsDTO s) {
        Response<Order> resp = orderService.buyCartContent(sessionToken, p, s);
        if (resp.isOk()) {
            Notification notification = Notification.show("Purchase successful");
            notification.setPosition(Position.MIDDLE);
            notification.setDuration(3000);
        } else {
            Notification notification = Notification.show("Failed to purchase items");
            notification.setPosition(Position.MIDDLE);
            notification.setDuration(3000);
        }
    }

    public List<ItemDTO> getCartContent(String sessionToken) {
        Response<List<ItemDTO>> resp = orderService.checkCartContent(sessionToken);
        if (resp.isOk() && resp.getData() != null) {
            return resp.getData();
        }
        return Collections.emptyList();
    }
}
