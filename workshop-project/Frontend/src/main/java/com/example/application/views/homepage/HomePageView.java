package com.example.application.views.homepage;

import Domain.DTOs.ItemDTO;
import Domain.DTOs.ShopDTO;
import com.example.application.presenters.HomePresenter;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@CssImport("./themes/my-app/shop-cards.css")
@PageTitle("Home Page")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.PENCIL_RULER_SOLID)
public class HomePageView extends Composite<VerticalLayout> {
    private final HomePresenter presenter;

    @Autowired
    public HomePageView(HomePresenter p) {
        this.presenter = p;
        // 1) Search bar + connected search button
        TextField searchBar = new TextField();
        searchBar.setPlaceholder("Search");
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
        searchContainer.setSpacing(false);
        searchContainer.setPadding(false);
        searchContainer.setAlignItems(FlexComponent.Alignment.CENTER);

// 2) Round cart icon button
        Button viewCart = new Button(VaadinIcon.CART.create());
        viewCart.addThemeVariants(ButtonVariant.LUMO_ICON);
        viewCart.getStyle()
                .set("width", "3rem")
                .set("height", "3rem")
                .set("min-width", "3rem")
                .set("min-height", "3rem")
                .set("padding", "0")
                .set("border-radius", "50%")
                .set("background-color", "white")
                .set("border", "2px solid darkblue")
                .set("color", "darkblue");

// 3) Login & Register buttons
        Button loginButton = new Button("Login");
        loginButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        loginButton.getStyle()
                .set("border-radius", "16px")
                .set("padding", "0.5em 1em")
                .set("background-color", "white")
                .set("border", "2px solid darkblue");

        Button registerButton = new Button("Register");
        registerButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        registerButton.getStyle()
                .set("border-radius", "16px")
                .set("padding", "0.5em 1em")
                .set("background-color", "white")
                .set("border", "2px solid darkblue");

        // 1) Left: cart button
        HorizontalLayout leftControls = new HorizontalLayout(viewCart);
        leftControls.setAlignItems(FlexComponent.Alignment.CENTER);

// 2) Center: your searchContainer (already sized to 400px or whatever)
        HorizontalLayout centerControls = new HorizontalLayout(searchContainer);
        centerControls.setAlignItems(FlexComponent.Alignment.CENTER);

// 3) Right: login & register
        HorizontalLayout rightControls = new HorizontalLayout(loginButton, registerButton);
        rightControls.setSpacing(true);
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);

        centerControls.setWidthFull();
        centerControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);


// 4) Combine into header
        HorizontalLayout header = new HorizontalLayout(leftControls, centerControls, rightControls);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

// Make only the center expand to push left and right to edges
        header.expand(centerControls);

// Optional: add some padding/gap
        header.setPadding(true);
        header.getStyle().set("gap", "1rem");

// 5) Add header to your view
        getContent().add(header);

        // 1) Get 4 random shops
        List<ShopDTO> featuredShops = presenter.getRandomShops();

// 2) Container for the shop cards
        HorizontalLayout shopsLayout = new HorizontalLayout();
        shopsLayout.setWidthFull();
        shopsLayout.setSpacing(true);
        shopsLayout.setAlignItems(FlexComponent.Alignment.START);

// 3) Build each shop card with 4 random item images
        for (ShopDTO shop : featuredShops) {
            VerticalLayout card = new VerticalLayout();
            card.setAlignItems(FlexComponent.Alignment.CENTER);
            card.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("border-radius", "8px")
                    .set("padding", "1rem")
                    .set("width", "240px");

            // Shop name as title
            H3 title = new H3(shop.getName());
            title.getStyle().set("margin", "0 0 0.5rem 0");
            card.add(title);

            // Grid for 4 items
            Div grid = new Div();
            grid.getStyle()
                    .set("display", "grid")
                    .set("grid-template-columns", "1fr 1fr")
                    .set("gap", "0.5rem");

            // Fetch 4 random items for this shop
            List<ItemDTO> items = presenter.get4rndShopItems(shop);
            for (ItemDTO item : items) {
                VerticalLayout cell = new VerticalLayout();
                cell.setAlignItems(FlexComponent.Alignment.CENTER);
                cell.getStyle().set("padding", "0.25rem");

                // Use Unsplash to get a picture by item name
                String q = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
                String imgUrl = "https://source.unsplash.com/100x100/?" + q;
                Image img = new Image(imgUrl, item.getName());
                img.setWidth("80px");
                img.setHeight("80px");

                Span name = new Span(item.getName());
                name.getStyle().set("font-size", "0.8em");

                cell.add(img, name);
                grid.add(cell);
            }

            card.add(grid);
            shopsLayout.add(card);
        }

// 4) Add the featured-shops row to the view
        getContent().add(shopsLayout);
    }
}

