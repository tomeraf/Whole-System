package com.halilovindustries.frontend.application.views;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.frontend.application.presenters.MyShopPresenter;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "manage-shop/", layout = MainLayout.class)
@PageTitle("Manage Shop")
public class ManageShopView extends VerticalLayout implements HasUrlParameter<Integer> {

    private Button editItemsBtn;
    private Button membersBtn;
    private MyShopPresenter presenter;
    private String shopName;
    private Button inboxBtn;
    private Button policiesBtn;
    private Button closeBtn;
    private Button historyBtn;

    H2 title;

    @Autowired
    public ManageShopView(MyShopPresenter presenter) {
        this.presenter = presenter;

        setPadding(true);
        setSpacing(true);


        // 1️⃣ Title + Controls Row
        title = new H2(shopName);


        policiesBtn = new Button("Policies", VaadinIcon.GAVEL.create());
        policiesBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        policiesBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");


        // — Shop Members —
        membersBtn = new Button("Members", VaadinIcon.GROUP.create());
        membersBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        membersBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");

        // — Edit Items —
        editItemsBtn = new Button("Edit Items", VaadinIcon.EDIT.create());
        editItemsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        editItemsBtn.getStyle().set("background-color", "white")
                               .set("border", "2px solid darkblue");


        // — Inbox —
        inboxBtn = new Button("Inbox", VaadinIcon.ENVELOPE.create());
        inboxBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        inboxBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");

        // — Close Shop (red) —
        closeBtn = new Button("Close Shop", VaadinIcon.CLOSE.create());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        closeBtn.getStyle().set("background-color", "red")
                            .set("border", "white")
                            .set("color", "white");
        
        historyBtn = new Button("History", VaadinIcon.CLOCK.create());
        historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        historyBtn.getStyle().set("background-color", "white")
                            .set("border", "2px solid darkblue");
        
        

        // pack the buttons together
        HorizontalLayout controls = new HorizontalLayout(
                policiesBtn,        
                membersBtn,
                editItemsBtn,
                inboxBtn,
                historyBtn,
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

    @Override
    public void setParameter(BeforeEvent event, Integer shopID) {
        editItemsBtn.addClickListener(e -> UI.getCurrent().navigate("shop-supply/" + shopID));
        presenter.getShopInfo(shopID, shop -> {

            UI.getCurrent().access(() -> {
                title.setText(shop.getName());
                shopName = shop.getName();
            });
        });

        membersBtn.addClickListener(e -> UI.getCurrent().navigate("shop-members/" + shopID));

        inboxBtn.addClickListener(e -> UI.getCurrent().navigate("shop-inbox/" + shopID));

        policiesBtn.addClickListener(e -> UI.getCurrent().navigate("shop-policies/" + shopID));

        historyBtn.addClickListener(e -> UI.getCurrent().navigate("shop-history/" + shopID));

        closeBtn.addClickListener(e -> {
            // 1️⃣ Ask for confirmation
            Dialog confirm = new Dialog();
            confirm.setWidth("300px");
            confirm.add(new Text("Are you sure you want to close \"" + shopName + "\"?"));

            // 2️⃣ “Yes” = call the presenter
            Button yes = new Button("Yes", ev -> {
                presenter.closeShop(shopID, success -> {
                    // since closeShop uses ui.access internally, we can just react here
                    UI.getCurrent().access(() -> {
                        if (success) {
                            confirm.close();
                            
                            // Optional: navigate away, refresh parent view, etc.
                            UI.getCurrent().navigate(MyShopsView.class);
                        } else {
                            
                        }
                    });
                });
            });
            yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            // 3️⃣ “No” = just close the confirmation dialog
            Button no = new Button("No", ev2 -> confirm.close());

            // 4️⃣ Put them side by side and show
            HorizontalLayout buttons = new HorizontalLayout(yes, no);
            buttons.setSpacing(true);
            confirm.add(buttons);
            confirm.open();
        });
    }
}