package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class DatabaseStatusView extends VerticalLayout {

    private final DatabaseHealthService dbHealthService;
    private ScheduledExecutorService executorService;
    private Registration broadcasterRegistration;
    private boolean lastKnownStatus = true;

    @Autowired
    public DatabaseStatusView(DatabaseHealthService dbHealthService) {
        this.dbHealthService = dbHealthService;
        setVisible(false); // Only show when there's an issue
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        
        // Create a scheduled task to check db status
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            boolean isConnected = dbHealthService.isDatabaseConnected();
            if (isConnected != lastKnownStatus) {
                lastKnownStatus = isConnected;
                ui.access(() -> {
                    if (!isConnected) {
                        showDatabaseDisconnectedMessage(ui);
                    } else {
                        showDatabaseReconnectedMessage(ui);
                    }
                });
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void showDatabaseDisconnectedMessage(UI ui) {
        Notification notification = new Notification(
            "Database connection lost. Please wait while we try to reconnect...",
            10000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private void showDatabaseReconnectedMessage(UI ui) {
        Notification notification = new Notification(
            "Database connection restored!",
            3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}