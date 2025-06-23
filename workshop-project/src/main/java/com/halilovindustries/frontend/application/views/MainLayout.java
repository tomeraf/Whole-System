package com.halilovindustries.frontend.application.views;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.halilovindustries.frontend.application.presenters.SupplyPresenter;
import com.halilovindustries.websocket.Broadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    //private DatabaseStatusView databaseStatusView;

    private H1 viewTitle;
    private HomePresenter presenter;
    private VerticalLayout drawerContent;
    private Header header;
    private Scroller scroller;
    private Span greeting;

    private Registration broadcasterRegistration;
    private boolean isInMaintenanceMode = false;

    @Autowired
    public MainLayout(HomePresenter presenter
                      //,DatabaseStatusView databaseStatusView
                      ) {

        this.presenter = presenter;
        setPrimarySection(Section.DRAWER);          // Set the primary section to the drawer
        this.drawerContent = new VerticalLayout();   // init
        addHeaderContent();                         // Add header
        
        // Add the database status view to the navbar (it's invisible anyway)
        //databaseStatusView.setVisible(false);  // Ensure it's not visible
        //addToNavbar(databaseStatusView);
    }
    
    /** Rebuilds header + nav + footer in the drawer */
    public void refreshDrawer() {
        // First check if UI is properly attached
        UI ui = UI.getCurrent();
        if (ui == null || !ui.isAttached()) {
            System.out.println("Not refreshing drawer - UI not available");
            return;
        }
        
        try {
            // Remove existing components
            if(scroller != null) {
                remove(scroller);
                scroller = null;
            }
            if(header != null) {
                remove(header);
                header = null;
            }
            if(greeting != null) {
                remove(greeting);
                greeting = null;
            }
            
            // Create basic navigation regardless of database state
            SideNav nav = new SideNav();
            nav.addItem(new SideNavItem("Home", HomePageView.class, VaadinIcon.HOME.create()));
            nav.addItem(new SideNavItem("Shops", ShopsView.class, VaadinIcon.SHOP.create()));
            nav.addItem(new SideNavItem("My Cart", CartView.class, VaadinIcon.CART.create()));

            // Initialize greeting with default value
            String defaultGreeting = "Hello, sign in";
            greeting = new Span(defaultGreeting);
            greeting.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
            header = new Header(greeting);
            
            // Always add these basic components to ensure UI renders
            scroller = new Scroller(nav);
            addToDrawer(header, scroller, createFooter());
            
            // Then try to enhance with user data if available
            try {
                presenter.getSessionToken(token -> {
                    if (token != null && presenter.validateToken(token)) {
                        try {
                            // If user is logged in, add additional navigation options
                            if (presenter.isLoggedIn(token)) {
                                UI.getCurrent().access(() -> {
                                    try {
                                        // Update greeting
                                        greeting.setText("Hello, " + presenter.getUsername(token));
                                        
                                        // Add logged-in specific items
                                        nav.addItem(new SideNavItem("My Shops", MyShopsView.class, VaadinIcon.USER.create()));
                                        nav.addItem(new SideNavItem("Inbox", InboxView.class, VaadinIcon.ENVELOPE.create()));
                                        nav.addItem(new SideNavItem("Order History", OrdersView.class, VaadinIcon.CHECK.create()));
                                    } catch (Exception e) {
                                        System.err.println("Error adding nav items: " + e.getMessage());
                                    }
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("Error in token validation: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Error getting session token: " + e.getMessage());
                // Continue with basic layout even if this fails
            }
        } catch (Exception e) {
            System.err.println("Error in refreshDrawer: " + e.getMessage());
        }
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Home", HomePageView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("My Cart", CartView.class, VaadinIcon.CART.create()));
        nav.addItem(new SideNavItem("Shops", ShopsView.class, VaadinIcon.SHOP.create()));

        // Initialize greeting with default value first
        String defaultGreeting = "Hello, sign in";
        greeting = new Span(defaultGreeting);
        greeting.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        header = new Header(greeting);

        presenter.getSessionToken(token -> {
            if (token != null && presenter.validateToken(token) && presenter.isLoggedIn(token)) {
                // Update greeting for logged in user
                String userGreeting = "Hello, " + presenter.getUsername(token);
                
                UI.getCurrent().access(() -> {
                    greeting.setText(userGreeting);
                    
                    // Add logged-in user specific navigation items
                    nav.addItem(new SideNavItem("My Shops", MyShopsView.class, VaadinIcon.USER.create()));
                    nav.addItem(new SideNavItem("Inbox", InboxView.class, VaadinIcon.ENVELOPE.create()));
                    nav.addItem(new SideNavItem("Order History", OrdersView.class, VaadinIcon.CHECK.create()));

                    presenter.isSystemManager(isManager -> {
                        if (isManager) {
                            UI.getCurrent().access(() -> {
                                nav.addItem(new SideNavItem(
                                    "System",
                                    SystemManagerView.class,
                                    VaadinIcon.TOOLS.create()
                                ));
                            });
                        }
                    });
                });
            } else {
                // Keep default greeting for non-logged-in users
                UI.getCurrent().access(() -> {
                    greeting.setText(defaultGreeting);
                });
            }
        });

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
        
        //refreshDrawer();
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }

    // ---------------------------------------------------------------------------------------

    public void registerForNotifications(String userId) {
        unregisterNotifications();  // in case we already had one
        
        // JavaScript-based detection for browser close
        UI ui = UI.getCurrent();
        if (ui != null) {

            // Generate a unique session ID to identify this specific connection
            String sessionId = UI.getCurrent().getUIId() + "-" + System.currentTimeMillis();

            // Register broadcaster
            broadcasterRegistration = Broadcaster.register(sessionId, userId, message -> {
                if (ui != null && ui.isAttached()) {
                    ui.access(() -> {
                        Notification.show("Notification: " + message, 
                                        10000, Notification.Position.TOP_CENTER);
                    });
                }
            });
        
            
            // Use multiple approaches for better reliability
            ui.getPage().executeJs(
                "const sessionId = $0;" +
                // 1. Use beforeunload (more reliable than unload)
                "window.addEventListener('beforeunload', function() {" +
                "  navigator.sendBeacon('/unregister-notification?sessionId=' + sessionId);" +
                "});" +
                // 2. Also use unload as backup
                "window.addEventListener('unload', function() {" +
                "  navigator.sendBeacon('/unregister-notification?sessionId=' + sessionId);" +
                "});",
                sessionId
            );
            
            // Store the sessionId with the registration for later reference
            ui.getSession().setAttribute("notificationSessionId", sessionId);
            ui.getSession().setAttribute("userId", userId);
        }
        
        System.out.println("MainLayout: Registered notifications for user " + userId);
    }
    
    public void unregisterNotifications() {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
            System.out.println("MainLayout: Unregistered broadcaster");
        }
        
        // Make debug logging more generic
        UI ui = UI.getCurrent();
        if (ui != null && ui.getSession() != null) {
            String userId = (String) ui.getSession().getAttribute("userId");
            if (userId != null) {
                System.out.println(Broadcaster.getListenerCount(userId) + 
                    " listeners left for current user: " + userId);
            }
            
            ui.getSession().setAttribute("notificationSessionId", null);
            ui.getSession().setAttribute("userId", null); // optional, if not needed anymore
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        System.out.println("MainLayout onAttach called");
        
        try {
            // CRITICAL: Check maintenance mode FIRST, before any DB operations
            try {
                isInMaintenanceMode = presenter.isInMaintenanceMode();
                System.out.println("Maintenance mode check result: " + isInMaintenanceMode);
            } catch (Exception e) {
                // If we can't even check maintenance mode, assume we're in it
                isInMaintenanceMode = true;
                System.out.println("Error checking maintenance mode, assuming maintenance: " + e.getMessage());
            }
            
            // Build UI with minimal DB operations if in maintenance mode
            if (isInMaintenanceMode) {
                // Skip any DB operations, just build basic UI
                createBasicUI();
                
                // Show maintenance notification
                UI.getCurrent().access(() -> {
                    Notification notification = Notification.show(
                        "System is in maintenance mode. Some features are limited.",
                        5000, 
                        Notification.Position.TOP_CENTER
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            } else {
                // Normal flow for non-maintenance mode
                refreshDrawer();
                
                // Try to register for notifications if user is logged in
                try {
                    presenter.getSessionToken(token -> {
                        // Existing token validation code...
                    });
                } catch (Exception e) {
                    System.err.println("Error checking session token: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in onAttach: " + e.getMessage());
            // Ensure we at least have a basic UI even in case of error
            createBasicUI();
        }
    }

    // New method to create a minimal UI without DB operations
    private void createBasicUI() {
        // Remove existing components first
        if(scroller != null) {
            remove(scroller);
            scroller = null;
        }
        if(header != null) {
            remove(header);
            header = null;
        }
        
        // Create basic navigation that works without DB
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Home", HomePageView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("Shops", ShopsView.class, VaadinIcon.SHOP.create()));
        nav.addItem(new SideNavItem("My Cart", CartView.class, VaadinIcon.CART.create()));
        
        // Create basic header
        greeting = new Span("Hello, sign in");
        greeting.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        header = new Header(greeting);
        
        // Add components to drawer
        scroller = new Scroller(nav);
        addToDrawer(header, scroller, createFooter());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        
        // Clean up UI components to prevent state tree issues
        if (scroller != null) {
            remove(scroller);
            scroller = null;
        }
        if (header != null) {
            remove(header);
            header = null;
        }
    }
}