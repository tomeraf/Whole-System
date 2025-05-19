package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.frontend.application.presenters.PoliciesPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "shop-policies", layout = MainLayout.class)
@PageTitle("Shop Policies")
public class PoliciesView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final PoliciesPresenter presenter;
    private int shopId;

    private Button addConditionButton = new Button("Add Condition");
    private Button addDiscountButton  = new Button("Add Discount");

    private Dialog conditionDialog;

    // Fields for Condition 1
    private Select<String> itemSelect1       = new Select<>();
    private Select<Category> categorySelect1 = new Select<>();
    private Select<ConditionLimits> limitsSelect1 = new Select<>();
    private IntegerField minField1           = new IntegerField("Min");
    private IntegerField maxField1           = new IntegerField("Max");

    // Fields for Condition 2
    private Select<String> itemSelect2       = new Select<>();
    private Select<Category> categorySelect2 = new Select<>();
    private Select<ConditionLimits> limitsSelect2 = new Select<>();
    private IntegerField minField2           = new IntegerField("Min");
    private IntegerField maxField2           = new IntegerField("Max");

    // Action buttons
    private Button baseButton = new Button("Base");
    private Button xorButton  = new Button("XOR");
    private Button andButton  = new Button("AND");
    private Button orButton   = new Button("OR");

    @Autowired
    public PoliciesView(PoliciesPresenter presenter) {
        this.presenter = presenter;
        setSizeFull();

        HorizontalLayout header = new HorizontalLayout(addConditionButton, addDiscountButton);
        add(header);

        configureConditionDialog();

        addConditionButton.addClickListener(e -> conditionDialog.open());
        addDiscountButton.addClickListener(e -> {
            // Future: open discount dialog
        });
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.shopId = parameter;
    }

    private void configureConditionDialog() {
        // Populate selects
        itemSelect1.setLabel("Item");
        itemSelect2.setLabel("Item");
        presenter.getShopInfo(shopId, shop -> {
            List<String> items = shop.getItems().values().stream().map(dto -> dto.getName()).toList();
            itemSelect1.setItems(items);
            itemSelect2.setItems(items);
        });

        categorySelect1.setLabel("Category");
        categorySelect1.setItems(Category.values());
        categorySelect2.setLabel("Category");
        categorySelect2.setItems(Category.values());

        limitsSelect1.setLabel("Condition Limits");
        limitsSelect1.setItems(ConditionLimits.values());
        limitsSelect2.setLabel("Condition Limits");
        limitsSelect2.setItems(ConditionLimits.values());


        // Style buttons
        baseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        xorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        andButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        orButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Listeners to toggle Base
        Runnable update = this::updateConditionButtons;
        itemSelect1.addValueChangeListener(e -> update.run());
        categorySelect1.addValueChangeListener(e -> update.run());
        limitsSelect1.addValueChangeListener(e -> update.run());
        minField1.addValueChangeListener(e -> update.run());
        maxField1.addValueChangeListener(e -> update.run());

        // Click listeners to build DTO and call presenter
        baseButton.addClickListener(e -> submitCondition(ConditionType.BASE));
        xorButton.addClickListener(e -> submitCondition(ConditionType.XOR));
        andButton.addClickListener(e -> submitCondition(ConditionType.AND));
        orButton.addClickListener(e -> submitCondition(ConditionType.OR));

        VerticalLayout content = new VerticalLayout(
            new Label("Condition 1"),
            new HorizontalLayout(itemSelect1, categorySelect1),
            new HorizontalLayout(limitsSelect1),
            new HorizontalLayout(minField1, maxField1),
            new Label("Condition 2"),
            new HorizontalLayout(itemSelect2, categorySelect2),
            new HorizontalLayout(limitsSelect2),
            new HorizontalLayout(minField2, maxField2),
            new HorizontalLayout(baseButton, xorButton, andButton, orButton)
        );

        conditionDialog = new Dialog(content);
        conditionDialog.setWidth("500px");
    }

    private void updateConditionButtons() {
        boolean firstFilled = itemSelect1.getValue() != null
                           || categorySelect1.getValue() != null
                           || limitsSelect1.getValue() != null
                           || minField1.getValue() != null
                           || maxField1.getValue() != null;
        baseButton.setEnabled(firstFilled);
    }

     private void submitCondition(ConditionType type) {
        // Extract first condition values
        int itemId1 = -1;
        try { itemId1 = Integer.parseInt(itemSelect1.getValue()); } catch (Exception ignored) {}
        Category cat1 = categorySelect1.getValue();
        ConditionLimits lim1 = limitsSelect1.getValue();
        int min1 = minField1.getValue() != null ? minField1.getValue() : -1;
        int max1 = maxField1.getValue() != null ? maxField1.getValue() : -1;
        
        // Map to proper price/quantity fields
        int minPrice = lim1 == ConditionLimits.PRICE ? min1 : -1;
        int maxPrice = lim1 == ConditionLimits.PRICE ? max1 : -1;
        int minQty   = lim1 == ConditionLimits.QUANTITY ? min1 : -1;
        int maxQty   = lim1 == ConditionLimits.QUANTITY ? max1 : -1;
        
        // Extract second condition values
        int itemId2 = -1;
        try { itemId2 = Integer.parseInt(itemSelect2.getValue()); } catch (Exception ignored) {}
        Category cat2 = categorySelect2.getValue();
        ConditionLimits lim2 = limitsSelect2.getValue();
        int min2 = minField2.getValue() != null ? minField2.getValue() : -1;
        int max2 = maxField2.getValue() != null ? maxField2.getValue() : -1;
        
        // Map second to price/quantity
        int minPrice2 = lim2 == ConditionLimits.PRICE ? min2 : -1;
        int maxPrice2 = lim2 == ConditionLimits.PRICE ? max2 : -1;
        int minQty2   = lim2 == ConditionLimits.QUANTITY ? min2 : -1;
        int maxQty2   = lim2 == ConditionLimits.QUANTITY ? max2 : -1;
        
        // Build DTO
        ConditionDTO dto;
        if (type == ConditionType.BASE) {
            dto = new ConditionDTO(itemId1, cat1, lim1, minPrice, maxPrice, minQty, maxQty);
        } else {
            dto = new ConditionDTO(
                type,
                itemId1, cat1, lim1, minPrice, maxPrice, minQty, maxQty,
                lim2, itemId2, minPrice2, maxPrice2, minQty2, maxQty2,
                cat2
            );
        }
        
        // Send to presenter
        presenter.addPurchaseCondition(shopId, dto, resp -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                if (resp.isOk()) {
                    Notification.show("Condition added");
                    conditionDialog.close();
                } else {
                    Notification.show("Error: " + resp.getError());
                }
            }));
        });
    }
}
