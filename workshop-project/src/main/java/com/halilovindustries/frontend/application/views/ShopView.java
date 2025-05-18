package com.halilovindustries.frontend.application.views;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import com.halilovindustries.frontend.application.presenters.ShopPresenter;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;

@Route(value = "shop/", layout = MainLayout.class)
@PageTitle("Shop Details")
public class ShopView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final ShopPresenter presenter;
    private int shopId;

    @Autowired
    public ShopView(ShopPresenter presenter) {
        this.presenter = presenter;
        setPadding(true);
        setSpacing(true);

        // Basic header rows (will be replaced on data load)
        add(new H2("Loading shop..."));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer shopId) {
        this.shopId = shopId;
        if (shopId != null) {
            loadShopData(shopId);
        } else {
            removeAll();
            add(new Span("Invalid shop ID."));
        }
    }

    private void loadShopData(int shopId) {
        presenter.getShopInfo(shopId, shop -> {
            if (shop == null) return;
            UI.getCurrent().access(() -> {
                removeAll(); // clear placeholder

                // Shop header
                H2 title = new H2(shop.getName());
                Span desc = new Span("Description: " + shop.getDescription());
                Span rating = new Span("⭐ " + shop.getRating() + " (" + shop.getRatingCount() + " raters)");
                add(title, desc, rating);

                // Search and filter bar (optional reuse)
                HorizontalLayout actions = buildActionsBar();
                add(actions);

                // Items grid
                presenter.showShopItems(shopId, items -> {
                    FlexLayout itemsLayout = new FlexLayout();
                    itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                    itemsLayout.getStyle().set("gap", "1rem");
                    items.forEach(item -> itemsLayout.add(createItemCard(item)));
                    add(itemsLayout);
                });
            });
        });
    }

    private HorizontalLayout buildActionsBar() {
        // Filter button
        Button filterBtn = new Button(VaadinIcon.FILTER.create());
        filterBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBtn.getStyle()
            .set("height", "36px").set("min-width", "36px").set("padding", "0")
            .set("border-radius", "4px 0 0 4px").set("border", "1px solid #ccc").set("border-right", "none")
            .set("background-color", "lightblue").set("color", "black");
        filterBtn.addClickListener(e -> openFilterDialog());

        // Search field
        TextField searchBar = new TextField();
        searchBar.setPlaceholder("Search items…");
        searchBar.setWidth("400px");
        searchBar.getStyle()
            .set("height", "38px").set("border-radius", "0").set("border-left", "none").set("border-right", "none");

        // Search button
        Button searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
            .set("height", "36px").set("min-width", "36px").set("padding", "0")
            .set("border-radius", "0 4px 4px 0").set("border", "1px solid #ccc").set("border-left", "none")
            .set("background-color", "#F7B05B").set("color", "black");

        HorizontalLayout group = new HorizontalLayout(filterBtn, searchBar, searchBtn);
        group.setWidthFull();
        group.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        group.setSpacing(false);
        return group;
    }

    private void openFilterDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");

        NumberField minPrice = new NumberField("Min Price");
        NumberField maxPrice = new NumberField("Max Price");
        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems("All", "Electronics", "Clothing", "Books");
        ComboBox<Integer> rating = new ComboBox<>("Rating");
        rating.setItems(1,2,3,4,5);

        Button apply = new Button("Apply", e -> dialog.close());
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
            new Text("Filter by:"),
            minPrice, maxPrice, category, rating, apply
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    private VerticalLayout createItemCard(ItemDTO item) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd").set("border-radius", "8px")
            .set("padding", "1rem").set("width", "200px");

        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "0.9em");
        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");

        // Quantity controls
        AtomicInteger qty = new AtomicInteger(1);
        Span qtyLabel = new Span(String.valueOf(qty.get()));
        Button minus = new Button("–", e -> {
            if (qty.get() > 1) qty.decrementAndGet();
            qtyLabel.setText(String.valueOf(qty.get()));
        });
        Button plus = new Button("+", e -> {
            qty.incrementAndGet();
            qtyLabel.setText(String.valueOf(qty.get()));
        });
        minus.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");
        plus.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");
        HorizontalLayout qtyControls = new HorizontalLayout(minus, qtyLabel, plus);
        qtyControls.setAlignItems(FlexComponent.Alignment.CENTER);

        Button add = new Button("Add to Cart", e -> 
            presenter.saveInCart(new ItemDTO(
                item.getName(), item.getCategory(), item.getPrice(),
                item.getShopId(), item.getItemID(), qty.get(), item.getRating(), item.getDescription(), item.getNumOfOrders()
            ))
        );
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setWidthFull();
        add.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");

        card.add(name, price, qtyControls, add);

        card.addClickListener(e -> {
            Dialog details = new Dialog();
            details.setWidth("400px");
            details.add(
                new H2(item.getName()),
                new Paragraph("Category: " + item.getCategory().name()),
                new Paragraph("Description: " + item.getDescription()),
                new Paragraph("Price: $" + item.getPrice()),
                new Paragraph("Rating: ⭐ " + item.getRating()),
                new Paragraph("Available: " + item.getQuantity()),
                new Button("Close", ev -> details.close())
            );
            details.open();
        });

        return card;
    }
}
