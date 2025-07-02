package com.halilovindustries.frontend.application.presenters;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class PurchasePresenter extends AbstractPresenter {

    public PurchasePresenter(
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

    // public ShipmentDetailsDTO fillShipmentDetails(String ID, String name, String email, String phone,
    //         String contry, String city, String address, String zipcode) {
    //     return new ShipmentDetailsDTO(ID, name, email, phone, contry, city, address, zipcode);
    // }

    // public PaymentDetailsDTO fillPaymentDetails(String cardNumber, String cardHolderName, String holderID, String cvv, String month, String year) {
    //     return new PaymentDetailsDTO(cardNumber, cardHolderName, holderID, expirationDate, cvv);
    // }

    public void purchase(ShipmentDetailsDTO shipmentDetails, PaymentDetailsDTO paymentDetails, Consumer<Order> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            // 1) Validate
            // if (!shipmentDetails.fullShipmentDetails() || !paymentDetails.fullDetails()) {
            //     // Show a warning if the form isn’t fully filled
            //         Notification.show("Please fill all the details", 2000, Position.MIDDLE);
            //         onFinish.accept(null);
            //     return;
            // }

            // 2) Call the backend
            Response<Order> response = orderService.buyCartContent(token, paymentDetails, shipmentDetails);

            // 3) Notify the user, on the UI thread
                if (response.isOk()) {
                    Notification success = Notification.show("Purchase successful");
                    success.setPosition(Position.MIDDLE);
                    success.setDuration(3000);
                    onFinish.accept(response.getData());
                } else {
                    Notification failure = Notification.show(response.getError());
                    failure.setPosition(Position.MIDDLE);
                    failure.setDuration(3000);
                    onFinish.accept(null);
                }
        });
        });
    }
}
