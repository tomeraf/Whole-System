package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.User.Suspension;
import com.halilovindustries.frontend.application.presenters.SystemManagerPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.halilovindustries.backend.Domain.DTOs.*;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Route(value = "system-manager", layout = MainLayout.class)
@PageTitle("System Manager")
public class SystemManagerView extends VerticalLayout 
        //implements BeforeEnterObserver 
        {

    private final SystemManagerPresenter presenter;
    private final Grid<Map.Entry<String, Suspension>> grid;
    private final Map<String, Suspension> suspensions = new LinkedHashMap<>();
    private final Grid<ShopDTO> closedShopsGrid;
    private final Map<String, Void> closedShops = new LinkedHashMap<>();





    public SystemManagerView(SystemManagerPresenter presenter) {
        this.presenter = presenter;
        setPadding(true);
        setSpacing(true);

        // Top bar: title + action buttons aligned right
        H2 header = new H2("System Manager");
        Button closeShopBtn = new Button("Close Shop");
        closeShopBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeShopBtn.addClickListener(e -> openCloseShopDialog());

        Button suspendUserBtn = new Button("Suspend User");
        suspendUserBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        suspendUserBtn.addClickListener(e -> openSuspendUserDialog());

        Button unsuspendUserBtn = new Button("Unsuspend User");
        unsuspendUserBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        unsuspendUserBtn.addClickListener(e -> openUnsuspendUserDialog());

        HorizontalLayout buttonBar = new HorizontalLayout(closeShopBtn, suspendUserBtn, unsuspendUserBtn);
        buttonBar.setSpacing(true);

        // then create topBar with header + buttonBar
        HorizontalLayout topBar = new HorizontalLayout(header, buttonBar);
        topBar.setWidthFull();
        // give all extra space to the header so buttons float right
        topBar.expand(header);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        add(topBar);

        // Grid for active suspended users
        grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey)
            .setHeader("User Name").setAutoWidth(true);
        grid.addColumn(entry -> entry.getValue().getStartDate())
            .setHeader("Date From").setAutoWidth(true);
        grid.addColumn(entry -> entry.getValue().getEndDate())
            .setHeader("Date To").setAutoWidth(true);
        grid.setItems(suspensions.entrySet());
        grid.setSizeFull();
        add(grid);
        setSizeFull();

        presenter.watchSuspensions(raw -> {
        UI.getCurrent().access(() -> {
            suspensions.clear();
            if (raw != null) {
                suspensions.putAll(parseSuspensionData(raw));
            }
            grid.setItems(suspensions.entrySet());
        });
    });

        closedShopsGrid = new Grid<>();
        closedShopsGrid.addColumn(ShopDTO::getName)
                .setHeader("Shop Name").setAutoWidth(true);
        closedShopsGrid.addColumn(ShopDTO::getId)
                .setHeader("Shop ID").setAutoWidth(true);
        closedShopsGrid.setItems(Collections.emptyList()); // placeholder until data arrives
        closedShopsGrid.setSizeFull();
        add(closedShopsGrid);

        // update closed‐shops in exactly the same pattern
        presenter.watchClosedShops(list -> {
            UI.getCurrent().access(() -> {
                if (list != null) {
                    closedShopsGrid.setItems(list);
                } else {
                    closedShopsGrid.setItems(Collections.emptyList());
                }
            });
        });




    }

    private void openSuspendUserDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");
        dlg.add(new H2("Suspend User"));

        TextField usernameField = new TextField("Username");
        DateTimePicker startPicker = new DateTimePicker("Start Suspension");
        DateTimePicker endPicker   = new DateTimePicker("End Suspension");

        Button yes = new Button("Suspend", evt -> {
            String username = usernameField.getValue().trim();
            Optional<LocalDateTime> start = Optional.ofNullable(startPicker.getValue());
            Optional<LocalDateTime> end   = Optional.ofNullable(endPicker.getValue());
            if (username.isEmpty()) {
                Notification.show("Username is required", 2000, Position.MIDDLE);
                return;
            }
            presenter.suspendUser(username, start, end, success -> {
                UI.getCurrent().access(() -> {
                    if (success) {
                        Suspension suspension = new Suspension();
                        if (start.isPresent() && end.isPresent()) {
                            suspension.setSuspension(start.get(), end.get());
                        } else {
                            suspension.setSuspension();
                        }
                        suspensions.put(username, suspension);
                        grid.setItems(suspensions.entrySet());
                        dlg.close();
                    } else {
                        Notification.show("Failed to suspend user.", 2000, Position.MIDDLE);
                    }
                });
            });
        });
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button no = new Button("Cancel", evt -> dlg.close());
        HorizontalLayout actions = new HorizontalLayout(yes, no);
        actions.setSpacing(true);
        dlg.add(usernameField, startPicker, endPicker, actions);
        dlg.open();
    }

    private void openCloseShopDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("300px");
        dlg.add(new H2("Close Shop"));

        TextField shopNameField = new TextField("Shop Name");
        shopNameField.setRequired(true);


        Button close = new Button("Close", evt -> {
            String shopName = shopNameField.getValue();
            if (shopName.isEmpty()) {
                Notification.show("Shop name is required", 3000, Position.MIDDLE);
                return;
            }
            presenter.getShopId(shopName, shopId -> {
                if (shopId == null) {
                    Notification.show("Shop not found", 3000, Position.MIDDLE);
                    return;
                }

                presenter.closeShop(shopId, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            dlg.close();
                        } else {
                        }
                    });
                });
            });
        });
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", evt -> dlg.close());

        HorizontalLayout actions = new HorizontalLayout(close, cancel);
        dlg.add(shopNameField, actions);
        dlg.open();
    }
    
    private void openUnsuspendUserDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("300px");
        dlg.add(new H2("Unsuspend User"));

        TextField usernameField = new TextField("Username");
        usernameField.setWidthFull();

        Button yes = new Button("Unsuspend", ev -> {
            String username = usernameField.getValue().trim();
            if (username.isEmpty()) {
                Notification.show("Please enter a username.", 2000, Position.MIDDLE);
                return;
            }
            presenter.unsuspendUser(username, success -> {
                UI.getCurrent().access(() -> {
                    if (success) {
                        suspensions.remove(username);
                        grid.setItems(suspensions.entrySet());
                        dlg.close();
                    } else {
                    }
                });
            });
        });
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button no = new Button("Cancel", ev -> dlg.close());
        HorizontalLayout actions = new HorizontalLayout(yes, no);
        actions.setSpacing(true);
        dlg.add(usernameField, actions);
        dlg.open();
    }

private Map<String, Suspension> parseSuspensionData(String raw) {
    Map<String, Suspension> parsed = new LinkedHashMap<>();

    if (raw == null || raw.isBlank()) return parsed;

    String[] lines = raw.split("\n");
    for (String line : lines) {
        try {
            String[] parts = line.split(",");
            String username = null;
            boolean isPermanent = false;
            LocalDateTime start = null;
            LocalDateTime end = null;

            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String value = kv[1].trim();

                if (key.equals("user")) {
                    username = value;
                } else if (key.equals("Suspension")) {
                    if (value.equalsIgnoreCase("Permanent")) {
                        isPermanent = true;
                    } else if (value.startsWith("From ")) {
                        // Example: From 2025-05-19T06:00 to 2025-05-19T23:00
                        String[] dateParts = value.substring(5).split(" to ");
                        if (dateParts.length == 2) {
                            start = LocalDateTime.parse(dateParts[0].trim());
                            end = LocalDateTime.parse(dateParts[1].trim());
                        }
                    }
                }
            }

            if (username != null) {
                Suspension s = new Suspension();
                if (isPermanent) {
                    s.setSuspension(); // no-arg permanent
                } else if (start != null && end != null) {
                    s.setSuspension(start, end);
                } else {
                    continue; // skip if incomplete
                }
                parsed.put(username, s);
            }

        } catch (Exception e) {
            Notification.show("Failed to parse a line: " + line, 3000, Notification.Position.BOTTOM_START);
        }
    }

    return parsed;
}


}