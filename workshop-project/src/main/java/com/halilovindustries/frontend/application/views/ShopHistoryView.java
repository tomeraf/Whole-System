package com.halilovindustries.frontend.application.views;

import java.util.List;

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
import com.vaadin.flow.component.grid.Grid;

@Route(value = "shop-history/", layout = MainLayout.class)
@PageTitle("Shop order history")
public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<Integer>  {
    private ShopHistoryPresenter presenter;

    public ShopHistoryView(ShopHistoryPresenter presenter) {
        this.presenter = presenter;
        setPadding(true);
        setSpacing(true);
        setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        // This method can be used to load the shop history based on the shop ID
        presenter.getShopOrderHistory(parameter, this::displayOrders);
    }

    private void displayOrders(List<BasketDTO> orders) {
        removeAll(); // Clear previous content

        for (BasketDTO basket : orders) {
            // Create a header for the basket
            add(new H3("Order: " + basket.getOrderId()+ " | User: " + basket.getUsername()));

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
            grid.setItems(basket.getItems());

            // Add the grid to the view
            add(grid);

            // Add a separator between baskets
            add(new Hr());
        }
    }

}

