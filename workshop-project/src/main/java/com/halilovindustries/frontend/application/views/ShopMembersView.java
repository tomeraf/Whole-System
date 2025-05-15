package com.halilovindustries.frontend.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Set;

@Route(value = "shop-members", layout = MainLayout.class)
@PageTitle("Shop Members")
public class ShopMembersView extends VerticalLayout {

    public ShopMembersView() {
        setPadding(true);
        setSpacing(true);

        // — Header —
        H2 title = new H2("Shop Members");
        Button addMember = new Button("Add Member", VaadinIcon.PLUS.create());
        addMember.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(title, addMember);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        add(header);

        // — Cards container laid out horizontally —
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setPadding(true);
        cardsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardsLayout.getStyle().set("gap", "4.5rem");
        cardsLayout.setAlignItems(FlexComponent.Alignment.START);
        cardsLayout.setWidthFull();
        cardsLayout.getStyle().set("flex-wrap", "wrap");  // allows wrapping to new line


        // stub: build 3 example cards
        for (int i = 1; i <= 3; i++) {
            cardsLayout.add(createMemberCard(
                    "Member " + i,
                    Set.of("Edit Items", "Manage Staff")
            ));
        }

        add(cardsLayout);
    }

    private VerticalLayout createMemberCard(String memberName, Set<String> currentPerms) {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("width", "250px");  // fixed card width

        H4 name = new H4(memberName);

        MultiSelectComboBox<String> perms = new MultiSelectComboBox<>("Permissions");
        perms.setItems("Edit Items", "Manage Staff", "View Orders");
        perms.select(currentPerms);
        perms.setWidthFull();

        Button remove = new Button("Remove", VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

        card.add(name, perms, remove);
        return card;
    }
}