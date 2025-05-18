package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.frontend.application.presenters.MyShopPresenter;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "shop-supply/", layout = MainLayout.class)
@PageTitle("Shop Supply")
public class SupplyView extends Composite<VerticalLayout> implements HasUrlParameter<Integer> {

    private final MyShopPresenter presenter;
    private final FlexLayout itemsLayout = new FlexLayout();
    private Button back = new Button("← Back");
    
    private final H3 shopTitle = new H3();
    private Button addItemButton;
    @Autowired
    public SupplyView(MyShopPresenter presenter) {
        this.presenter = presenter;

        addItemButton = new Button("Add Item", VaadinIcon.PLUS.create());
        addItemButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout header = new HorizontalLayout(shopTitle, back, addItemButton);
        header.setWidthFull();
        header.expand(shopTitle);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().add(header);

        itemsLayout.setWidthFull();
        itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        itemsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        itemsLayout.getStyle().set("gap", "1rem");

        getContent().add(itemsLayout);
    }

    /**
     * Vaadin will call this *after* matching “shop/{someInt}”
     */
    @Override
    public void setParameter(BeforeEvent event, Integer shopID) {
        loadItems(shopID);
        addItemButton.addClickListener(e -> openAddItemDialog(shopID));  // open your “add item” dialog
        back.addClickListener(e -> UI.getCurrent().navigate("manage-shop/" + shopID));

    }

    private void loadItems(int shopID) {
        itemsLayout.removeAll();
        presenter.getShopInfo(shopID, shop -> {
            UI.getCurrent().access(() -> {
                if (shop == null) {
                    Notification.show("Failed to load shop", 2000, Position.MIDDLE);
                    return;
                }
                // set the header title
                shopTitle.setText(shop.getName());

                // now render each item
                shop.getItems().values()
                    .forEach(item -> itemsLayout.add(createItemCard(item)));
            });
        });
    }

    // private void openAddItemDialog1(int shopID) {
    //     Dialog dlg = new Dialog();
    //     dlg.setWidth("400px");

    //     TextField name = new TextField("Name");
    //     ComboBox<String> category = new ComboBox<>("Category");
    //     category.setItems(Arrays.asList(Category.values()).stream().map(Category::name).toArray(String[]::new));
    //     TextArea desc = new TextArea("Description");
    //     NumberField price = new NumberField("Price");
    //     price.setMin(0);

    //     Button save = new Button("Add", e -> {
    //         presenter.addItemToShop(shopID, name.getValue(), Category.valueOf(category.getValue()), price.getValue(), desc.getValue(),  item -> {
    //             if (item == null) {
    //                 UI.getCurrent().access(() ->
    //                     Notification.show("Failed to add item", 2000, Position.MIDDLE)
    //                 );
    //                 return;
    //             }
    //             createItemCard(item);
    //             loadItems(shopID);
    //             dlg.close();
    //         });
    //     });
    //     save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    //     Button cancel = new Button("Cancel", e -> dlg.close());
    //     cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    //     HorizontalLayout actions = new HorizontalLayout(save, cancel);
    //     actions.setSpacing(true);

    //     dlg.add(new VerticalLayout(name, category, desc, price, actions));
    //     dlg.open();
    // }

    private void openAddItemDialog(int shopID) {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");

        TextField name = new TextField("Name");
        name.setRequiredIndicatorVisible(true);
        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems(
        Arrays.stream(Category.values())
                .map(Category::name)
                .toArray(String[]::new)
        );
        category.setRequiredIndicatorVisible(true);
        TextArea desc = new TextArea("Description");
        
        NumberField price = new NumberField("Price");
        price.setMin(0);
        price.setRequiredIndicatorVisible(true);

        // —— Binder<Void> for validation only ——
        Binder<Void> validator = new Binder<>();
        validator.forField(name)
                .asRequired("Name is required")
                .bind(v -> null, (bean, v) -> {});
        validator.forField(category)
                .asRequired("Category is required")
                .bind(v -> null, (bean, v) -> {});
        validator.forField(price)
                .asRequired("Price is required")
                .bind(v -> null, (bean, v) -> {});

        Button save = new Button("Add", e -> {
            // run all the asRequired() checks:
            if (validator.validate().hasErrors()) {
                // any empty field will now show its error message underneath
                return;
            }
            // since DTO is untouched, manually pull values:
            presenter.addItemToShop(
                shopID,
                name.getValue(),
                Category.valueOf(category.getValue()),
                price.getValue(),
                desc.getValue(),
                item -> {
                    if (item == null) {
                        UI.getCurrent().access(() ->
                            Notification.show("Failed to add item", 2000, Position.MIDDLE)
                        );
                        return;
                    }
                    createItemCard(item);
                    loadItems(shopID);
                    dlg.close();
                }
            );
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dlg.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setSpacing(true);

        dlg.add(new VerticalLayout(name, category, desc, price, actions));
        dlg.open();
    }


    private VerticalLayout createItemCard(ItemDTO item) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");        // or “30%” if you prefer fluid sizing

        // image
        // String q = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
        // Image img = new Image("https://source.unsplash.com/100x100/?" + q, item.getName());
        // img.setWidth("80px");
        // img.setHeight("80px");

        // name & price
        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "1.9em");
        name.getStyle().set("font-weight", "bold");
        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");
        Span rating = new Span("⭐" + item.getRating());
        rating.getStyle().set("font-size", "0.85em");
        rating.getStyle().set("color", "#333");
        card.addClickListener(e ->
            editItemDetails(item)
        );
        card.add(name, price, rating);
        return card;
    }

    // private void editItemDetails1(ItemDTO item) {
    //     Div stock = new Div();
    //     stock.getStyle()
    //         .set("font-size", "0.85em")
    //         .set("color", "#333")
    //         .set("margin-bottom", "0.5rem");
    //     stock.setText(item.getQuantity() + " left in stock");

    //     Div description = new Div();
    //     description.getStyle()
    //         .set("font-size", "0.85em")
    //         .set("color", "#333")
    //         .set("margin-bottom", "0.5rem");
    //     description.setText(item.getDescription());

    //     Div price = new Div();
    //     price.getStyle()
    //         .set("font-size", "0.85em")
    //         .set("color", "#333")
    //         .set("margin-bottom", "0.5rem");
    //     price.setText("$" + item.getPrice());

    //     Div rating = new Div();
    //     rating.getStyle()
    //         .set("font-size", "0.85em")
    //         .set("color", "#333")
    //         .set("margin-bottom", "0.5rem");
    //     rating.setText("⭐" + item.getRating());

    // }

    private void editItemDetails(ItemDTO item) {
        // 1) Dialog setup
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");
        
        // 2) Read-only rating
        Div ratingDiv = new Div();
        ratingDiv.getStyle()
            .set("font-size", "0.85em")
            .set("color", "#333")
            .set("margin-bottom", "0.5rem");
        ratingDiv.setText("⭐ " + item.getRating());

        // 3) Editable fields
        NumberField qtyField = new NumberField("Quantity");
        // qtyField.setValue((double)item.getQuantity());
        qtyField.setPlaceholder(String.valueOf(item.getQuantity()));
        qtyField.setMin(0);

        NumberField priceField = new NumberField("Price");
        priceField.setValue(item.getPrice());
        priceField.setMin(0);

        TextArea descArea = new TextArea("Description");
        descArea.setValue(item.getDescription());
        descArea.setWidthFull();

        // 4) Action buttons
        Button save = new Button("Save", e -> {
            int currentShopID = item.getShopId();
            int itemID = item.getItemID();
            int newQty = qtyField.getValue().intValue();
            double newPrice = priceField.getValue();
            String newDesc = descArea.getValue();

            //4a) update quantity
            presenter.changeItemQuantityInShop(currentShopID, itemID, newQty, qtyOk -> {
                if (!qtyOk) {
                    UI.getCurrent().access(() ->
                        Notification.show("Failed to update quantity", 2000, Position.MIDDLE)
                    );
                    return;
                }
                // 4b) update price
                presenter.changeItemPriceInShop(currentShopID, itemID, newPrice, priceOk -> {
                    if (!priceOk) {
                        UI.getCurrent().access(() ->
                            Notification.show("Failed to update price", 2000, Position.MIDDLE)
                        );
                        return;
                    }
                    // 4c) update description
                    presenter.changeItemDescriptionInShop(currentShopID, itemID, newDesc, descOk -> {
                        UI.getCurrent().access(() -> {
                            if (descOk) {
                                Notification.show("Item updated", 2000, Position.TOP_CENTER);
                                dlg.close();
                                loadItems(currentShopID);
                            } else {
                                Notification.show("Failed to update description", 2000, Position.MIDDLE);
                            }
                        });
                    });
                });
            });
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button("Delete", e1 -> {
            int currentShopID = item.getShopId();
            int itemID = item.getItemID();
            presenter.removeItemFromShop(currentShopID, itemID, delOk -> {
                UI.getCurrent().access(() -> {
                    if (delOk) {
                        Notification.show("Item deleted", 2000, Position.TOP_CENTER);
                        dlg.close();
                        loadItems(currentShopID);
                    } else {
                        Notification.show("Failed to delete item", 2000, Position.MIDDLE);
                    }
                });
            });
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button("Cancel", e2 -> dlg.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(save, delete, cancel);
        actions.setSpacing(true);

        // 5) Assemble dialog content
        VerticalLayout layout = new VerticalLayout(
            ratingDiv,
            qtyField,
            priceField,
            descArea,
            actions
        );
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        dlg.add(layout);
        dlg.open();

    }

}
