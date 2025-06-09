package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@PreserveOnRefresh
@CssImport("./themes/my-app/shop-cards.css")
@PageTitle("Home Page")
@Route(value = "", layout = MainLayout.class)
public class HomePageView extends Composite<VerticalLayout> {
    private final HomePresenter presenter;
    private Button loginButton, registerButton, logoutButton;
    private Button viewCart;
    private HorizontalLayout randomSection;
    private HashMap<String, String> filters = new HashMap<>();
    //private final FlexLayout itemsLayout = new FlexLayout();
    private final FlexLayout cardsLayout;
    private TextField searchBar;
    private Registration broadcasterRegistration;

    @Autowired
    public HomePageView(HomePresenter p) {
        this.presenter = p;

        getContent().add(createHeader());
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
        searchBar = new TextField();
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

        // Populate filters here, then trigger search
        searchBtn.addClickListener(e -> {
            filters.clear();
            String q = searchBar.getValue().trim();
            if (!q.isEmpty()) {
                filters.put("name", q);
            }
            doSearch();
        });
        Button filterBtn = new Button("", VaadinIcon.FILTER.create());
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
        // 1) Card container
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");

        // 2) Name & price
        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "0.9em");
        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");

        // 3) Quantity selector
        AtomicInteger qty = new AtomicInteger(1);
        Span qtyLabel = new Span(String.valueOf(qty.get()));
        Button minus = new Button("–", e -> {
            if (qty.get() > 1) {
                qty.decrementAndGet();
                qtyLabel.setText(String.valueOf(qty.get()));
            }
        });
        Button plus = new Button("+", e -> {
            qty.incrementAndGet();
            qtyLabel.setText(String.valueOf(qty.get()));
        });
        // prevent qty buttons from triggering the card click
        minus.getElement().addEventListener("click", domEvent -> {}).addEventData("event.stopPropagation()");
        plus.getElement().addEventListener("click", domEvent -> {}).addEventData("event.stopPropagation()");

        HorizontalLayout qtyControls = new HorizontalLayout(minus, qtyLabel, plus);
        qtyControls.setAlignItems(FlexComponent.Alignment.CENTER);

        // 4) “Add to Cart” button
        Button add = new Button("Add to Cart", e -> {
            presenter.saveInCart(new ItemDTO(item.getName(), item.getCategory(), item.getPrice(), item.getShopId(), item.getItemID(), qty.get(),item.getRating(), item.getDescription(), item.getNumOfOrders()));
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setWidthFull();
        // stop it from firing the card‐click
        add.getElement().addEventListener("click", e -> {}).addEventData("event.stopPropagation()");

        // 5) Put it all together
        card.add(name, price, qtyControls, add);

        // 6) Click on the card → show details dialog
        card.addClickListener(e -> {
            Dialog details = new Dialog();
            details.setWidth("400px");
            details.add(
                new H3(item.getName()),
                new Paragraph("Category: " + item.getCategory().name()),
                new Paragraph("Description: " + item.getDescription()),
                new Paragraph("Price: $" + item.getPrice()),
                new Paragraph("Rating: ⭐ " + item.getRating()),
                new Paragraph("Available Quantity: " + item.getQuantity()),
                new Button("Close", ev -> details.close())
            );
            details.open();
        });

        return card;
    }


    // private VerticalLayout createItemCard(ItemDTO item) {
    //     VerticalLayout card = new VerticalLayout();
    //     card.setAlignItems(FlexComponent.Alignment.CENTER);
    //     card.getStyle()
    //         .set("border", "1px solid #ddd")
    //         .set("border-radius", "8px")
    //         .set("padding", "1rem")
    //         .set("width", "200px");        // or “30%” if you prefer fluid sizing

    //     // image
    //     // String q = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
    //     // Image img = new Image("https://source.unsplash.com/100x100/?" + q, item.getName());
    //     // img.setWidth("80px");
    //     // img.setHeight("80px");

    //     // name & price
    //     Span name = new Span(item.getName());
    //     name.getStyle().set("font-size", "0.9em");
    //     Span price = new Span("$" + item.getPrice());
    //     price.getStyle().set("font-weight", "bold");

    //     // Save In Cart
    //     Button save = new Button("Save In Cart", evt -> presenter.saveInCart(item));
    //     save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    //     save.getStyle().set("width", "100%");

    //     card.add(name, price, save);
    //     return card;
    // }

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
                    String userId = presenter.extractUserId(newToken);
                    registerForNotifications(userId);

                    // we already set localStorage & showed a welcome toast in presenter
                    showLoggedInUI();  
                    dialog.close();
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
            String currentUserId = null;
            
            // Check if we already have a registration for this user
            
                try {
                    // Get current token and extract user ID
                    presenter.getSessionToken(currentToken -> {
                        if (currentToken != null && presenter.validateToken(currentToken)) {
                            String currId = presenter.extractUserId(currentToken);
                            registerForNotifications(currId);
                        }
                    });
                } catch (Exception e) {

                }
                showLoggedInUI();
        }
    });
}

    private void doLogout() {
        // Find parent MainLayout and unregister there
        getUI().ifPresent(ui -> {
            ui.getChildren()
                .filter(component -> component instanceof MainLayout)
                .findFirst()
                .map(layout -> (MainLayout) layout)
                .ifPresent(MainLayout::unregisterNotifications);
        });
        
        presenter.logoutUser(); // performs backend logout and gets new guest token
        showGuestUI();          // switch back to guest view
    }

    private void doSearch() {
        presenter.showItemsByFilter(filters, items -> {
            UI.getCurrent().access(() -> {
                cardsLayout.removeAll();

                System.out.println("Removed all previous items");
                if (items.isEmpty()) {
                    // show a “no results” message in the cards area
                    Span none = new Span("No items found");
                    none.getStyle().set("font-style", "italic")
                                    .set("color", "#666")
                                    .set("padding", "2rem");
                    cardsLayout.add(none);
                } else {
                    // otherwise build one card per item
                    items.forEach(item -> cardsLayout.add(createItemCard(item)));
                }
            });
        });
    }

    /** Show login/register, hide logout */
    private void showGuestUI() {
        UI.getCurrent().access(() -> {
                    UI.getCurrent().getChildren()
                        .filter(c -> c instanceof MainLayout)
                        .map(c -> (MainLayout)c)
                        .forEach(MainLayout::refreshDrawer);
                        loginButton.setVisible(true);
                        registerButton.setVisible(true);
                        logoutButton.setVisible(false);
                    });
    }

    /** Hide login/register, show logout */
    private void showLoggedInUI() {
        UI.getCurrent().access(() -> {
                    UI.getCurrent().getChildren()
                        .filter(c -> c instanceof MainLayout)
                        .map(c -> (MainLayout)c)
                        .forEach(MainLayout::refreshDrawer);
                        loginButton.setVisible(false);
                        registerButton.setVisible(false);
                        logoutButton.setVisible(true);
                    });
    }


    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        System.out.println("HomePageView onAttach called");

        presenter.getSessionToken(token -> {
            UI ui = UI.getCurrent();
            
            if (ui == null || !ui.isAttached()) {
                System.out.println("UI not available or not attached in getSessionToken callback");
                return;
            }

            ui.access(() -> {
                try {
                    boolean valid = token != null && presenter.validateToken(token);
                    boolean loggedIn = valid && presenter.isLoggedIn(token);
                    System.out.println("Token validation: valid=" + valid + ", loggedIn=" + loggedIn);

                    if (!valid) {
                        // Token is null or corrupted → get a new guest
                        System.out.println("Invalid token - creating new guest session");
                        UI.getCurrent().getPage().executeJs(
                            "localStorage.removeItem('token'); sessionStorage.removeItem('token');"
                        );
                        presenter.saveSessionToken();
                        showGuestUI();
                    } else if (loggedIn) {
                        String userId = presenter.extractUserId(token);
                        System.out.println("User is logged in with ID: " + userId);
                        showLoggedInUI();
                    } else {
                        // Token is valid JWT but not logged in
                        System.out.println("Token valid but user not logged in");
                        UI.getCurrent().getPage()
                            .executeJs("return localStorage.getItem('token') === $0;", token)
                            .then(Boolean.class, isPersistent -> {
                                if (Boolean.TRUE.equals(isPersistent) || !presenter.isInSystem(token)) {
                                    UI.getCurrent().getPage().executeJs(
                                        "localStorage.removeItem('token'); sessionStorage.removeItem('token');"
                                    );
                                    presenter.saveSessionToken();
                                }
                                showGuestUI();
                            });
                    }
                } catch (Exception e) {
                    System.err.println("Error in onAttach: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        loadRandomItems();
    }

    private void clearStorage() {
        
        UI.getCurrent().getPage().executeJs(
            "localStorage.removeItem('token'); sessionStorage.removeItem('token');"
        );
    }


    private void openFilterDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");

        NumberField minPrice = new NumberField("Min Price");
        NumberField maxPrice = new NumberField("Max Price");

        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems(
        Arrays.stream(Category.values())
                .map(Category::name)
                .toArray(String[]::new)
        );
        ComboBox<Integer> ItemRating = new ComboBox<>("Item Rating");
        ItemRating.setItems(1, 2, 3, 4, 5);

        ComboBox<Integer> ShopRating = new ComboBox<>("Shop Rating");
        ShopRating.setItems(1, 2, 3, 4, 5);

        Button apply = new Button("Apply", e -> {
            filters.clear();

            // name filter
            String name = searchBar.getValue().trim();
            if (!name.isEmpty()) {
                filters.put("name", name);
            }

            // category filter
            String cat = category.getValue();
            if (cat != null && !cat.equals("All")) {
                filters.put("category", cat);
            }

            // minPrice filter
            Double min = minPrice.getValue();
            if (min != null) {
                filters.put("minPrice", min.toString());
            }

            // maxPrice filter — fixed!
            Double max = maxPrice.getValue();
            if (max != null) {
                filters.put("maxPrice", max.toString());
            }

            // item rating filter
            Integer ir = ItemRating.getValue();
            if (ir != null) {
                filters.put("minRating", ir.toString());
            }

            // shop rating filter
            Integer sr = ShopRating.getValue();
            if (sr != null) {
                filters.put("shopRating", sr.toString());
            }

            dialog.close();
            doSearch();
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

    /** Subscribe to receive live pushes for this user */
    private void registerForNotifications(String userId) {
        // Find parent MainLayout and register there
        getUI().ifPresent(ui -> {
            ui.getChildren()
                .filter(component -> component instanceof MainLayout)
                .findFirst()
                .map(layout -> (MainLayout) layout)
                .ifPresent(mainLayout -> mainLayout.registerForNotifications(userId));
        });
        
        // Still call your notification trigger
        presenter.loginNotify();
    }

    // /** Clean up subscription */
    // private void unregisterNotifications() {
    //     if (broadcasterRegistration != null) {
    //         broadcasterRegistration.remove();
    //         broadcasterRegistration = null;
    //         System.out.println("Unsubscribed from live notifications");
    //     }
    // }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        System.out.println("HomePageView: UI detached");
    }
}