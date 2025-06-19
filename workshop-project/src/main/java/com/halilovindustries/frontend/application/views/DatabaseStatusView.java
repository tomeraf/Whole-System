package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import com.halilovindustries.websocket.DatabaseEventBroadcaster;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStatusView extends Div {

    private final DatabaseEventBroadcaster broadcaster;
    private Registration broadcasterRegistration;
    private final Div maintenanceBanner;

    @Autowired
    public DatabaseStatusView(DatabaseEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
        maintenanceBanner = new Div();
        maintenanceBanner.addClassName("maintenance-banner");
        maintenanceBanner.getStyle()
            .set("position", "fixed")
            .set("top", "0")
            .set("left", "0")
            .set("width", "100%")
            .set("background-color", "#f8d7da")
            .set("color", "#721c24")
            .set("padding", "10px")
            .set("text-align", "center")
            .set("z-index", "9999")
            .set("font-weight", "bold");
        
        maintenanceBanner.setVisible(false);
        add(maintenanceBanner);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        UI ui = attachEvent.getUI();
        broadcasterRegistration = broadcaster.register(event -> {
            ui.access(() -> {
                boolean connected = event.isConnected();
                String message = event.getMessage();
                
                if (!connected) {
                    maintenanceBanner.setText(message);
                    maintenanceBanner.setVisible(true);
                } else {
                    maintenanceBanner.setVisible(false);
                }
            });
        });
    }
    
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
        super.onDetach(detachEvent);
    }
}