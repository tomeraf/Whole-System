package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Order History")
public class OrdersView extends VerticalLayout {

    public OrdersView() {
        setPadding(true);
        setSpacing(true);

        // 1️⃣ Title
        add(new H2("Order History"));

        // 2️⃣ Empty Grid with just column headers
        Grid<Void> grid = new Grid<>(Void.class, false);
        grid.addColumn(v -> "").setHeader("Order number").setAutoWidth(true);
        grid.addColumn(v -> "").setHeader("Total Price").setAutoWidth(true);
        grid.addComponentColumn(v -> {
            Button arrow = new Button(VaadinIcon.ARROW_RIGHT.create());
            arrow.addThemeVariants(ButtonVariant.LUMO_ICON);
            return arrow;
        }).setHeader("").setAutoWidth(true);

        // No items added → you see only the headers
        add(grid);
        setSizeFull();
    }
}