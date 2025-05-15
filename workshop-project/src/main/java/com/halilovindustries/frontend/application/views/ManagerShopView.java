package com.halilovindustries.frontend.application.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "manage-shop", layout = MainLayout.class)
@PageTitle("Manage Shop")
public class ManagerShopView extends VerticalLayout {

    private final String shopName = "My Awesome Shop"; // replace with dynamic name

    public ManagerShopView() {
        setPadding(true);
        setSpacing(true);

        // 1️⃣ Title + Controls Row
        H2 title = new H2(shopName);

        // — Shop Members —
        Button membersBtn = new Button("Members", VaadinIcon.GROUP.create());
        membersBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        membersBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");
        membersBtn.addClickListener(e -> openMembersDialog());

        // — Edit Items —
        Button editItemsBtn = new Button("Edit Items", VaadinIcon.EDIT.create());
        editItemsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editItemsBtn.getStyle().set("background-color", "white")
                               .set("border", "2px solid darkblue");
        editItemsBtn.addClickListener(e -> openEditItemsDialog());

        // — Inbox —
        Button inboxBtn = new Button("Inbox", VaadinIcon.ENVELOPE.create());
        inboxBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        inboxBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");
        inboxBtn.addClickListener(e -> openInboxDialog());

        // — Close Shop (red) —
        Button closeBtn = new Button("Close Shop", VaadinIcon.CLOSE.create());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        closeBtn.getStyle().set("background-color", "red")
                            .set("border", "white")
                            .set("color", "white");
        closeBtn.addClickListener(e -> openCloseConfirmDialog());

        // pack the buttons together
        HorizontalLayout controls = new HorizontalLayout(
                membersBtn,
                editItemsBtn,
                inboxBtn,
                closeBtn
        );
        controls.setSpacing(true);
        controls.setAlignItems(FlexComponent.Alignment.CENTER);

        // header: title left, controls right
        HorizontalLayout header = new HorizontalLayout(title, controls);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        add(header);

        // … put the rest of your manager UI below here …
    }

    private void openMembersDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");
        dlg.add(new H2("Shop Members"));
        // TODO: list your managers here
        dlg.open();
    }

    private void openEditItemsDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");
        dlg.add(new H2("Edit Items"));
        // TODO: your edit-items UI
        dlg.open();
    }

    private void openInboxDialog() {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");
        dlg.add(new H2("Inbox Messages"));
        // TODO: your inbox UI
        dlg.open();
    }

    private void openCloseConfirmDialog() {
        Dialog confirm = new Dialog();
        confirm.setWidth("300px");
        confirm.add(new Text("Are you sure you want to close “" + shopName + "”?"));
        Button yes = new Button("Yes", e -> {
            // TODO: call presenter to close shop
            confirm.close();
        });
        Button no = new Button("No", e -> confirm.close());
        confirm.add(new HorizontalLayout(yes, no));
        confirm.open();
    }
}