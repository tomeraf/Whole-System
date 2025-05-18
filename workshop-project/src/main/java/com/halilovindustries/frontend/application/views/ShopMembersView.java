// --- ShopMembersView.java ---
package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.UserDTO;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.frontend.application.presenters.MyShopPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "shop-members/", layout = MainLayout.class)
@PageTitle("Shop Members")
public class ShopMembersView extends VerticalLayout implements HasUrlParameter<Integer> {
    private final MyShopPresenter presenter;
    private int shopID;
    private H2 title;
    private Button back = new Button("← Back");
    private Button addManager;
    private Button addOwner;

    public ShopMembersView(MyShopPresenter myShopPresenter) {
        this.presenter = myShopPresenter;
        setPadding(true);
        setSpacing(true);

        // Header
        title = new H2();
        addManager = new Button("Add Manager", VaadinIcon.PLUS.create());
        addManager.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addManager.addClickListener(e -> openAddManagerDialog());

        addOwner = new Button("Add Owner", VaadinIcon.PLUS.create());
        addOwner.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addOwner.addClickListener(e -> openAddOwnerDialog());

        back.addClickListener(e -> {
            if (shopID > 0) {
                UI.getCurrent().navigate("manage-shop/" + shopID);
            }
        });

        HorizontalLayout header = new HorizontalLayout(back, title, addManager, addOwner);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        add(header);
    }

    private void openAddManagerDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        TextField nameField = new TextField("Username");
        nameField.setWidthFull();
        Button confirm = new Button("Add", ev -> {
            String username = nameField.getValue();
            if (username != null && !username.isEmpty()) {
                presenter.addShopManager(shopID, username, Set.of(), success -> {
                    if (Boolean.TRUE.equals(success)) {
                        reloadMembers();
                    }
                });
            }
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", ev -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(confirm, cancel);
        actions.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(nameField, actions);
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    private void openAddOwnerDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        TextField nameField = new TextField("Username");
        nameField.setWidthFull();
        Button confirm = new Button("Add", ev -> {
            String username = nameField.getValue();
            if (username != null && !username.isEmpty()) {
                presenter.addShopOwner(shopID, username, success -> {
                    if (Boolean.TRUE.equals(success)) {
                        reloadMembers();
                    }
                });
            }
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", ev -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(confirm, cancel);
        actions.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(nameField, actions);
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }


    @Override
    public void setParameter(BeforeEvent event, Integer shopID) {
        this.shopID = shopID;
        presenter.getShopInfo(shopID, shop -> {
            UI.getCurrent().access(() -> title.setText(shop.getName() + "'s Members"));
            reloadMembers();
        });
    }

    private void reloadMembers() {
        System.out.println("Reloading members for shop ID: " + shopID);
        removeAll();
        HorizontalLayout header = new HorizontalLayout(back, title, addManager, addOwner);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        add(header);

        presenter.getShopMembers(shopID, members -> {
            System.out.println("Members fetched: " + members);
            UI.getCurrent().access(() -> {
                if (members == null || members.isEmpty()) {
                    add(new H4("No members found."));
                } else {
                    HorizontalLayout cards = new HorizontalLayout();
                    cards.setPadding(true);
                    cards.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                    cards.setAlignItems(FlexComponent.Alignment.START);
                    cards.getStyle().set("gap", "2rem");
                    cards.setWidthFull();

                    for (UserDTO user : members) {
                        // Fetch permissions per user
                        presenter.checkMemberPermissions(user.getUsername(), shopID, permsList -> {
                            UI.getCurrent().access(() -> {
                                Set<Permission> currentPerms = permsList != null ? new HashSet<>(permsList) : new HashSet<>();
                                if (currentPerms.contains(Permission.OWNER)) {
                                    cards.add(createMemberCard(user.getUsername(), null));
                                } else {
                                    Set<String> readablePerms = currentPerms.stream()
                                            .map(Permission::toString)
                                            .collect(Collectors.toSet());
                                    cards.add(createMemberCard(user.getUsername(), readablePerms));
                                }

                            });
                        });
                    }
                    add(cards);
                }
            });
        });
    }

    /**
     * An “owner” card gets a gold accent, a big OWNER label, and no extra buttons.
     */
    private VerticalLayout createOwnerCard1(String username) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "2px solid gold")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("background-color", "#FFF8E1");

        Span role = new Span("OWNER");
        role.getStyle()
            .set("color", "goldenrod")
            .set("font-weight", "bold")
            .set("font-size", "0.9em");

        H4 name = new H4(username);

        card.add(role, name);
        return card;
    }

    /**
     * A “manager” card gets a cool blue accent, a MANAGER label,
     * and a little list of their permissions.
     */
    private VerticalLayout createManagerCard1(String username, Set<Permission> perms) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "2px solid cornflowerblue")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("background-color", "#E3F2FD");

        Span role = new Span("MANAGER");
        role.getStyle()
            .set("color", "steelblue")
            .set("font-weight", "bold")
            .set("font-size", "0.9em");

        H4 name = new H4(username);

        // turn the enum set into a comma-separated list:
        String permsLine = perms.stream()
                                .map(Enum::name)
                                .collect(Collectors.joining(", "));
        Span permsSpan = new Span("Permissions: " + permsLine);
        permsSpan.getStyle().set("font-size", "0.8em").set("color", "#555");

        card.add(role, name, permsSpan);
        return card;
    }

    private VerticalLayout createMemberCard(String memberName, Set<String> currentPerms) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("width", "250px");

        H4 name = new H4(memberName);
        card.add(name);

        if (currentPerms != null) {
            MultiSelectComboBox<String> perms = new MultiSelectComboBox<>("Permissions");
            List<Permission> allPerms = new ArrayList<>(java.util.Arrays.asList(Permission.values()));
            allPerms.remove(Permission.OWNER);
            allPerms.remove(Permission.FOUNDER);
            perms.select(currentPerms);
            perms.setWidthFull();
            card.add(perms);
        } else {
            Span ownerLabel = new Span("OWNER");
            ownerLabel.getStyle().set("color", "goldenrod").set("font-weight", "bold");
            card.add(ownerLabel);
        }

        Button remove = new Button("Remove", VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        remove.addClickListener(e -> {
            presenter.removeAppointment(shopID, memberName, success -> {
                if (Boolean.TRUE.equals(success)) {
                    Notification.show("Member removed successfully", 3000, Position.MIDDLE);
                    reloadMembers();
                } else {
                    Notification.show("Failed to remove member", 3000, Position.MIDDLE);
                }
            });
        });
        card.add(remove);

        return card;
    }
}
