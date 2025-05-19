package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.frontend.application.presenters.ShopsPresenter;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "shops", layout = MainLayout.class)
@PageTitle("Shops")
public class ShopsView extends VerticalLayout {

    private final ShopsPresenter presenter;
    private final TextField searchBar = new TextField();
    private final FlexLayout cardsLayout = new FlexLayout();

    @Autowired
    public ShopsView(ShopsPresenter presenter) {
        this.presenter = presenter;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createSearchBar());
        add(new H2("Shops"));

        cardsLayout.setWidthFull();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        cardsLayout.getStyle().set("gap", "1rem");
        add(cardsLayout);

        // Fetch and display some shops on load
        presenter.getNRandomShops(5, this::displayShops);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        presenter.showAllShops(this::displayShops);
    }

    private HorizontalLayout createSearchBar() {
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

        searchBtn.addClickListener(e -> {
            String searchTerm = searchBar.getValue().trim().toLowerCase();
            if (searchTerm.isEmpty()) {
                presenter.showAllShops(this::displayShops);
            } else {
                presenter.showAllShops(all -> {
                    List<ShopDTO> filtered = all.stream()
                            .filter(s -> s.getName().toLowerCase().contains(searchTerm))
                            .toList();
                    displayShops(filtered);
                });
            }
        });

        HorizontalLayout container = new HorizontalLayout(searchBar, searchBtn);
        container.setWidthFull();
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setSpacing(false);
        container.setPadding(false);
        container.setAlignItems(FlexComponent.Alignment.CENTER);

        return container;
    }

    private void displayShops(List<ShopDTO> shops) {
        cardsLayout.removeAll();
        if (shops == null || shops.isEmpty()) {
            cardsLayout.add(new Span("No shops found."));
            return;
        }
        shops.forEach(shop -> cardsLayout.add(createShopCard(shop)));
    }

    private VerticalLayout createShopCard(ShopDTO shop) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");

        Span name = new Span(shop.getName());
        name.getStyle().set("font-weight", "bold");
        Span rating = new Span("⭐ " + String.format("%.1f", shop.getRating()));

        Button viewBtn = new Button("View", e -> UI.getCurrent().navigate("shop/" + shop.getId()));
        viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewBtn.setWidthFull();

        card.add(name, rating, viewBtn);
        return card;
    }
}
