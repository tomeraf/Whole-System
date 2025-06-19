package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class DatabaseNavigationGuard implements BeforeEnterListener {
    
    private static final Set<String> DATA_DEPENDENT_VIEWS = new HashSet<>(Arrays.asList(
        "shops", "cart", "myshops", "orders", "auction"
    ));
    
    private final DatabaseHealthService dbHealthService;
    
    @Autowired
    public DatabaseNavigationGuard(DatabaseHealthService dbHealthService) {
        this.dbHealthService = dbHealthService;
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String targetView = event.getLocation().getPath();
        
        if (isDatabaseRequired(targetView) && !dbHealthService.isHealthy()) {
            // Database is required but unavailable - prevent navigation
            event.rerouteToError(new DatabaseUnavailableException(), 
                "Database is currently unavailable. Please try again later.");
            
            // Show notification
            Notification notification = new Notification(
                "Cannot access " + targetView + " - Database is currently unavailable.", 
                5000, 
                Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            
            // Stay on current page instead
            if (!targetView.equals("")) {
                event.forwardTo("");
            }
        }
    }
    
    private boolean isDatabaseRequired(String viewName) {
        return DATA_DEPENDENT_VIEWS.contains(viewName.toLowerCase());
    }
    
    public static class DatabaseUnavailableException extends Exception {
        public DatabaseUnavailableException() {
            super("Database is currently unavailable");
        }
    }
}