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

    private DatabaseStatusView databaseStatusView;

    private H1 viewTitle;
    private HomePresenter presenter;
    private VerticalLayout drawerContent;
    private Header header;
    private Scroller scroller;
    private Span greeting;

    private Registration broadcasterRegistration;

    @Autowired
    public MainLayout(HomePresenter presenter, 
                      DatabaseStatusView databaseStatusView) {

        this.presenter = presenter;
        setPrimarySection(Section.DRAWER);          // Set the primary section to the drawer
        this.drawerContent = new VerticalLayout();   // init
        addHeaderContent();                         // Add header
        
        // Add the database status view to the navbar (it's invisible anyway)
        databaseStatusView.setVisible(false);  // Ensure it's not visible
        addToNavbar(databaseStatusView);
    }
    
    /** Rebuilds header + nav + footer in the drawer */
    public void refreshDrawer() {
        if(scroller != null) {
            remove(scroller);
        }
        if(header != null) {
            remove(header);
        }
        if(greeting != null) {
            remove(greeting);
        }
        
        presenter.getSessionToken(token -> {
            if (token == null || !presenter.validateToken(token)) {
                // // invalid token → build empty drawer
                // UI.getCurrent().access(() -> {
                //     Scroller scroller = new Scroller(createNavigation());
                //     addToDrawer(scroller);
                // });
                return;   
            }

            // valid token → build full drawer
            UI.getCurrent().access(() -> {

                scroller = new Scroller(createNavigation());

                addToDrawer(header, scroller, createFooter());   
            
            });
        });
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
                    if (loggedIn) {
                        String id = presenter.extractUserId(token);
                        registerForNotifications(id);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error in onAttach: " + e.getMessage());
                }
            });
        });
        refreshDrawer();
    }
}