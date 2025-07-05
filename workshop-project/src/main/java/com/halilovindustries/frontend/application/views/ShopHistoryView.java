package com.halilovindustries.frontend.application.views;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.backend.Domain.DTOs.BasketDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.frontend.application.presenters.ShopHistoryPresenter;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;

@Route(value = "shop-history/", layout = MainLayout.class)
@PageTitle("Shop order history")
public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<Integer> {
    private ShopHistoryPresenter presenter;
    private final Button back = new Button("← Back", e -> UI.getCurrent().navigate(""));
    private final VerticalLayout contentLayout = new VerticalLayout(); // Separate layout for content

    @Autowired
    public ShopHistoryView(ShopHistoryPresenter presenter) {
        this.presenter = presenter;

        // Create a layout for the back button
        HorizontalLayout backLayout = new HorizontalLayout(back);
        add(backLayout);

        // Add the content layout
        add(contentLayout);

        setPadding(true);
        setSpacing(true);
        setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        presenter.getShopOrderHistory(parameter, this::displayOrders);
    }

    private void displayOrders(List<BasketDTO> orders) {
        contentLayout.removeAll(); // Clear only the content layout

        for (BasketDTO basket : orders) {
            // Create a header for the basket
            contentLayout.add(new H3("Order: " + basket.getOrderId() + " | User: " + basket.getUsername()));

            // Create a grid for the items in the basket
            Grid<ItemDTO> grid = new Grid<>(ItemDTO.class);

            // Clear any existing columns to avoid duplication
            grid.removeAllColumns();

            // Define columns for the grid
            grid.addColumn(ItemDTO::getName).setHeader("Item Name");
            grid.addColumn(ItemDTO::getCategory).setHeader("Category");
            grid.addColumn(ItemDTO::getDescription).setHeader("Description");
            grid.addColumn(ItemDTO::getPrice).setHeader("Price");
            grid.addColumn(ItemDTO::getQuantity).setHeader("Quantity");

            // Set the items for the grid
            List<ItemDTO> items = basket.getItems();
            grid.setItems(items);

            // Calculate height dynamically based on the number of rows
            int rowCount = items.size();
            int rowHeight = 50; // Approximate height of a single row in pixels
            int headerHeight = 50; // Approximate height of the header in pixels
            int totalHeight = rowCount * rowHeight + headerHeight;

            // Set the grid height dynamically
            grid.setHeight(totalHeight + "px");

            // Add the grid to the content layout
            contentLayout.add(grid);

            // Add a separator between baskets
            contentLayout.add(new Hr());
        }
    }
}

