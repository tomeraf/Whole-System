package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PoliciesPresenter extends AbstractPresenter {

    @Autowired
    public PoliciesPresenter(
        UserService userService,
        ShopService shopService,
        JWTAdapter jwtAdapter,
        OrderService orderService
    ) {
        this.userService = userService;
        this.shopService = shopService;
        this.jwtAdapter = jwtAdapter;
        this.orderService = orderService;
    }

    public void addDiscount(int shopID, DiscountDTO discountDetails, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.addDiscount(token, shopID, discountDetails);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void removeDiscount(int shopID, int discountID, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.removeDiscount(token, shopID, discountID);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void updateDiscountType(int shopID, DiscountType discountType, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.updateDiscountType(token, shopID, discountType);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void updatePurchaseType(int shopID, String purchaseType, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.updatePurchaseType(token, shopID, purchaseType);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void addPurchaseCondition(int shopID, ConditionDTO condition, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.addPurchaseCondition(token, shopID, condition);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void removePurchaseCondition(int shopID, int conditionID, Consumer<Response<Void>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Response.error("Invalid session")));
                return;
            }
            Response<Void> resp = shopService.removePurchaseCondition(token, shopID, conditionID);
            UI.getCurrent().access(() -> onFinish.accept(resp));
        });
    }

    public void getShopPurchaseConditions(int shopID, Consumer<List<ConditionDTO>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Collections.emptyList()));
                return;
            }
            Response<List<ConditionDTO>> resp = shopService.getShopPurchaseConditions(token, shopID);
            List<ConditionDTO> data = resp.isOk() ? resp.getData() : Collections.emptyList();
            UI.getCurrent().access(() -> onFinish.accept(data));
        });
    }

    public void getShopDiscounts(int shopID, Consumer<List<DiscountDTO>> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                UI.getCurrent().access(() -> onFinish.accept(Collections.emptyList()));
                return;
            }
            Response<List<DiscountDTO>> resp = shopService.getShopDiscounts(token, shopID);
            List<DiscountDTO> data = resp.isOk() ? resp.getData() : Collections.emptyList();
            UI.getCurrent().access(() -> onFinish.accept(data));
        });
    }

    
    public void getShopInfo(int shopID, Consumer<ShopDTO> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<ShopDTO> resp = shopService.getShopInfo(token, shopID);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                Notification.show("Shop retrieved successfully!", 2000, Position.MIDDLE);
                onFinish.accept(resp.getData());
            }
            });
        });        
    }
}
