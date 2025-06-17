package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.halilovindustries.websocket.DatabaseEventBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStatusView extends VerticalLayout {

    private final DatabaseEventBroadcaster broadcaster;
    private Registration broadcasterRegistration;

    private String lastMessageShown = "";
    private long lastMessageTime = 0;
    private static final long MESSAGE_DEBOUNCE_MS = 2000;

    @Autowired
    public DatabaseStatusView(DatabaseEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
        setVisible(false); // This component doesn't need to be visible
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        
        // Register to receive database events
        broadcasterRegistration = broadcaster.register(event -> {
            ui.access(() -> {
                if (event.isConnected()) {
                    showDatabaseReconnectedMessage(ui, event.getMessage());
                } else {
                    showDatabaseDisconnectedMessage(ui, event.getMessage());
                }
            });
        });
    }

    private void showDatabaseDisconnectedMessage(UI ui, String message) {
        long now = System.currentTimeMillis();
        // Only show message if it's different or if enough time has passed since the last one
        if (!message.equals(lastMessageShown) || (now - lastMessageTime > MESSAGE_DEBOUNCE_MS)) {
            lastMessageShown = message;
            lastMessageTime = now;

            Notification notification = new Notification(
                message, 10000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }
    }

    private void showDatabaseReconnectedMessage(UI ui, String message) {
        long now = System.currentTimeMillis();
        // Only show message if it's different or if enough time has passed since the last one
        if (!message.equals(lastMessageShown) || (now - lastMessageTime > MESSAGE_DEBOUNCE_MS)) {
            lastMessageShown = message;
            lastMessageTime = now;
            Notification notification = new Notification(
                message, 3000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.open();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }
}