package com.halilovindustries.frontend.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Route(value = "purchase", layout = MainLayout.class)
@PageTitle("Checkout")
public class PurchaseView extends VerticalLayout {

    public PurchaseView() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.START);

        add(new H2("Checkout"));

        // —— Shipment Details ——
        add(new H3("Shipment Details"));
        FormLayout shipForm = new FormLayout();
        shipForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        shipForm.getElement().setAttribute("autocomplete", "off");

        TextField fullName = new TextField("Full Name");
        fullName.getElement().setAttribute("name", "ship-fullName");
        fullName.getElement().setAttribute("autocomplete", "off");

        TextField address = new TextField("Address");
        address.getElement().setAttribute("name", "ship-address");
        address.getElement().setAttribute("autocomplete", "off");

        TextField city = new TextField("City");
        city.getElement().setAttribute("name", "ship-city");
        city.getElement().setAttribute("autocomplete", "off");

        ComboBox<String> country = new ComboBox<>("Country");
        country.getElement().setAttribute("name", "ship-country");
        country.getElement().setAttribute("autocomplete", "off");
        country.setItems(
            Arrays.stream(Locale.getISOCountries())
                  .map(c -> new Locale("",c).getDisplayCountry())
                  .sorted()
                  .collect(Collectors.toList())
        );

        shipForm.add(fullName, address, city, country);
        add(shipForm);

        // —— Payment Details ——
        add(new H3("Payment Details"));
        FormLayout payForm = new FormLayout();
        payForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );

        // Card number — allow proper CC autofill
        TextField cardNumber = new TextField("Card Number");
        cardNumber.getElement().setAttribute("name", "cc-number");
        cardNumber.getElement().setAttribute("autocomplete", "cc-number");

        // Exp month/year
        ComboBox<String> expMonth = new ComboBox<>("Exp. Month");
        expMonth.getElement().setAttribute("name", "cc-exp-month");
        expMonth.getElement().setAttribute("autocomplete", "cc-exp-month");
        expMonth.setItems("01","02","03","04","05","06","07","08","09","10","11","12");

        ComboBox<String> expYear = new ComboBox<>("Exp. Year");
        expYear.getElement().setAttribute("name", "cc-exp-year");
        expYear.getElement().setAttribute("autocomplete", "cc-exp-year");
        int currentYear = Year.now().getValue();
        List<String> years = IntStream.range(currentYear, currentYear+41)
                                      .mapToObj(String::valueOf)
                                      .collect(Collectors.toList());
        expYear.setItems(years);

        // CVV — credit‐card security code
        PasswordField cvv = new PasswordField("CVV");
        cvv.getElement().setAttribute("name", "cc-csc");
        cvv.getElement().setAttribute("autocomplete", "cc-csc");

        payForm.add(cardNumber, expMonth, expYear, cvv);
        add(payForm);

        // —— Place Order Button ——
        Button placeOrder = new Button("Place Order", VaadinIcon.CART.create());
        placeOrder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        placeOrder.getStyle().set("margin-top", "1rem");
        add(placeOrder);
        setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, placeOrder);
    }
}
