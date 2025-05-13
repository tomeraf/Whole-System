package com.halilovindustries.frontend.application.views.homepage;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.frontend.application.views.MainLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

// 1️⃣ Register under “shops” and tie it to MainLayout
@Route(value = "Categories", layout = MainLayout.class)
@CssImport("./themes/my-app/categories-view.css")
public class CategoriesView extends VerticalLayout {
    public CategoriesView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 1) Page title
        add(new H2("Categories"));

        // 2) The grid container
        Div grid = new Div();
        grid.addClassName("categories-grid");

        // 3) One little card per enum
        for (Category cat : Category.values()) {
            Div card = new Div();
            card.addClassName("category-card");

            // placeholder – swap with your real image URLs later
            Image img = new Image(
                    "https://via.placeholder.com/150?text=" + cat.name(),
                    cat.name()
            );
            img.setWidthFull();   // fill card width
            img.setHeight("150px");

            // nicer label (capitalize first, rest lowercase)
            String label = cat.name().charAt(0)
                    + cat.name().substring(1).toLowerCase();
            Paragraph p = new Paragraph(label);

            card.add(img, p);
            grid.add(card);
        }

        add(grid);
    }
}
