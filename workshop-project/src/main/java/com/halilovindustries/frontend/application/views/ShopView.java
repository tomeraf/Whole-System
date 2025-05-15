package com.halilovindustries.frontend.application.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route(value = "shop", layout = MainLayout.class)
@PageTitle("Shop Details")
public class ShopView extends VerticalLayout {

    public ShopView() {
        setPadding(true);
        setSpacing(true);

        // 1️⃣ Title row
        H2 shopTitle = new H2("My Awesome Shop");
        HorizontalLayout titleRow = new HorizontalLayout(shopTitle);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.START);

        // 1️⃣ Create the filter button first and style it
        Button filterBtn = new Button("", VaadinIcon.FILTER.create());
        filterBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBtn.getStyle()
                .set("height", "36px")
                .set("min-width", "36px")
                .set("padding", "0")
                .set("border-radius", "4px 0 0 4px")       // round left corners
                .set("border", "1px solid #ccc")
                .set("border-right", "none")                // drop the right border
                .set("background-color", "lightblue")
                .set("color", "black");
        filterBtn.addClickListener(e -> openFilterDialog());

// 2️⃣ Re-style the search bar to shed its left-border/radius
        TextField searchBar = new TextField();
        searchBar.setPlaceholder("Search here…");
        searchBar.setWidth("400px");
        searchBar.getStyle()
                .set("height", "38px")
                .set("border-radius", "0")                  // no rounding
                .set("border-left", "none")                 // drop its left border
                .set("border-right", "none");

// 3️⃣ Keep the search button on the right
        Button searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
                .set("height", "36px")
                .set("min-width", "36px")
                .set("padding", "0")
                .set("border-radius", "0 4px 4px 0")        // round right corners
                .set("border", "1px solid #ccc")
                .set("border-left", "none")                 // drop its left border
                .set("background-color", "#F7B05B")
                .set("color", "black");

        Button msgBtn = new Button("Message", VaadinIcon.ENVELOPE.create());
        msgBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        msgBtn.getStyle()
                .set("background-color", "#6200EE")
                .set("color", "white")
                .set("border", "none")
                .set("border-radius", "8px")
                .set("padding", "0.6em 1.2em");

        // 1️⃣ Build an inner bar with filter/search/searchBtn
        HorizontalLayout searchBarGroup = new HorizontalLayout(filterBtn, searchBar, searchBtn);
        searchBarGroup.setWidthFull();                                             // fill available space
        searchBarGroup.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        searchBarGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        searchBarGroup.setSpacing(false);
        searchBarGroup.setPadding(false);

// 2️⃣ Parent row that contains the centered group + the right‐aligned msgBtn
        HorizontalLayout actionsRow = new HorizontalLayout(searchBarGroup, msgBtn);
        actionsRow.setWidthFull();
        actionsRow.setAlignItems(FlexComponent.Alignment.CENTER);
        actionsRow.expand(searchBarGroup);                                          // give all extra space to searchBarGroup

// 3️⃣ Add your titleRow and this new row
        add(titleRow, actionsRow);
    }

    private void openFilterDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");

        NumberField minPrice = new NumberField("Min Price");
        NumberField maxPrice = new NumberField("Max Price");

        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems("All", "Electronics", "Clothing", "Books"); // example

        ComboBox<Integer> rating = new ComboBox<>("Rating");
        rating.setItems(1, 2, 3, 4, 5);

        Button apply = new Button("Apply", e -> {
            // TODO: pull values from minPrice, maxPrice, category, rating
            // and pass them to your presenter
            dialog.close();
        });
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
                new Text("Filter by:"),
                minPrice,
                maxPrice,
                category,
                rating,
                apply
        );
        layout.setPadding(false);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.open();
    }
}