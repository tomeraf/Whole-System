package com.halilovindustries.frontend.application.views;

import com.halilovindustries.frontend.application.views.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

// ——— New imports for search bar ———
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
// ——————————————————————————————

@Route(value = "shops", layout = MainLayout.class)
public class ShopsView extends VerticalLayout {
    public ShopsView() {
        setPadding(true);

        add(new H2("Shops"));

        // add search bar above the title
        add(createSearchBar());


        // … later: pull your shop-list component here
    }

    private HorizontalLayout createSearchBar() {
        TextField searchBar = new TextField();
        searchBar.setPlaceholder("Search Shops...");
        searchBar.setWidth("400px");
        searchBar.getStyle()
                .set("height", "38px")
                .set("border-radius", "4px 0 0 4px")
                .set("border-right", "none");

        Button searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
                .set("height", "38px")
                .set("min-width", "38px")
                .set("width", "38px")
                .set("padding", "0")
                .set("border-radius", "0 4px 4px 0")
                .set("border", "1px solid #ccc")
                .set("background-color", "#F7B05B")
                .set("color", "black");

        HorizontalLayout searchContainer = new HorizontalLayout(searchBar, searchBtn);

        searchContainer.setWidthFull();
        searchContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);


        searchContainer.setSpacing(false);
        searchContainer.setPadding(false);
        searchContainer.setAlignItems(FlexComponent.Alignment.CENTER);

        return searchContainer;
    }
}