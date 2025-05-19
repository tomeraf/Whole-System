package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.frontend.application.presenters.PoliciesPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "shop-policies", layout = MainLayout.class)
@PageTitle("Shop Policies")
public class PoliciesView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final PoliciesPresenter presenter;
    private int shopId;

    private Button addConditionButton = new Button("Add Condition");
    private Button addDiscountButton  = new Button("Add Discount");

    private Grid<ConditionDTO> conditionsGrid = new Grid<>(ConditionDTO.class, false);
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

    private Map<String,Integer> itemNameToId = new HashMap<>();

    @Autowired
    public PoliciesView(PoliciesPresenter presenter) {
        this.presenter = presenter;
        setSizeFull();

        // Header and grid
        HorizontalLayout header = new HorizontalLayout(addConditionButton, addDiscountButton);
        add(header);

        add(conditionsGrid);

        configureConditionDialog();

        addConditionButton.addClickListener(e -> conditionDialog.open());
        addDiscountButton.addClickListener(e -> {
            // Future: open discount dialog
        });
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.shopId = parameter;
        // Fetch shop once, then build the grid in order
        presenter.getShopInfo(shopId, shop -> {
            if (shop == null) return;
            // Build grid now that we have the items map
            configureConditionsGrid(shop);
            loadConditions();
        });
    }

    private void configureConditionsGrid(ShopDTO shop) {
        conditionsGrid.removeAllColumns();

        // 1) Type
        conditionsGrid.addColumn(ConditionDTO::getConditionType)
            .setHeader("Type")
            .setWidth("80px").setFlexGrow(0);

        // 2) Item 1
        conditionsGrid.addColumn(dto -> {
            if (dto.getItemId() >= 0) {
                itemNameToId.put(shop.getItems().get(dto.getItemId()).getName(), dto.getItemId());
                return shop.getItems().get(dto.getItemId()).getName();
            }
            return "";
        })
        .setHeader("Item 1")
        .setWidth("80px").setFlexGrow(0);

        // 3) Category 1
        conditionsGrid.addColumn(dto -> dto.getCategory() != null ? dto.getCategory().name() : "")
            .setHeader("Category 1")
            .setWidth("140px").setFlexGrow(0);

        // 4) Limits 1
        conditionsGrid.addColumn(dto -> dto.getConditionLimits() != null ? dto.getConditionLimits().name() : "")
            .setHeader("Limits 1")
            .setWidth("140px").setFlexGrow(0);

        // 5) Min/Max 1 (price vs quantity)
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits() == ConditionLimits.PRICE && dto.getMinPrice() >= 0) {
                return String.valueOf(dto.getMinPrice());
            } else if (dto.getConditionLimits() == ConditionLimits.QUANTITY && dto.getMinQuantity() >= 0) {
                return String.valueOf(dto.getMinQuantity());
            }
            return "";
        })
        .setHeader("Min 1")
        .setWidth("80px").setFlexGrow(0);

        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits() == ConditionLimits.PRICE && dto.getMaxPrice() >= 0) {
                return String.valueOf(dto.getMaxPrice());
            } else if (dto.getConditionLimits() == ConditionLimits.QUANTITY && dto.getMaxQuantity() >= 0) {
                return String.valueOf(dto.getMaxQuantity());
            }
            return "";
        })
        .setHeader("Max 1")
        .setWidth("80px").setFlexGrow(0);

        // 6) Item 2
        conditionsGrid.addColumn(dto -> {
            if (dto.getItemId2() >= 0) {
                itemNameToId.put(shop.getItems().get(dto.getItemId()).getName(), dto.getItemId());
                return shop.getItems().get(dto.getItemId2()).getName();
            }
            return "";
        })
        .setHeader("Item 2")
        .setWidth("80px").setFlexGrow(0);

        // 7) Category 2
        conditionsGrid.addColumn(dto -> dto.getCategory2() != null ? dto.getCategory2().name() : "")
            .setHeader("Category 2")
            .setWidth("140px").setFlexGrow(0);

        // 8) Limits 2
        conditionsGrid.addColumn(dto -> dto.getConditionLimits2() != null ? dto.getConditionLimits2().name() : "")
            .setHeader("Limits 2")
            .setWidth("140px").setFlexGrow(0);

        // 9) Min/Max 2
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits2() == ConditionLimits.PRICE && dto.getMinPrice2() >= 0) {
                return String.valueOf(dto.getMinPrice2());
            } else if (dto.getConditionLimits2() == ConditionLimits.QUANTITY && dto.getMinQuantity2() >= 0) {
                return String.valueOf(dto.getMinQuantity2());
            }
            return "";
        })
        .setHeader("Min 2")
        .setWidth("80px").setFlexGrow(0);

        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits2() == ConditionLimits.PRICE && dto.getMaxPrice2() >= 0) {
                return String.valueOf(dto.getMaxPrice2());
            } else if (dto.getConditionLimits2() == ConditionLimits.QUANTITY && dto.getMaxQuantity2() >= 0) {
                return String.valueOf(dto.getMaxQuantity2());
            }
            return "";
        })
        .setHeader("Max 2")
        .setWidth("80px").setFlexGrow(0);

        // 10) Actions
        conditionsGrid.addComponentColumn(dto -> {
            Button remove = new Button("Remove", ev -> submitRemove(dto));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return remove;
        })
        .setHeader("Actions")
        .setWidth("100px").setFlexGrow(0);
    }


    
    private void loadConditions() {
        presenter.getShopPurchaseConditions(shopId, list -> {
            getUI().ifPresent(ui -> ui.access(() -> conditionsGrid.setItems(list)));
        });
    }

    private void submitRemove(ConditionDTO dto) {
        // remove by index of dto in list, or if dto has ID, use that; here assume itemId as key
        presenter.removePurchaseCondition(shopId, dto.getId(), resp -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                if (resp.isOk()) {
                    Notification.show("Condition removed");
                    loadConditions();
                } else {
                    Notification.show("Error: " + resp.getError());
                }
            }));
        });
    }
    
    // existing configureConditionDialog, updateConditionButtons, submitCondition, openPlaceholderDialog ...
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
        // Fetch the shop so we can look up IDs by name
        presenter.getShopInfo(shopId, shop -> {
            if (shop == null) {
                Notification.show("Unable to resolve shop items", 2000, Position.MIDDLE);
                return;
            }

            // Lookup itemId1 by matching the selected name
            int itemId1 = shop.getItems().entrySet().stream()
                .filter(e -> e.getValue().getName().equals(itemSelect1.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);

            // Lookup itemId2 similarly
            int itemId2 = shop.getItems().entrySet().stream()
                .filter(e -> e.getValue().getName().equals(itemSelect2.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);

            // Gather the rest of the form
            Category cat1 = categorySelect1.getValue();
            ConditionLimits lim1 = limitsSelect1.getValue();
            int min1 = minField1.getValue() != null ? minField1.getValue() : -1;
            int max1 = maxField1.getValue() != null ? maxField1.getValue() : -1;
            ConditionLimits lim2 = limitsSelect2.getValue();
            int min2 = minField2.getValue() != null ? minField2.getValue() : -1;
            int max2 = maxField2.getValue() != null ? maxField2.getValue() : -1;
            Category cat2 = categorySelect2.getValue();

            // Map price vs quantity
            int minPrice  = lim1 == ConditionLimits.PRICE    ? min1 : -1;
            int maxPrice  = lim1 == ConditionLimits.PRICE    ? max1 : -1;
            int minQty    = lim1 == ConditionLimits.QUANTITY ? min1 : -1;
            int maxQty    = lim1 == ConditionLimits.QUANTITY ? max1 : -1;
            int minPrice2 = lim2 == ConditionLimits.PRICE    ? min2 : -1;
            int maxPrice2 = lim2 == ConditionLimits.PRICE    ? max2 : -1;
            int minQty2   = lim2 == ConditionLimits.QUANTITY ? min2 : -1;
            int maxQty2   = lim2 == ConditionLimits.QUANTITY ? max2 : -1;

            // Build the DTO
            ConditionDTO dto;
            if (type == ConditionType.BASE) {
                dto = new ConditionDTO(
                    itemId1, cat1, lim1,
                    minPrice, maxPrice,
                    minQty,   maxQty
                );
            } else {
                dto = new ConditionDTO(
                    type,
                    itemId1, cat1, lim1, minPrice,  maxPrice,  minQty,  maxQty,
                    lim2,    itemId2, minPrice2, maxPrice2, minQty2, maxQty2, cat2
                );
            }

            // Finally send it
            presenter.addPurchaseCondition(shopId, dto, resp -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (resp.isOk()) {
                        Notification.show("Condition added");
                        conditionDialog.close();
                        loadConditions();
                    } else {
                        Notification.show("Error: " + resp.getError());
                    }
                }));
            });
        });
    }

}
