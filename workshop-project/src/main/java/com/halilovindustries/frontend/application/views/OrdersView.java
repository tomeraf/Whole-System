package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.frontend.application.presenters.OrdersPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Order History")
public class OrdersView extends VerticalLayout {

    private final OrdersPresenter presenter;
    private final Grid<Order> grid;

    @Autowired
    public OrdersView(OrdersPresenter presenter) {
        this.presenter = presenter;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        // Title
        add(new H2("Order History"));

        // Grid setup: use getId() and getTotalPrice()
        grid = new Grid<>(Order.class, false);
        grid.addColumn(Order::getId)
            .setHeader("Order #").setAutoWidth(true);
        grid.addColumn(Order::getTotalPrice)
            .setHeader("Total Price").setAutoWidth(true);
        grid.addComponentColumn(this::createDetailsButton)
            .setHeader("Details").setAutoWidth(true);

        add(grid);
        expand(grid);

        // Fetch and display orders
        fetchOrders();
    }

    private void fetchOrders() {
        presenter.viewPersonalOrderHistory(orders -> {
            UI.getCurrent().access(() -> {
                if (orders != null) {
                    grid.setItems(orders);
                }
            });
        });
    }

    private Button createDetailsButton(Order order) {
        Button btn = new Button(VaadinIcon.ARROW_RIGHT.create());
        btn.addThemeVariants(ButtonVariant.LUMO_ICON);
        btn.addClickListener(e -> showDetailsDialog(order));
        return btn;
    }

    private void showDetailsDialog(Order order) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");
        VerticalLayout layout = new VerticalLayout();

        layout.add(new H3("Order #" + order.getOrderID()));
        layout.add(new Paragraph("Placed by User: " + order.getUserId()));
        layout.add(new Paragraph("Total: $" + order.getTotalPrice()));

        for (ItemDTO item : order.getItems()) {
            String line = item.getQuantity() + " x " + item.getName() + " for $" + (item.getQuantity() * item.getPrice());
            layout.add(new Paragraph(line));

            // Rating dropdown for item
            Select<Integer> ratingSelect = new Select<>();
            ratingSelect.setItems(1, 2, 3, 4, 5);
            ratingSelect.setPlaceholder("Rate this item");

            Button submitRating = new Button("Submit");
            submitRating.addClickListener(event -> {
                Integer rating = ratingSelect.getValue();
                if (rating != null) {
                    presenter.rateItem(item.getShopId(), item.getItemID(), rating, success -> {
                        if (success) {
                            ratingSelect.setEnabled(false);
                            submitRating.setEnabled(false);
                        }
                    });
                }
            });
            submitRating.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            HorizontalLayout ratingRow = new HorizontalLayout(ratingSelect, submitRating);
            layout.add(ratingRow);
            
            // Rating for shop
            Select<Integer> shopRating = new Select<>();
            shopRating.setItems(1, 2, 3, 4, 5);
            shopRating.setPlaceholder("Rate the shop");
    
            Button rateShopBtn = new Button("Submit");
            rateShopBtn.addClickListener(ev -> {
                Integer rating = shopRating.getValue();
                if (rating != null && !order.getItems().isEmpty()) {
                    int shopId = item.getShopId(); // Assumes all items from same shop
                    presenter.rateShop(shopId, rating, success -> {
                        if (success) {
                            shopRating.setEnabled(false);
                            rateShopBtn.setEnabled(false);
                        }
                    });
                }
            });
            rateShopBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            layout.add(new HorizontalLayout(shopRating, rateShopBtn));
        }

        Button close = new Button("Close", ev -> dialog.close());
        layout.add(close);

        dialog.add(layout);
        dialog.open();
    }
}
