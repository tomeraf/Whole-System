package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.frontend.application.presenters.CartPresenter;
import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Route(value = "cart", layout = MainLayout.class)
@PageTitle("My Cart")
public class CartView extends Composite<VerticalLayout>
                       implements BeforeEnterObserver {
    private final CartPresenter presenter;
    private final Grid<ItemDTO> grid = new Grid<>(ItemDTO.class);
//    private final Button back = new Button("← Back",
//                                         e -> UI.getCurrent().navigate(""));

    @Autowired
    public CartView(CartPresenter presenter) {
        this.presenter = presenter;

        Button checkout = new Button("Checkout", VaadinIcon.CREDIT_CARD.create());
        checkout.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // if checkout has been clicked
        checkout.addClickListener(e -> {
            presenter.checkCartContent(items -> {
                if (items.isEmpty()) {
                    Notification.show("Your cart is empty", 2000, Position.MIDDLE);
                } else {
                    UI.getCurrent().navigate("purchase");
                }
            });
        });


        
        HorizontalLayout toolbar = new HorizontalLayout(checkout);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        // 1) configure columns
        grid.removeAllColumns();
        grid.addColumn(ItemDTO::getName)
            .setHeader("Name")
            .setAutoWidth(true);
        grid.addColumn(i -> i.getCategory().name())
            .setHeader("Category")
            .setAutoWidth(true);
        grid.addColumn(ItemDTO::getQuantity)
            .setHeader("Qty")
            .setAutoWidth(true);
        grid.addColumn(ItemDTO::getPrice)
            .setHeader("Price")
            .setAutoWidth(true);
        grid.addColumn(ItemDTO::getRating)
            .setHeader("Rating")
            .setAutoWidth(true);

        // 2) when you click a row, open a detail dialog
        grid.addItemClickListener(event -> {
        ItemDTO item = event.getItem();
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");

        // build the “card” content
        VerticalLayout detail = new VerticalLayout();
        detail.setAlignItems(FlexComponent.Alignment.CENTER);
        detail.getStyle().set("padding", "1rem");
        detail.getStyle().set("border", "1px solid #ddd");
        detail.getStyle().set("border-radius", "8px");

        detail.add(
            new H3(item.getName()),
            new Span("$" + item.getPrice()),
            new Paragraph("Category: " + item.getCategory().name()),
            new Paragraph(item.getDescription()),
            new Paragraph("⭐ " + item.getRating()),
            new Paragraph("In cart: " + item.getQuantity())
        );

        // The Remove button
        Button remove = new Button("Remove from Cart", ev -> {
            // prepare the map <shopId → [itemId]>
            HashMap<Integer, List<Integer>> toRemove = new HashMap<>();
            toRemove.put(
            item.getShopId(),
            Collections.singletonList(item.getItemID())
            );

            presenter.removeItemFromCart(item.getShopId(),item.getItemID(), success -> {
            UI.getCurrent().access(() -> {
                dlg.close();
                if (success) {
                Notification.show(
                    "Item removed from cart", 2000, Position.MIDDLE
                );
                // refresh the grid
                presenter.checkCartContent(updated -> 
                    UI.getCurrent().access(() -> grid.setItems(updated))
                );
                } else {
                Notification.show(
                    "Error removing item", 2000, Position.MIDDLE
                );
                }
            });
            });
        });
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);

        detail.add(remove);
        dlg.add(detail);
        dlg.open();
        });

        getContent().add(toolbar, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        presenter.getSessionToken(token -> {
        UI.getCurrent().access(() -> {
            if (token == null || !presenter.validateToken(token)) {
            Notification.show(
                "Please log in first", 2000, Position.MIDDLE
            );
            event.rerouteTo(HomePageView.class);
            return;
            }
            presenter.checkCartContent(items -> {
            UI.getCurrent().access(() -> {
                grid.setItems(items);
            });
            });
        });
        });
    }
}
