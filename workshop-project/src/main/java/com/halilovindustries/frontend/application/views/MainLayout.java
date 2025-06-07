package com.halilovindustries.frontend.application.views;

import com.halilovindustries.frontend.application.presenters.SupplyPresenter;
import com.halilovindustries.websocket.Broadcaster;
import com.halilovindustries.websocket.SessionCleanupService;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private H1 viewTitle;
    private SupplyPresenter presenter;
    private VerticalLayout drawerContent;
    private Header header;
    private Scroller scroller;
    private Span greeting;
    private Registration broadcasterRegistration;
    //private Registration beforeUnloadRegistration;



    public MainLayout(SupplyPresenter presenter) {

    this.presenter = presenter;
    setPrimarySection(Section.DRAWER);          // Set the primary section to the drawer
    this.drawerContent = new VerticalLayout();   // init
    //drawerContent.setPadding(false);
    //drawerContent.setSpacing(false);
    //drawerContent.setWidthFull();
    //addToDrawer(drawerContent);                 // Add it here!
    //refreshDrawer();                            // Build it initially
    addHeaderContent();                         // Add header
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
                // 1) Greeting
                greeting = new Span(presenter.isLoggedIn(token)
                    ? "Hello, " + presenter.getUsername(token)
                    : "Hello, sign in");
                greeting.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
                
                greeting.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
                header = new Header(greeting);

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

    // private void addDrawerContent() {
        // presenter.getSessionToken(token -> {
        //     if (token != null && presenter.validateToken(token)) {
        //          Span appName;
        //         if (presenter.isLoggedIn(token)) {
        //             appName = new Span("Hello, " + presenter.getUsername(token));
        //         } else {
        //             appName = new Span("Hello, sign in");
        //         }
            //     appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
            //     Header header = new Header(appName);

            //     Scroller scroller = new Scroller(createNavigation());

            //     addToDrawer(header, scroller, createFooter());   
            // }
        // });
    // }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Home", HomePageView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("My Cart", CartView.class, VaadinIcon.CART.create()));
        nav.addItem(new SideNavItem("Shops", ShopsView.class, VaadinIcon.SHOP.create()));

        presenter.getSessionToken(token -> {
            if (token != null
                    && presenter.validateToken(token)
                    && presenter.isLoggedIn(token)) {

                nav.addItem(new SideNavItem("My Shops", MyShopsView.class, VaadinIcon.USER.create()));
                nav.addItem(new SideNavItem("Inbox",   InboxView.class,   VaadinIcon.ENVELOPE.create()));
                nav.addItem(new SideNavItem("Order History", OrdersView.class, VaadinIcon.CHECK.create()));

                presenter.isSystemManager(isManager -> {
                    if (isManager) {
                        nav.addItem(new SideNavItem(
                            "System",
                            SystemManagerView.class,
                            VaadinIcon.TOOLS.create()
                        ));
                    }
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

    public void registerForNotifications(String userId) {
        unregisterNotifications();  // in case we already had one
        
        // Register broadcaster
        broadcasterRegistration = Broadcaster.register(userId, message -> {
            UI ui = UI.getCurrent();
            if (ui != null && ui.isAttached()) {
                ui.access(() -> {
                    Notification.show("Notification: " + message, 
                                    10000, Notification.Position.TOP_CENTER);
                });
            }
        });
        
        // JavaScript-based detection for browser close
        UI ui = UI.getCurrent();
        if (ui != null) {
            // Generate a unique session ID to identify this specific connection
            String sessionId = UI.getCurrent().getUIId() + "-" + System.currentTimeMillis();
            
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
                "});" +
                // 3. Setup ping/heartbeat to detect disconnection
                "const heartbeatInterval = setInterval(function() {" +
                "  fetch('/ping?sessionId=' + sessionId).catch(e => {});" +
                "}, 30000);", // 30 second interval
                sessionId
            );
            
            // Store the sessionId with the registration for later reference
            ui.getSession().setAttribute("notificationSessionId", sessionId);
            ui.getSession().setAttribute("userId", userId);
        }
        
        System.out.println("MainLayout: Registered notifications for user " + userId);
    }
    
    @PostMapping("/unregister-notification")
    @GetMapping("/unregister-notification")
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
        }
    }

    
    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        
        // Check if this is happening because of navigation within the app
        // In navigation/refresh cases, we want to keep the listener active
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && session.hasLock()) {
            // If we still have a valid session with lock, this is likely 
            // just a navigation - don't unregister notifications
            System.out.println("MainLayout: UI detached but session active, keeping notifications");
            return;
        }
        
        // Otherwise, this is likely a browser close or timeout
        System.out.println("MainLayout: UI detached, unregistering notifications");
        unregisterNotifications();
    }
}