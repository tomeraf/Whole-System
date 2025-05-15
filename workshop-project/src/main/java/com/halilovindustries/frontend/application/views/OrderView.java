package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "order", layout = MainLayout.class)
@PageTitle("Order Preview")
public class OrderView extends VerticalLayout {

    public OrderView() {
        setPadding(true);
        setSpacing(true);

        // 1️⃣ Title
        add(new H2("Order History"));

        Grid<Void> grid = new Grid<>(Void.class, false);
        // keep references to each column so we can target their footer cell
        Grid.Column<Void> itemCol = grid.addColumn(v -> "")
                .setHeader("Item Name")
                .setAutoWidth(true);
        Grid.Column<Void> qtyCol  = grid.addColumn(v -> "")
                .setHeader("Quantity")
                .setAutoWidth(true);
        Grid.Column<Void> priceCol= grid.addColumn(v -> "")
                .setHeader("Price")
                .setAutoWidth(true);

        // 3️⃣ Footer row for the total
        FooterRow footer = grid.appendFooterRow();
        footer.getCell(itemCol).setText("Total");                      // label in 1st column
        footer.getCell(qtyCol).setText("");                           // blank middle cell
        footer.getCell(priceCol).setText("$0.00");

        add(grid);
        setSizeFull();
    }
}