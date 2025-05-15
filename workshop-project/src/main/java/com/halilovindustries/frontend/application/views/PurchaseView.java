package com.halilovindustries.frontend.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@Route(value = "purchase", layout = MainLayout.class)
@PageTitle("Checkout")
public class PurchaseView extends VerticalLayout {

    public PurchaseView() {
        setPadding(true);
        setSpacing(true);

        // —— Page Title ——
        add(new H2("Checkout"));

        // —— Shipment Details ——
        add(new H3("Shipment Details"));
        FormLayout shipForm = new FormLayout();
        shipForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        TextField fullName = new TextField("Full Name");
        fullName.setPlaceholder("Ben Enjeries");
        TextField address  = new TextField("Address");
        address.setPlaceholder("Ben Gurion 15");
        TextField city = new TextField("City");
        city.setPlaceholder("Nor Yehuda");
        ComboBox<String> country = new ComboBox<>("Country");
        List<String> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> new Locale("", code).getDisplayCountry())
                .sorted()
                .collect(Collectors.toList());

        country.setItems(countries);
        shipForm.add(fullName, address, city, country);
        add(shipForm);

        // —— Payment Details ——
        add(new H3("Payment Details"));
        FormLayout payForm = new FormLayout();
        payForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        TextField cardNumber = new TextField("Card Number");
        cardNumber.setPlaceholder("1234 5678 9012 3456");
        ComboBox<String> expMonth = new ComboBox<>("Exp. Month");
        expMonth.setItems("01","02","03","04","05","06","07","08","09","10","11","12");
        ComboBox<String> expYear = new ComboBox<>("Exp. Year");
        // e.g. 2025–2035
        java.util.List<String> years = new java.util.ArrayList<>();
        for (int y = java.time.Year.now().getValue(); y <= java.time.Year.now().getValue() + 40; y++) {
            years.add(String.valueOf(y));
        }
        expYear.setItems(years);
        PasswordField cvv = new PasswordField("CVV");
        cvv.setPlaceholder("123");
        payForm.add(cardNumber, expMonth, expYear, cvv);
        add(payForm);

        // —— Purchase Button ——
        Button purchase = new Button("Place Order", VaadinIcon.CART.create());
        purchase.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        purchase.getStyle().set("margin-top", "1rem");
        // purchase.addClickListener(e -> presenter.placeOrder(...));
        add(purchase);

        setHorizontalComponentAlignment(
                FlexComponent.Alignment.CENTER,
                purchase
        );

        // center everything
        setAlignItems(FlexComponent.Alignment.START);
    }
}