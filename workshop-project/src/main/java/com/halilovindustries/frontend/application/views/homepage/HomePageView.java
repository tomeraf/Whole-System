package com.halilovindustries.frontend.application.views.homepage;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@CssImport("./themes/my-app/shop-cards.css")
@PageTitle("Home Page")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.PENCIL_RULER_SOLID)
public class HomePageView extends Composite<VerticalLayout> {
    private final HomePresenter presenter;
    private Registration myBroadcastRegistration;
    private Button loginButton, registerButton, logoutButton;
    private Button viewCart;
    private FlexLayout cardsLayout;
    @Autowired
    public HomePageView(HomePresenter p) {
        this.presenter = p;
        
        // Build UI components
        HorizontalLayout header = createHeader();
        
        // Add components to main layout
        getContent().add(header);

        cardsLayout = new FlexLayout();
        cardsLayout.addClassName("shop-cards-layout");
        cardsLayout.setSizeFull();

        // add to the root
        getContent().add(header, cardsLayout);
        getContent().setFlexGrow(1, cardsLayout);  // so cardsLayout expands
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout leftControls = new HorizontalLayout(createCartButton());
        leftControls.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout centerControls = new HorizontalLayout(createSearchBar());
        centerControls.setAlignItems(FlexComponent.Alignment.CENTER);
        centerControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        HorizontalLayout rightControls = createAuthButtons();
        rightControls.setSpacing(true);
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);

        // Combine into header
        HorizontalLayout header = new HorizontalLayout(leftControls, centerControls, rightControls);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.getStyle().set("gap", "1rem");
        
        return header;
    }

    private Button createCartButton() {
        viewCart = new Button(VaadinIcon.CART.create());
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
        
        viewCart.addClickListener(e -> UI.getCurrent().navigate("cart"));
        
        return viewCart;
    }

    private HorizontalLayout createSearchBar() {
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
        
        return searchContainer;
    }

    private HorizontalLayout createAuthButtons() {
        loginButton = createLoginButton();
        registerButton = createRegisterButton();
        logoutButton = createLogoutButton();
        
        return new HorizontalLayout(loginButton, registerButton, logoutButton);
    }

    private Button createLoginButton() {
        Button button = new Button("Login");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("border-radius", "16px")
                .set("padding", "0.5em 1em")
                .set("background-color", "white")
                .set("border", "2px solid darkblue");

        button.addClickListener(e -> openLoginDialog());
        return button;
    }

    private Button createRegisterButton() {
        Button button = new Button("Register");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("border-radius", "16px")
                .set("padding", "0.5em 1em")
                .set("background-color", "white")
                .set("border", "2px solid darkblue");

        button.addClickListener(e -> openRegisterDialog());
        return button;
    }

    private Button createLogoutButton() {
        Button button = new Button("Logout");
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("border-radius", "16px")
                .set("padding", "0.5em 1em")
                .set("background-color", "white")
                .set("border", "2px solid darkblue");

        button.addClickListener(e -> doLogout());
        return button;
    }
    private void openRegisterDialog() {
        // create the dialog
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        // form fields
        TextField nameField = new TextField("Name");
        PasswordField passwordField = new PasswordField("Password");
        DatePicker dobPicker = new DatePicker("Date of Birth");

        // submit button
        Button submit = new Button("Create Account", evt -> {
            String name = nameField.getValue().trim();
            String pw = passwordField.getValue();
            LocalDate dob = dobPicker.getValue();
            // fire off the registration
            presenter.registerUser(name, pw, dob, (newToken, success) -> {
                if (success) {
                    // we already set localStorage & showed a welcome toast in presenter
                    showLoggedInUI();  
                    dialog.close();
                } else {
                    // nothing to do—errors already surfaced in presenter
                }
            });
            dialog.close();
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // cancel
        Button cancel = new Button("Cancel", e -> dialog.close());

        // layout
        VerticalLayout layout = new VerticalLayout(
                nameField,
                passwordField,
                dobPicker,
                new HorizontalLayout(submit, cancel)
        );
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        dialog.add(layout);
        dialog.open();
    }

    private void openLoginDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        TextField usernameField = new TextField("Username");
        PasswordField passwordField = new PasswordField("Password");

        Button submit = new Button("Log In", evt -> {
            String username = usernameField.getValue().trim();
            String pw = passwordField.getValue();
            doLogin(username, pw);
            dialog.close();
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(
                usernameField,
                passwordField,
                new HorizontalLayout(submit, cancel)
        );
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        dialog.add(layout);
        dialog.open();
    }

    private void doLogin(String username, String password) {
        presenter.loginUser(username, password, (token, success) -> {
            if (success && token != null && presenter.validateToken(token)) {
                String userId = presenter.extractUserId(token);

                // ✅ Store this view's listener registration
                myBroadcastRegistration = presenter.subscribeToBroadcast(userId, msg -> {
                    UI.getCurrent().access(() -> {
                        Notification.show("Server: " + msg, 3000, Position.TOP_CENTER);
                    });
                });

                showLoggedInUI(); // hide login, show logout
            }
        });
    }

    private void doLogout() {
        if (myBroadcastRegistration != null) {
            myBroadcastRegistration.remove();
            myBroadcastRegistration = null;
        }
        presenter.logoutUser(); // performs backend logout and gets new guest token
        showGuestUI();          // switch back to guest view
    }

    /** Show login/register, hide logout */
    private void showGuestUI() {
        loginButton.setVisible(true);
        registerButton.setVisible(true);
        logoutButton.setVisible(false);
    }

    /** Hide login/register, show logout */
    private void showLoggedInUI() {
        loginButton.setVisible(false);
        registerButton.setVisible(false);
        logoutButton.setVisible(true);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        // Grab the token from localStorage:
        presenter.getSessionToken(token -> {
            // Vaadin callbacks already run on the UI thread, but to be safe:
            UI.getCurrent().access(() -> {
                System.out.println("Token: " + token);
                // If there's no token, or it's expired/invalid:
                if (token == null || !presenter.validateToken(token) || !presenter.isLoggedIn(token)) {
                    // create a fresh guest token
                    presenter.saveSessionToken();
                    showGuestUI();
                } else {
                    // leave the existing user token in place
                    showLoggedInUI();
                }
            });
        });

        // then load & render the shop cards
        loadShopCards();
    }

    private void loadShopCards() {
        presenter.showAllShops((shops, success) -> {
            if (!success) {
                UI.getCurrent().access(() ->
                    Notification.show("Failed to load shops", 2000, Position.TOP_CENTER)
                );
                return;
            }
            UI.getCurrent().access(() -> {
                cardsLayout.removeAll();
                // show up to 3 cards
                shops.stream()
                     .limit(3)
                     .forEach(shop -> cardsLayout.add(createShopCard(shop)));
            });
        });
    }

    private Component createShopCard(ShopDTO shop) {
        // image placeholder
        Div img = new Div();
        img.addClassName("shop-card-image");
        // shop info
        H4 title = new H4(shop.getName());
        Paragraph desc = new Paragraph(shop.getDescription());

        // “Save In Cart” button
        Button save = new Button("Save In Cart");
        save.addClassName("shop-card-button");
        save.addClickListener(e -> {
            // grab the first item from this shop (or however you choose)
            Integer firstItemId = shop.getItems().keySet().stream().findFirst().orElse(null);
            if (firstItemId == null) {
                Notification.show("No items to add!", 1500, Position.TOP_CENTER);
                return;
            }
            presenter.addItemToCart(
                shop.getId(),
                firstItemId,
                1,                 // quantity
                success -> UI.getCurrent().access(() -> {
                    if (success) {
                        Notification.show("Added to cart!", 1500, Position.TOP_CENTER);
                    } else {
                        Notification.show("Failed to add to cart", 2000, Position.MIDDLE);
                    }
                })
            );
        });
        save.addClassName("shop-card-button");

        // assemble
        VerticalLayout card = new VerticalLayout(img, title, desc, save);
        card.addClassName("shop-card");
        card.setPadding(false);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        return card;
    }

    @ClientCallable
    public void onBrowserUnload() {
        // fire your normal logout logic
        doLogout();
    }
}