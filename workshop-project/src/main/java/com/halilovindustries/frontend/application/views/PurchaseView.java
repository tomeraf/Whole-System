package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.frontend.application.presenters.PurchasePresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "purchase", layout = MainLayout.class)
@PageTitle("Checkout")
public class PurchaseView extends VerticalLayout {

    private final PurchasePresenter presenter;

    @Autowired
    public PurchaseView(PurchasePresenter presenter) {
        this.presenter = presenter;            // ① store the injected presenter

        setPadding(true);
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.START);

        // —— Header ——
        H2 title = new H2("Checkout");
        Button back = new Button("← Back", e -> UI.getCurrent().navigate("cart"));
        HorizontalLayout header = new HorizontalLayout(title, back);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        add(header);

        // —— Shipment Details ——
        add(new H3("Shipment Details"));
        FormLayout shipForm = new FormLayout();
        shipForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        shipForm.getElement().setAttribute("autocomplete", "off");

        TextField fullName = new TextField("Full Name");
        TextField email    = new TextField("Email");
        TextField phone    = new TextField("Phone");
        TextField address  = new TextField("Address");
        TextField city     = new TextField("City");
        TextField zipCode  = new TextField("Zip Code");
        ComboBox<String> country = new ComboBox<>("Country");
        country.setItems(
            Arrays.stream(Locale.getISOCountries())
                  .map(c -> new Locale("",c).getDisplayCountry())
                  .sorted()
                  .collect(Collectors.toList())
        );

        shipForm.add(fullName, email, phone, address, city, country, zipCode);
        add(shipForm);

        // —— Payment Details ——
        add(new H3("Payment Details"));
        FormLayout payForm = new FormLayout();
        payForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        payForm.getElement().setAttribute("autocomplete", "off");

        TextField cardHolderName = new TextField("Card Holder Name");
        TextField holderId       = new TextField("Card Holder ID");
        TextField cardNumber     = new TextField("Card Number");
        ComboBox<String> expMonth = new ComboBox<>("Exp. Month");
        expMonth.setItems("01","02","03","04","05","06","07","08","09","10","11","12");
        ComboBox<String> expYear  = new ComboBox<>("Exp. Year");
        int currentYear = Year.now().getValue();
        List<String> years = IntStream
            .range(currentYear, currentYear + 41)
            .mapToObj(String::valueOf)
            .collect(Collectors.toList());
        expYear.setItems(years);
        expYear.getElement().setAttribute("autocomplete", "new-password");

        PasswordField cvv        = new PasswordField("CVV");
        cvv.getElement().setAttribute("autocomplete", "new-password");


        payForm.add(cardHolderName, holderId, cardNumber, expMonth, expYear, cvv);
        add(payForm);

        // —— Place Order ——
        Button placeOrder = new Button("Place Order", VaadinIcon.CART.create());
        placeOrder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        placeOrder.getStyle().set("margin-top", "1rem");
        placeOrder.addClickListener(e -> {
            // asynchronously grab the token, extract userId, then build DTOs
            presenter.getSessionToken(token -> {
                UI ui = UI.getCurrent();
                if (ui == null) return;
                ui.access(() -> {
                    if (token == null || !presenter.validateToken(token)) {
                        Notification.show("Session expired, please log in again", 2000, Notification.Position.MIDDLE);
                        return;
                    }


                    // build our two DTOs
                    ShipmentDetailsDTO shipDto = new ShipmentDetailsDTO(
                        holderId.getValue(),
                        fullName.getValue(),
                        email.getValue(),
                        phone.getValue(),
                        country.getValue(),
                        city.getValue(),
                        address.getValue(),
                        zipCode.getValue()
                    );

                    PaymentDetailsDTO payDto = new PaymentDetailsDTO(
                        cardNumber.getValue(),
                        cardHolderName.getValue(),
                        holderId.getValue(),
                        cvv.getValue(),
                        expMonth.getValue(), expYear.getValue()
                    );

                    // hand off to the presenter
                    presenter.purchase(shipDto, payDto, order -> {
                        ui.access(() -> {
                            if (order != null) {
                                Notification.show("Order placed! 🎉", 2000, Position.MIDDLE);
                                UI.getCurrent().navigate("");
                            } else {
                                Notification.show("Failed to place order", 2000, Position.MIDDLE);
                            }
                        });
                    });
                });
            });
        });
        add(placeOrder);

        setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, placeOrder);
    }
}
