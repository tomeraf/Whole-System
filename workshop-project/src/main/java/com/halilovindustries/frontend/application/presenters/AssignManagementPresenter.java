package com.halilovindustries.frontend.application.presenters;
import java.util.Set;
import java.util.function.Consumer;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Service.OrderService;  
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

public class AssignManagementPresenter extends AbstractPresenter {

    public AssignManagementPresenter(
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

    public void addShopOwner(String sessionToken, int shopID, String appointeeName, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> resp = shopService.addShopOwner(sessionToken, shopID, appointeeName);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Shop owner added successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    }

    public void removeShopOwner(String sessionToken, int shopID, String appointeeName, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> resp = shopService.removeAppointment(sessionToken, shopID, appointeeName);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Shop owner removed successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    }

    public void addShopManager(String sessionToken, int shopID, String appointeeName, Set<Permission> permissions, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
            if (token == null) {
                Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> resp = shopService.addShopManager(sessionToken, shopID, appointeeName, permissions);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Shop manager added successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
        });
    }
}
