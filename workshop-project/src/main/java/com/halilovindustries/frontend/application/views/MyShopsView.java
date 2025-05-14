package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.halilovindustries.frontend.application.presenters.MyShopsPresenter;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.*;  // FlexLayout imported here
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@PageTitle("My Shops")
@Route(value = "my-shops", layout = MainLayout.class)
public class MyShopsView extends VerticalLayout {
    private final MyShopsPresenter presenter;
    private final FlexLayout cardsLayout;

    @Autowired
    public MyShopsView(MyShopsPresenter presenter) {
        this.presenter = presenter;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // header (back + create)
        Button back = new Button("← Back", e -> UI.getCurrent().navigate(""));
        Button create = new Button("Create Shop", e -> openCreateDialog());
        HorizontalLayout header = new HorizontalLayout(back, create);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        add(header);

        // a FlexLayout that will hold all the cards, wrapping as needed
        cardsLayout = new FlexLayout();
        cardsLayout.setWidthFull();
        cardsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // <<< ADD THESE TWO LINES TO ENABLE wrapping and 4-across modules >>>
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.setAlignItems(FlexComponent.Alignment.START);

        add(cardsLayout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadShops();
    }

    private void loadShops() {
        cardsLayout.removeAll();
        presenter.fetchMyShops((shops, ok) -> {
            UI.getCurrent().access(() -> {
                if (!ok) {
                    Notification.show("Could not load your shops", 2000, Position.MIDDLE);
                    return;
                }
                if (shops.isEmpty()) {
                    cardsLayout.add(new H3("You have no shops yet."));
                } else {
                    for (ShopDTO shop : shops) {
                        cardsLayout.add(createCard(shop));
                    }
                }
            });
        });
    }

    private VerticalLayout createShopCard(ShopDTO shop) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("200px");
        card.setHeight("120px");
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "0.5rem")
            .set("box-shadow", "2px 2px 6px rgba(0,0,0,0.1)")
            .set("cursor", "pointer");

        // <<< ADD THIS LINE TO force each card to occupy 25% of the row >>>
        card.getStyle().set("flex", "0 0 25%");

        // shop name as a button (full width)
        Button title = new Button(shop.getName(), e ->
            UI.getCurrent().navigate("shop/" + shop.getId())
        );
        title.setWidthFull();
        title.getStyle()
             .set("background", "none")
             .set("border", "none")
             .set("text-align", "center")
             .set("font-weight", "bold");
        title.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        card.add(title);
        return card;
    }

    private Button createCard1(ShopDTO shop) {
        Button card = new Button(shop.getName(), e ->
            UI.getCurrent().navigate("shop/" + shop.getId())
        );
        card.setWidth("200px");
        card.setHeight("200px");
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
            .set("background-color", "white")
            .set("cursor", "pointer");
        // remove the default button padding & background so it looks like a card:
        card.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_CONTRAST);
        return card;
    }

    private Div createCard(ShopDTO shop) {
        // card wrapper
        Div card = new Div();
        card.setWidth("200px");
        card.setHeight("200px");
        card.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("padding", "0.5rem")
            .set("background-color", "white")
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
            .set("cursor", "pointer");

        // click anywhere
        card.addClickListener(e ->
            UI.getCurrent().navigate("shop/" + shop.getId())
        );

        // 1) Title
        H3 title = new H3(shop.getName());
        title.getStyle().set("margin", "0 0 0.25rem 0");

        // 2) Description
        Div desc = new Div();
        desc.setText(shop.getDescription());
        desc.getStyle()
            .set("flex-grow", "1")                // fill the middle area
            .set("font-size", "0.9em")
            .set("color", "#555")
            .set("overflow", "hidden")            // hide anything beyond our clamp
            .set("display", "-webkit-box")        // use WebKit’s box model
            .set("-webkit-box-orient", "vertical")
            .set("-webkit-line-clamp", "2");      // show up to 2 lines

        // 3) Rating row
        Div ratingRow = new Div();
        ratingRow.getStyle()
            .set("font-size", "0.85em")
            .set("color", "#333");
        ratingRow.setText("⭐ " 
            + shop.getRating() 
            + "  (" 
            + shop.getRatingCount() 
            + " reviews)");

        // assemble
        card.add(title, desc, ratingRow);
        return card;
    }



    private void openCreateDialog() {
        Dialog dlg = new Dialog();
        TextField name = new TextField("Shop Name");
        TextArea desc = new TextArea("Description");
        Button ok = new Button("Create", e -> {
            presenter.createShop(name.getValue(), desc.getValue(), (newShop, success) -> {
                UI.getCurrent().access(() -> {
                    if (success) {
                        Notification.show("Created “" + newShop.getName() + "”", 1500, Position.TOP_CENTER);
                        dlg.close();
                        loadShops();
                    } else {
                        Notification.show("Failed to create shop", 2000, Position.MIDDLE);
                    }
                });
            });
        });
        Button cancel = new Button("Cancel", e -> dlg.close());
        HorizontalLayout actions = new HorizontalLayout(ok, cancel);
        actions.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(name, desc, actions);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dlg.add(layout);
        dlg.open();
    }
}
