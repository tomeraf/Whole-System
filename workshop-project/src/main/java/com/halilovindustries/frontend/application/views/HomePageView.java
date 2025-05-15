
package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.NumberField;
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
import java.util.HashMap;
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
    private HorizontalLayout randomSection;
    private HashMap<String, String> filters = new HashMap<>();
    //private final FlexLayout itemsLayout = new FlexLayout();
    private final FlexLayout cardsLayout;
    private TextField searchBar;
    private Button searchBtn;
    private Button filterBtn;
    @Autowired
    public HomePageView(HomePresenter p) {
        this.presenter = p;

        getContent().add(createHeader());
        searchBar = new TextField();
        cardsLayout = new FlexLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardsLayout.getStyle().set("gap", "1rem");
        
        getContent().add(cardsLayout);
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout leftControls = new HorizontalLayout(createCartButton());
        leftControls.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout centerControls = new HorizontalLayout(createSearchBar());
        centerControls.setAlignItems(FlexComponent.Alignment.CENTER);
        centerControls.setWidthFull();
        centerControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        HorizontalLayout rightControls = createAuthButtons();
        // rightControls.setSpacing(true);
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);

        // Combine into header
        HorizontalLayout header = new HorizontalLayout(leftControls, centerControls, rightControls);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(centerControls);
        header.setPadding(true);
        header.getStyle().set("padding", "1rem");
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
        searchBar.setPlaceholder("Search");
        searchBar.setWidth("400px");
        searchBar.getStyle()
                .set("height", "38px")
                .set("border-radius", "4px 0 0 4px")
                .set("border-right", "none");

        searchBtn = new Button(VaadinIcon.SEARCH.create());
        searchBtn.getStyle()
                .set("height", "38px")
                .set("min-width", "38px")
                .set("width", "38px")
                .set("padding", "0")
                .set("border-radius", "0 4px 4px 0")
                .set("border", "1px solid #ccc")
                .set("background-color", "#F7B05B")
                .set("color", "black");
        searchBtn.addClickListener(e -> doSearch());

        filterBtn = new Button("", VaadinIcon.FILTER.create());
        filterBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        filterBtn.getStyle()
                .set("height", "36px")
                .set("min-width", "36px")
                .set("padding", "0")
                .set("border-radius", "4px 0 0 4px")       // round left corners
                .set("border", "1px solid #ccc")
                .set("border-right", "none")                // drop the right border
                .set("background-color", "lightblue")
                .set("color", "black");

        filterBtn.addClickListener(e -> openFilterDialog());

        HorizontalLayout searchContainer = new HorizontalLayout(filterBtn, searchBar, searchBtn);
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

    private void loadRandomItems() {
        // now fetch 3 items
        presenter.getRandomItems(3, items -> {
            // back on the UI thread, add their cards
            UI.getCurrent().access(() -> {
                cardsLayout.removeAll();
                items.forEach(item -> cardsLayout.add(createItemCard(item)));
            });
        });
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

    private HorizontalLayout createRandomItemsSection() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.getStyle().set("gap", "1rem");

        // asynchronously fetch 3 random items…
        presenter.getRandomItems(3, items -> {
        // back onto the UI thread to mutate the layout
            UI.getCurrent().access(() -> {
                layout.removeAll();
                for (ItemDTO item : items) {
                    layout.add(createItemCard(item));
                }
            });
        });

        return layout;
    }

    private VerticalLayout createItemCard(ItemDTO item) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");        // or “30%” if you prefer fluid sizing

        // image
        String q = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
        Image img = new Image("https://source.unsplash.com/100x100/?" + q, item.getName());
        img.setWidth("80px");
        img.setHeight("80px");

        // name & price
        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "0.9em");
        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");

        // Save In Cart
        Button save = new Button("Save In Cart", evt -> presenter.saveInCart(item));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle().set("width", "100%");

        card.add(img, name, price, save);
        return card;
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

                // Store this view's listener registration
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

    private void doSearch() {        
        // presenter.showItemsByFilter(filters, items -> {
        //     UI.getCurrent().access(() -> {
        //         cardsLayout.removeAll();
        //         items.forEach(item -> cardsLayout.add(createItemCard(item)));
        //     });
        // });
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
        loadRandomItems();
    }

    @ClientCallable
    public void onBrowserUnload() {
        // fire your normal logout logic
        doLogout();
    }

    private void openFilterDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");

        NumberField minPrice = new NumberField("Min Price");
        NumberField maxPrice = new NumberField("Max Price");

        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems("All", "Electronics", "Clothing", "Books"); // example

        ComboBox<Integer> ItemRating = new ComboBox<>("Item Rating");
        ItemRating.setItems(1, 2, 3, 4, 5);

        ComboBox<Integer> ShopRating = new ComboBox<>("Shop Rating");
        ShopRating.setItems(1, 2, 3, 4, 5);

        Button apply = new Button("Apply", e -> {
            filters.put("name", searchBar.getValue());
            filters.put("category", category.getValue());
            filters.put("minPrice", String.valueOf(minPrice.getValue()));
            filters.put("maxPrice", String.valueOf(minPrice.getValue()));
            filters.put("minRating", String.valueOf(ItemRating.getValue()));
            filters.put("shopRating", String.valueOf(ShopRating.getValue()));
            // and pass them to your presenter
            dialog.close();
        });
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
                new Text("Filter by:"),
                minPrice,
                maxPrice,
                category,
                ItemRating,
                ShopRating,
                apply
        );
        layout.setPadding(false);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.open();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        doLogout();
        // presenter.exitAsGuest();
    }
}