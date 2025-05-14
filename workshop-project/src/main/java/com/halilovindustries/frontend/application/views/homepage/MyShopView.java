package com.halilovindustries.frontend.application.views.homepage;

import java.util.List;
import java.util.function.BiConsumer;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.frontend.application.presenters.AbstractPresenter;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import io.jsonwebtoken.lang.Collections;

public class MyShopView extends AbstractPresenter {
    /**
     * Fetch all shops for the current user.
     */
    public void fetchMyShops(BiConsumer<List<ShopDTO>, Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null || !validateToken(token)) {
                onFinish.accept(Collections.emptyList(), false);
                return;
            }

            // ------------- should not be removed! -------------
            // Response<List<ShopDTO>> resp = shopService.showUserShops(token);
            // if (!resp.isOk()) {
            //     onFinish.accept(Collections.emptyList(), false);
            // } else {
            //     onFinish.accept(resp.getData(), true);
            // }
        });
    }

    // at the bottom of HomePresenter.java, add:
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
