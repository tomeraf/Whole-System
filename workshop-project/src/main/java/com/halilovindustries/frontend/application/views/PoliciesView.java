package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionLimits;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.ConditionType;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;
import com.halilovindustries.backend.Domain.Shop.Policies.Purchase.PurchaseType;
import com.halilovindustries.frontend.application.presenters.PoliciesPresenter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "shop-policies", layout = MainLayout.class)
@PageTitle("Shop Policies")
public class PoliciesView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final PoliciesPresenter presenter;
    private int shopId = -1;
    private boolean suppressPurchaseEvents = false;
    private boolean suppressDiscountEvents = false;

    // ─── EXISTING CONDITION FIELDS (UNCHANGED) ─────────────────────────────────

    private Button addConditionButton = new Button("Add Condition");
    private Grid<ConditionDTO> conditionsGrid = new Grid<>(ConditionDTO.class, false);
    private Dialog conditionDialog;

    private Select<String> itemSelect1 = new Select<>();
    private Select<Category> categorySelect1 = new Select<>();
    private Select<ConditionLimits> limitsSelect1 = new Select<>();
    private IntegerField minField1 = new IntegerField("Min");
    private IntegerField maxField1 = new IntegerField("Max");

    private Select<String> itemSelect2 = new Select<>();
    private Select<Category> categorySelect2 = new Select<>();
    private Select<ConditionLimits> limitsSelect2 = new Select<>();
    private IntegerField minField2 = new IntegerField("Min");
    private IntegerField maxField2 = new IntegerField("Max");

    private Button baseButton = new Button("Base");
    private Button xorButton = new Button("XOR");
    private Button andButton = new Button("AND");
    private Button orButton = new Button("OR");

    private Map<String, Integer> itemNameToId = new HashMap<>();

    // ─── NEW DISCOUNT FIELDS ───────────────────────────────────────────────────

    private Button addDiscountButton = new Button("Add Discount");
    private Grid<DiscountDTO> discountsGrid = new Grid<>(DiscountDTO.class, false);
    private Dialog discountDialog;

    // ─── DISCOUNT-CONDITION DIALOG #1 FIELDS ──────────────────────────
    private Select<String> dcItemSelect1 = new Select<>();
    private Select<Category> dcCategorySelect1 = new Select<>();
    private Select<ConditionLimits> dcLimitsSelect1 = new Select<>();
    private IntegerField dcMinField1 = new IntegerField("Min");
    private IntegerField dcMaxField1 = new IntegerField("Max");
    private Dialog discountConditionDialog1;

    // ─── DISCOUNT-CONDITION DIALOG #2 FIELDS ──────────────────────────
    private Select<String> dcItemSelect2 = new Select<>();
    private Select<Category> dcCategorySelect2 = new Select<>();
    private Select<ConditionLimits> dcLimitsSelect2 = new Select<>();
    private IntegerField dcMinField2 = new IntegerField("Min");
    private IntegerField dcMaxField2 = new IntegerField("Max");
    private Dialog discountConditionDialog2;

    private ConditionDTO selectedCondition1;
    private ConditionDTO selectedCondition2;

    // temporary holder & callback for re-using the same conditionDialog
    private ConditionDTO tempCondition;
    // private Dialog discountConditionDialog1;
    // private Dialog discountConditionDialog2;
    private Consumer<ConditionDTO> onConditionSaved;
    // fd
    private CheckboxGroup<PurchaseType> purchaseTypeGroup = new CheckboxGroup<>();
    private CheckboxGroup<DiscountType> discountTypeGroup = new CheckboxGroup<>();

    // ─── CONSTRUCTOR ────────────────────────────────────────────────────────────

    @Autowired
    public PoliciesView(PoliciesPresenter presenter) {
        this.presenter = presenter;
        setSizeFull();

        // header: both buttons
        HorizontalLayout header = new HorizontalLayout(addConditionButton, addDiscountButton);
        purchaseTypeGroup.setLabel("Purchase Policies");
        purchaseTypeGroup.setItems(PurchaseType.values());

        discountTypeGroup.setLabel("Discount Policies");
        discountTypeGroup.setItems(DiscountType.values());

        purchaseTypeGroup.addValueChangeListener(e -> {
            if (suppressPurchaseEvents) return;

            Set<PurchaseType> newValue = e.getValue();
            Set<PurchaseType> oldValue = e.getOldValue();

            for (PurchaseType type : newValue) {
                if (!oldValue.contains(type)) {
                    presenter.updatePurchaseType(shopId, type, resp -> {
                        // Optional notification
                    });
                }
            }
            for (PurchaseType type : oldValue) {
                if (!newValue.contains(type)) {
                    presenter.updatePurchaseType(shopId, type, resp -> {
                        // Optional notification
                    });
                }
            }
        });

        discountTypeGroup.addValueChangeListener(e -> {
            if (suppressDiscountEvents) return;

            Set<DiscountType> newValue = e.getValue();
            Set<DiscountType> oldValue = e.getOldValue();

            for (DiscountType type : newValue) {
                if (!oldValue.contains(type)) {
                    presenter.updateDiscountType(shopId, type, resp -> {});
                }
            }
            for (DiscountType type : oldValue) {
                if (!newValue.contains(type)) {
                    presenter.updateDiscountType(shopId, type, resp -> {});
                }
            }
        });


        HorizontalLayout policyControls = new HorizontalLayout(
                purchaseTypeGroup,
                discountTypeGroup);
        add(header, policyControls);

        // add grids
        add(conditionsGrid);
        add(discountsGrid);
        // =====================================shop ID1: " + shopId);

        // // configure both dialogs
        // configureConditionDialog(); // builds conditionDialog & wiring
        // configureDiscountDialog();
        //         =====================================shop ID2: " + shopId);

        // configureDiscountConditionDialog(
        //         true,
        //         dcItemSelect1, dcCategorySelect1, dcLimitsSelect1, dcMinField1, dcMaxField1,
        //         dlg -> discountConditionDialog1 = dlg);
        // configureDiscountConditionDialog(
        //         false,
        //         dcItemSelect2, dcCategorySelect2, dcLimitsSelect2, dcMinField2, dcMaxField2,
        //         dlg -> discountConditionDialog2 = dlg);

        // openers
        addConditionButton.addClickListener(e -> conditionDialog.open());
        addDiscountButton.addClickListener(e -> discountDialog.open());
    }

    // ─── ROUTER PARAMETER ────────────────────────────────────────────────────────

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.shopId = parameter;

        presenter.getShopInfo(shopId, shop -> {
            if (shop == null)
                return;

            presenter.getPurchaseTypes(shopId, types -> getUI().ifPresent(ui -> ui.access(() -> {
                suppressPurchaseEvents = true;
                purchaseTypeGroup.setValue(types == null ? Set.of() : Set.copyOf(types));
                suppressPurchaseEvents = false;
            })));

            presenter.getDiscountTypes(shopId, types -> getUI().ifPresent(ui -> ui.access(() -> {
                suppressDiscountEvents = true;
                discountTypeGroup.setValue(types == null ? Set.of() : Set.copyOf(types));
                suppressDiscountEvents = false;
            })));
            // build & load conditions
            configureConditionsGrid(shop);
            loadConditions();

            // build & load discounts
            configureDiscountsGrid(shop);
            loadDiscounts();

        // configure both dialogs
        configureConditionDialog(); // builds conditionDialog & wiring
        configureDiscountDialog();
        configureDiscountConditionDialog(
                true,
                dcItemSelect1, dcCategorySelect1, dcLimitsSelect1, dcMinField1, dcMaxField1,
                dlg -> discountConditionDialog1 = dlg);
        configureDiscountConditionDialog(
                false,
                dcItemSelect2, dcCategorySelect2, dcLimitsSelect2, dcMinField2, dcMaxField2,
                dlg -> discountConditionDialog2 = dlg);

        });
    }

    // ─── CONDITIONS GRID & CRUD (UNCHANGED) ────────────────────────────────────

    private void configureConditionsGrid(ShopDTO shop) {
        conditionsGrid.removeAllColumns();
        conditionsGrid.addColumn(ConditionDTO::getConditionType).setHeader("Type").setWidth("80px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> dto.getItemId() >= 0
                ? shop.getItems().get(dto.getItemId()).getName()
                : "")
                .setHeader("Item 1").setWidth("80px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> dto.getCategory() != null ? dto.getCategory().name() : "")
                .setHeader("Category 1").setWidth("140px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> dto.getConditionLimits() != null ? dto.getConditionLimits().name() : "")
                .setHeader("Limits 1").setWidth("140px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits() == ConditionLimits.PRICE && dto.getMinPrice() >= 0)
                return String.valueOf(dto.getMinPrice());
            if (dto.getConditionLimits() == ConditionLimits.QUANTITY && dto.getMinQuantity() >= 0)
                return String.valueOf(dto.getMinQuantity());
            return "";
        }).setHeader("Min 1").setWidth("80px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits() == ConditionLimits.PRICE && dto.getMaxPrice() >= 0)
                return String.valueOf(dto.getMaxPrice());
            if (dto.getConditionLimits() == ConditionLimits.QUANTITY && dto.getMaxQuantity() >= 0)
                return String.valueOf(dto.getMaxQuantity());
            return "";
        }).setHeader("Max 1").setWidth("80px").setFlexGrow(0);

        conditionsGrid.addColumn(dto -> dto.getItemId2() >= 0
                ? shop.getItems().get(dto.getItemId2()).getName()
                : "")
                .setHeader("Item 2").setWidth("80px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> dto.getCategory2() != null ? dto.getCategory2().name() : "")
                .setHeader("Category 2").setWidth("140px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> dto.getConditionLimits2() != null ? dto.getConditionLimits2().name() : "")
                .setHeader("Limits 2").setWidth("140px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits2() == ConditionLimits.PRICE && dto.getMinPrice2() >= 0)
                return String.valueOf(dto.getMinPrice2());
            if (dto.getConditionLimits2() == ConditionLimits.QUANTITY && dto.getMinQuantity2() >= 0)
                return String.valueOf(dto.getMinQuantity2());
            return "";
        }).setHeader("Min 2").setWidth("80px").setFlexGrow(0);
        conditionsGrid.addColumn(dto -> {
            if (dto.getConditionLimits2() == ConditionLimits.PRICE && dto.getMaxPrice2() >= 0)
                return String.valueOf(dto.getMaxPrice2());
            if (dto.getConditionLimits2() == ConditionLimits.QUANTITY && dto.getMaxQuantity2() >= 0)
                return String.valueOf(dto.getMaxQuantity2());
            return "";
        }).setHeader("Max 2").setWidth("80px").setFlexGrow(0);

        conditionsGrid.addComponentColumn(dto -> {
            Button rm = new Button("Remove", ev -> submitRemove(dto));
            rm.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return rm;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);
    }

    private void loadConditions() {
        presenter.getShopPurchaseConditions(shopId,
                list -> getUI().ifPresent(ui -> ui.access(() -> conditionsGrid.setItems(list))));
    }

    private void submitRemove(ConditionDTO dto) {
        presenter.removePurchaseCondition(shopId, dto.getId(), resp -> getUI().ifPresent(ui -> ui.access(() -> {
            if (resp.isOk()) {
                Notification.show("Condition removed");
                loadConditions();
            } else {
                Notification.show("Error: " + resp.getError());
            }
        })));
    }

    private void configureConditionDialog() {
        // === your existing logic entirely unchanged ===
        itemSelect1.setLabel("Item");
        itemSelect2.setLabel("Item");
        presenter.getShopInfo(shopId, shop -> {
            List<String> items = shop.getItems().values().stream()
                    .map(dto -> dto.getName()).toList();
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
        baseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        xorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        andButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        orButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Runnable update = this::updateConditionButtons;
        itemSelect1.addValueChangeListener(e -> update.run());
        categorySelect1.addValueChangeListener(e -> update.run());
        limitsSelect1.addValueChangeListener(e -> update.run());
        minField1.addValueChangeListener(e -> update.run());
        maxField1.addValueChangeListener(e -> update.run());
        baseButton.addClickListener(e -> submitCondition(ConditionType.BASE));
        xorButton.addClickListener(e -> submitCondition(ConditionType.XOR));
        andButton.addClickListener(e -> submitCondition(ConditionType.AND));
        orButton.addClickListener(e -> submitCondition(ConditionType.OR));
        baseButton.setEnabled(false);
        xorButton.setEnabled(false);
        andButton.setEnabled(false);
        orButton.setEnabled(false);
        itemSelect2.addValueChangeListener(e -> update.run());
        categorySelect2.addValueChangeListener(e -> update.run());
        limitsSelect2.addValueChangeListener(e -> update.run());
        minField2.addValueChangeListener(e -> update.run());
        maxField2.addValueChangeListener(e -> update.run());
        Runnable clearAllFields = () -> {
            // Clear selections
            itemSelect1.clear();
            itemSelect2.clear();
            categorySelect1.clear();
            categorySelect2.clear();
            limitsSelect1.clear();
            limitsSelect2.clear();
            minField1.clear();
            minField2.clear();
            maxField1.clear();
            maxField2.clear();
            
            // Reset item options to full list
            presenter.getShopInfo(shopId, shop -> {
                List<String> items = shop.getItems().values().stream()
                        .map(dto -> dto.getName()).toList();
                itemSelect1.setItems(items);
                itemSelect2.setItems(items);
            });
            
            // Reset other options to defaults
            categorySelect1.setItems(Category.values());
            categorySelect2.setItems(Category.values());
            limitsSelect1.setItems(ConditionLimits.values());
            limitsSelect2.setItems(ConditionLimits.values());
            
            // Update buttons state
            updateConditionButtons();
        };
        Button clearAllButton = createClearAllButton(clearAllFields);

        // Create a horizontal layout for the buttons
        HorizontalLayout actionButtons = new HorizontalLayout(baseButton, xorButton, andButton, orButton);
        actionButtons.setWidthFull();
        actionButtons.setJustifyContentMode(JustifyContentMode.START);

        // Create bottom layout with both button groups
        HorizontalLayout bottomLayout = new HorizontalLayout(actionButtons, clearAllButton);
        bottomLayout.setWidthFull();
        bottomLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout content = new VerticalLayout(
            new Label("Condition 1"),
            new HorizontalLayout(itemSelect1, categorySelect1),
            new HorizontalLayout(limitsSelect1),
            new HorizontalLayout(minField1, maxField1),
            new Label("Condition 2"),
            new HorizontalLayout(itemSelect2, categorySelect2),
            new HorizontalLayout(limitsSelect2),
            new HorizontalLayout(minField2, maxField2),
            bottomLayout);  // Add the combined layout
            conditionDialog = new Dialog(content);
            conditionDialog.setWidth("500px");

        // ── NEW: re-use hook ─────────────────────────────────────
        conditionDialog.addOpenedChangeListener(evt -> {
            if (!evt.isOpened() && onConditionSaved != null && tempCondition != null) {
                // empty the dialog
                itemSelect1.clear();
                itemSelect2.clear();
                categorySelect1.clear();
                categorySelect2.clear();
                limitsSelect1.clear();
                limitsSelect2.clear();
                minField1.clear();
                minField2.clear();
                maxField1.clear();
                maxField2.clear();

                onConditionSaved.accept(tempCondition);
                tempCondition = null;
                onConditionSaved = null;
            }
        });

    }

    private void updateConditionButtons() {
        boolean firstFilled = limitsSelect1.getValue() != null
                && (minField1.getValue() != null
                || maxField1.getValue() != null);

        boolean secondFilled = limitsSelect2.getValue() != null
                && (minField2.getValue() != null
                || maxField2.getValue() != null);

        // Disable all by default
        baseButton.setEnabled(false);
        xorButton.setEnabled(false);
        andButton.setEnabled(false);
        orButton.setEnabled(false);

        if (firstFilled) {
            baseButton.setEnabled(true);
            if (secondFilled) {
                xorButton.setEnabled(true);
                andButton.setEnabled(true);
                orButton.setEnabled(true);
            }
        }
    }

    private void submitCondition(ConditionType type) {
        presenter.getShopInfo(shopId, shop -> {
            if (shop == null) {
                Notification.show("Unable to resolve shop items", 2000, Position.MIDDLE);
                return;
            }
            int itemId1 = shop.getItems().entrySet().stream()
                    .filter(e -> e.getValue().getName().equals(itemSelect1.getValue()))
                    .map(Map.Entry::getKey).findFirst().orElse(-1);
            int itemId2 = shop.getItems().entrySet().stream()
                    .filter(e -> e.getValue().getName().equals(itemSelect2.getValue()))
                    .map(Map.Entry::getKey).findFirst().orElse(-1);
            Category cat1 = categorySelect1.getValue();
            ConditionLimits lim1 = limitsSelect1.getValue();
            int min1 = minField1.getValue() != null ? minField1.getValue() : -1;
            int max1 = maxField1.getValue() != null ? maxField1.getValue() : -1;
            ConditionLimits lim2 = limitsSelect2.getValue();
            int min2 = minField2.getValue() != null ? minField2.getValue() : -1;
            int max2 = maxField2.getValue() != null ? maxField2.getValue() : -1;
            Category cat2 = categorySelect2.getValue();

            int minPrice = lim1 == ConditionLimits.PRICE ? min1 : -1;
            int maxPrice = lim1 == ConditionLimits.PRICE ? max1 : -1;
            int minQty = lim1 == ConditionLimits.QUANTITY ? min1 : -1;
            int maxQty = lim1 == ConditionLimits.QUANTITY ? max1 : -1;
            int minPrice2 = lim2 == ConditionLimits.PRICE ? min2 : -1;
            int maxPrice2 = lim2 == ConditionLimits.PRICE ? max2 : -1;
            int minQty2 = lim2 == ConditionLimits.QUANTITY ? min2 : -1;
            int maxQty2 = lim2 == ConditionLimits.QUANTITY ? max2 : -1;

            ConditionDTO dto;
            if (type == ConditionType.BASE) {
                dto = new ConditionDTO(itemId1, cat1, lim1, minPrice, maxPrice, minQty, maxQty);
            } else {
                dto = new ConditionDTO(type,
                        itemId1, cat1, lim1, minPrice, maxPrice, minQty, maxQty,
                        lim2, itemId2, minPrice2, maxPrice2, minQty2, maxQty2, cat2);
            }
            // stash for discount re-use
            tempCondition = dto;

            presenter.addPurchaseCondition(shopId, dto, resp -> getUI().ifPresent(ui -> ui.access(() -> {
                if (resp.isOk()) {
                    Notification.show("Condition added");
                    // close the dialog
                    itemSelect1.clear();
                    itemSelect2.clear();
                    categorySelect1.clear();
                    categorySelect2.clear();
                    limitsSelect1.clear();
                    limitsSelect2.clear();
                    minField1.clear();
                    minField2.clear();
                    maxField1.clear();
                    maxField2.clear();

                    conditionDialog.close();
                    loadConditions();
                    tempCondition = null;
                } else {
                    Notification.show("Error: " + resp.getError());
                }
            })));
        });
    }

    // ─── DISCOUNTS GRID ─────────────────────────────────────────────────────────

    private void configureDiscountsGrid(ShopDTO shop) {
        discountsGrid.removeAllColumns();

        // Kind
        discountsGrid.addColumn(DiscountDTO::getDiscountKind)
                .setHeader("Kind")
                .setWidth("100px").setFlexGrow(0);

        // Applies To 1
        discountsGrid.addColumn(dto -> dto.getItemId() >= 0
                ? shop.getItems().get(dto.getItemId()).getName()
                : dto.getCategory() != null
                        ? dto.getCategory().name()
                        : "–")
                .setHeader("Applies To 1")
                .setWidth("140px").setFlexGrow(0);

        // % 1
        discountsGrid.addColumn(DiscountDTO::getPercentage)
                .setHeader("% 1")
                .setWidth("80px").setFlexGrow(0);

        // ─── Button for Condition 1 ──────────────────────────────────────────────
        discountsGrid.addComponentColumn(dto -> {
            Button btn = new Button("View Cond 1", evt -> showConditionDialog(dto.getCondition()));
            return btn;
        })
                .setHeader("Condition 1")
                .setWidth("120px").setFlexGrow(0);

        // Applies To 2
        discountsGrid.addColumn(dto -> dto.getItemId2() >= 0
                ? shop.getItems().get(dto.getItemId2()).getName()
                : dto.getCategory2() != null
                        ? dto.getCategory2().name()
                        : "–")
                .setHeader("Applies To 2")
                .setWidth("140px").setFlexGrow(0);

        // % 2
        discountsGrid.addColumn(DiscountDTO::getPercentage2)
                .setHeader("% 2")
                .setWidth("80px").setFlexGrow(0);

        // ─── Button for Condition 2 ──────────────────────────────────────────────
        discountsGrid.addComponentColumn(dto -> {
            Button btn = new Button("View Cond 2", evt -> showConditionDialog(dto.getCondition2()));
            return btn;
        })
                .setHeader("Condition 2")
                .setWidth("120px").setFlexGrow(0);

        // Actions
        discountsGrid.addComponentColumn(dto -> {
            Button rm = new Button("Remove", ev -> submitRemoveDiscount(dto));
            rm.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return rm;
        })
                .setHeader("Actions")
                .setFlexGrow(0);
    }

    private void showConditionDialog(ConditionDTO c) {
        Dialog dlg = new Dialog();
        dlg.setWidth("300px");

        if (c == null) {
            dlg.add(new Span("No condition set"));
        } else {
            VerticalLayout content = new VerticalLayout();
            content.add(new Span("Type:   " + c.getConditionType()));
            content.add(new Span("Limits: " + c.getConditionLimits()));
            content.add(new Span("Min:    " +
                    (c.getConditionLimits() == ConditionLimits.PRICE
                            ? c.getMinPrice()
                            : c.getMinQuantity())));
            content.add(new Span("Max:    " +
                    (c.getConditionLimits() == ConditionLimits.PRICE
                            ? c.getMaxPrice()
                            : c.getMaxQuantity())));
            dlg.add(content);
        }

        dlg.open();
    }

    private void loadDiscounts() {
        presenter.getShopDiscounts(shopId,
                list -> getUI().ifPresent(ui -> ui.access(() -> discountsGrid.setItems(list))));
    }

    private void submitRemoveDiscount(DiscountDTO dto) {
        presenter.removeDiscount(shopId, dto.getId(), resp -> getUI().ifPresent(ui -> ui.access(() -> {
            if (resp.isOk()) {
                Notification.show("Discount removed");
                loadDiscounts();
            } else {
                Notification.show("Error: " + resp.getError());
            }
        })));
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    /**
     * Builds one of the two discount-condition dialogs.
     *
     * @param first      whether this is condition1 (true) or condition2 (false)
     * @param itemSel    the item-select component
     * @param catSel     the category-select component
     * @param limSel     the limits-select component
     * @param minField   the Min integer-field
     * @param maxField   the Max integer-field
     * @param dialogSink receives the built Dialog so we can store it
     */
    private void configureDiscountConditionDialog(
            boolean first,
            Select<String> itemSel1,
            Select<Category> categorySel1,
            Select<ConditionLimits> limitsSel1,
            IntegerField minField1,
            IntegerField maxField1,
            Consumer<Dialog> dialogSink) {
        // Fields for condition 2 (symmetric to condition 1)
        Select<String> itemSel2 = new Select<>();
        Select<Category> categorySel2 = new Select<>();
        Select<ConditionLimits> limitsSel2 = new Select<>();
        IntegerField minField2 = new IntegerField("Min");
        IntegerField maxField2 = new IntegerField("Max");

        // Labeling
        itemSel1.setLabel("Item");
        itemSel2.setLabel("Item");
        categorySel1.setLabel("Category");
        categorySel2.setLabel("Category");
        limitsSel1.setLabel("Condition Limits");
        limitsSel2.setLabel("Condition Limits");

        // Populate shop items
        presenter.getShopInfo(shopId, shop -> {
            List<String> items = shop.getItems().values().stream().map(i -> i.getName()).toList();
            itemSel1.setItems(items);
            itemSel2.setItems(items);
        });

        categorySel1.setItems(Category.values());
        categorySel2.setItems(Category.values());
        limitsSel1.setItems(ConditionLimits.values());
        limitsSel2.setItems(ConditionLimits.values());

        // Buttons
        Button base = new Button("Base");
        Button xor = new Button("XOR");
        Button and = new Button("AND");
        Button or = new Button("OR");
        Stream.of(base, xor, and, or).forEach(b -> b.addThemeVariants(ButtonVariant.LUMO_PRIMARY));

        // Submit handler
        Consumer<ConditionType> submit = type -> {
            presenter.getShopInfo(shopId, shop -> {
                int itemId1 = shop.getItems().entrySet().stream()
                        .filter(e -> e.getValue().getName().equals(itemSel1.getValue()))
                        .map(Map.Entry::getKey).findFirst().orElse(-1);
                int itemId2 = shop.getItems().entrySet().stream()
                        .filter(e -> e.getValue().getName().equals(itemSel2.getValue()))
                        .map(Map.Entry::getKey).findFirst().orElse(-1);

                Category cat1 = categorySel1.getValue();
                Category cat2 = categorySel2.getValue();
                ConditionLimits lim1 = limitsSel1.getValue();
                ConditionLimits lim2 = limitsSel2.getValue();

                int min1 = minField1.getValue() != null ? minField1.getValue() : -1;
                int max1 = maxField1.getValue() != null ? maxField1.getValue() : -1;
                int min2 = minField2.getValue() != null ? minField2.getValue() : -1;
                int max2 = maxField2.getValue() != null ? maxField2.getValue() : -1;

                int minPrice1 = lim1 == ConditionLimits.PRICE ? min1 : -1;
                int maxPrice1 = lim1 == ConditionLimits.PRICE ? max1 : -1;
                int minQty1 = lim1 == ConditionLimits.QUANTITY ? min1 : -1;
                int maxQty1 = lim1 == ConditionLimits.QUANTITY ? max1 : -1;

                int minPrice2 = lim2 == ConditionLimits.PRICE ? min2 : -1;
                int maxPrice2 = lim2 == ConditionLimits.PRICE ? max2 : -1;
                int minQty2 = lim2 == ConditionLimits.QUANTITY ? min2 : -1;
                int maxQty2 = lim2 == ConditionLimits.QUANTITY ? max2 : -1;

                ConditionDTO dto = (type == ConditionType.BASE)
                        ? new ConditionDTO(itemId1, cat1, lim1, minPrice1, maxPrice1, minQty1, maxQty1)
                        : new ConditionDTO(type,
                                itemId1, cat1, lim1, minPrice1, maxPrice1, minQty1, maxQty1,
                                lim2, itemId2, minPrice2, maxPrice2, minQty2, maxQty2, cat2);

                // clear all fields and vaadin buttons:
                // itemSel1.clear();
                // categorySel1.clear();
                // limitsSel1.clear();
                // minField1.clear();
                // maxField1.clear();
                // itemSel2.clear();
                // categorySel2.clear();
                // limitsSel2.clear();
                // minField2.clear();
                // maxField2.clear();
                // base.setEnabled(false);
                // xor.setEnabled(false);
                // and.setEnabled(false);
                // or.setEnabled(false);
                tempCondition = dto;
                (first ? discountConditionDialog1 : discountConditionDialog2).close();
            });
        };

        base.addClickListener(e -> submit.accept(ConditionType.BASE));
        xor.addClickListener(e -> submit.accept(ConditionType.XOR));
        and.addClickListener(e -> submit.accept(ConditionType.AND));
        or.addClickListener(e -> submit.accept(ConditionType.OR));
        Stream.of(base, xor, and, or).forEach(b -> b.setEnabled(false));
        // Stream.of(itemSelect2, categorySelect2, limitsSelect2, minField2,
        // maxField2).forEach(b -> b.addValueChangeListener(e -> update.run()));
        // Enable BASE only if something is filled
        Runnable updateButtons = () -> {
            boolean firstFilled = limitsSel1.getValue() != null
                    && (minField1.getValue() != null
                    || maxField1.getValue() != null);

            boolean secondFilled = limitsSel2.getValue() != null
                    || (minField2.getValue() != null
                    && maxField2.getValue() != null);

            base.setEnabled(false);
            xor.setEnabled(false);
            and.setEnabled(false);
            or.setEnabled(false);

            if (firstFilled) {
                base.setEnabled(true);
                if (secondFilled) {
                    xor.setEnabled(true);
                    and.setEnabled(true);
                    or.setEnabled(true);
                }
            }
        };
        // Stream.of(limitsSel1, minField1, maxField1, limitsSel2, minField2,
        // maxField2).forEach(c -> c.addValueChangeListener(e -> updateButtons.run()));

        limitsSel1.addValueChangeListener(e -> updateButtons.run());
        limitsSel2.addValueChangeListener(e -> updateButtons.run());

        minField1.addValueChangeListener(e -> updateButtons.run());
        maxField1.addValueChangeListener(e -> updateButtons.run());
        minField2.addValueChangeListener(e -> updateButtons.run());
        maxField2.addValueChangeListener(e -> updateButtons.run());

         Runnable clearAllFields = () -> {
            // Clear selections
            itemSel1.clear();
            categorySel1.clear();
            limitsSel1.clear();
            minField1.clear();
            maxField1.clear();
            itemSel2.clear();
            categorySel2.clear();
            limitsSel2.clear();
            minField2.clear();
            maxField2.clear();
            
            // Reset item options to full list
            presenter.getShopInfo(shopId, shop -> {
                List<String> items = shop.getItems().values().stream()
                        .map(dto -> dto.getName()).toList();
                itemSel1.setItems(items);
                itemSel2.setItems(items);
            });
            
            // Reset other options to defaults
            categorySel1.setItems(Category.values());
            categorySel2.setItems(Category.values());
            limitsSel1.setItems(ConditionLimits.values());
            limitsSel2.setItems(ConditionLimits.values());
            
            // Update button state
            updateButtons.run();
        };
        Button clearAllButton = createClearAllButton(clearAllFields);
        // Build dialog layout — SAME AS `configureConditionDialog`
        VerticalLayout content = new VerticalLayout(
            new Label("Condition 1"),
            new HorizontalLayout(itemSel1, categorySel1),
            new HorizontalLayout(limitsSel1),
            new HorizontalLayout(minField1, maxField1),
            new Label("Condition 2"),
            new HorizontalLayout(itemSel2, categorySel2),
            new HorizontalLayout(limitsSel2),
            new HorizontalLayout(minField2, maxField2),
            new HorizontalLayout(base, xor, and, or),
            clearAllButton);
        Dialog dlg = new Dialog(content);
        dlg.setWidth("500px");

        dlg.addOpenedChangeListener(evt -> {
            if (!evt.isOpened()) {
                if (tempCondition != null && onConditionSaved != null) {
                    onConditionSaved.accept(tempCondition);
                }
                // always reset both to avoid lingering data
                tempCondition = null;
                onConditionSaved = null;
            }
        });

        Runnable resetFields = () -> {
            itemSel1.clear();
            categorySel1.clear();
            limitsSel1.clear();
            minField1.clear();
            maxField1.clear();

            itemSel2.clear();
            categorySel2.clear();
            limitsSel2.clear();
            minField2.clear();
            maxField2.clear();

            tempCondition = null;
            onConditionSaved = null;
        };
        // dlg.addOpenedChangeListener(evt -> {
        // if (!evt.isOpened() && onConditionSaved != null && tempCondition != null) {
        // onConditionSaved.accept(tempCondition);
        // tempCondition = null;
        // onConditionSaved = null;
        // }
        // });

        dialogSink.accept(dlg);
    }

    private void configureDiscountDialog() {
        discountDialog = new Dialog();
        discountDialog.setWidth("1000px");
        Select<String> discountItemSelect1 = new Select<>();
        Select<String> discountItemSelect2 = new Select<>();

        presenter.getShopInfo(shopId, shop -> {
            
            List<String> names = shop.getItems().values().stream().map(i -> i.getName()).toList();
            discountItemSelect1.setLabel("Item 1");
            discountItemSelect2.setLabel("Item 2");
            discountItemSelect1.setItems(names);
            discountItemSelect2.setItems(names);
        }); 
        // Create UI elements
        Select<DiscountKind> discountKindSelect = new Select<>();
        discountKindSelect.setLabel("Discount Kind");
        discountKindSelect.setItems(DiscountKind.values());

        Select<DiscountType> discountType1Select = new Select<>();
        discountType1Select.setLabel("Type 1");
        discountType1Select.setItems(DiscountType.values());

        Select<DiscountType> discountType2Select = new Select<>();
        discountType2Select.setLabel("Type 2");
        discountType2Select.setItems(DiscountType.values());

        Select<Category> discountCategorySelect1 = new Select<>();
        discountCategorySelect1.setLabel("Category 1");
        discountCategorySelect1.setItems(Category.values());

        Select<Category> discountCategorySelect2 = new Select<>();
        discountCategorySelect2.setLabel("Category 2");
        discountCategorySelect2.setItems(Category.values());

        IntegerField percentage1Field = new IntegerField("Percentage 1");
        IntegerField percentage2Field = new IntegerField("Percentage 2");

        Button cond1Button = new Button("Add Condition 1");
        cond1Button.setVisible(false);
        discountType1Select.addValueChangeListener(e -> {
            if (discountType1Select.getValue() == DiscountType.BASE) {
                cond1Button.setVisible(false);
            } else {
                cond1Button.setVisible(true);
            }
        });
        Button cond2Button = new Button("Add Condition 2");
        cond2Button.setVisible(false);
        discountType2Select.addValueChangeListener(e -> {
            if (discountType2Select.getValue() == DiscountType.BASE) {
                cond2Button.setVisible(false);
            } else {
                cond2Button.setVisible(true);
            }
        });
        Button saveButton = new Button("Save Discount", e -> {
            presenter.getShopInfo(shopId, shop -> {
                int itemId1 = shop.getItems().entrySet().stream()
                        .filter(e1 -> e1.getValue().getName().equals(discountItemSelect1.getValue()))
                        .map(Map.Entry::getKey).findFirst().orElse(-1);
                int itemId2 = shop.getItems().entrySet().stream()
                        .filter(e2 -> e2.getValue().getName().equals(discountItemSelect2.getValue()))
                        .map(Map.Entry::getKey).findFirst().orElse(-1);

                DiscountDTO dto = new DiscountDTO(
                        discountKindSelect.getValue(),
                        itemId1,
                        discountCategorySelect1.getValue(),
                        percentage1Field.getValue() != null ? percentage1Field.getValue() : -1,
                        selectedCondition1,
                        itemId2,
                        discountCategorySelect2.getValue(),
                        percentage2Field.getValue() != null ? percentage2Field.getValue() : -1,
                        selectedCondition2,
                        discountType1Select.getValue(),
                        discountType2Select.getValue());

                presenter.addDiscount(shopId, dto, resp -> getUI().ifPresent(ui -> ui.access(() -> {
                    if (resp.isOk()) {
                        Notification.show("Discount added");
                        discountKindSelect.clear();
                        discountType1Select.clear();
                        discountType2Select.clear();
                        discountItemSelect1.clear();
                        discountItemSelect2.clear();
                        discountCategorySelect1.clear();
                        discountCategorySelect2.clear();
                        percentage1Field.clear();
                        percentage2Field.clear();
                        cond1Button.setEnabled(false);
                        cond2Button.setEnabled(false);
                        ;
                        discountDialog.close();
                        loadDiscounts();
                    } else {
                        Notification.show("Error: " + resp.getError());
                    }
                })));
            });
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

         Runnable clearAllFields = () -> {
            discountKindSelect.clear();
            discountType1Select.clear();
            discountType2Select.clear();
            discountItemSelect1.clear();
            discountItemSelect2.clear();
            discountCategorySelect1.clear();
            discountCategorySelect2.clear();
            percentage1Field.clear();
            percentage2Field.clear();
            selectedCondition1 = null;
            selectedCondition2 = null;
            
            // Reset options to defaults
            discountKindSelect.setItems(DiscountKind.values());
            discountType1Select.setItems(DiscountType.values());
            discountType2Select.setItems(DiscountType.values());
            discountCategorySelect1.setItems(Category.values());
            discountCategorySelect2.setItems(Category.values());
            
            // Reset item options to full list
            presenter.getShopInfo(shopId, shop -> {
                List<String> names = shop.getItems().values().stream()
                        .map(i -> i.getName()).toList();
                discountItemSelect1.setItems(names);
                discountItemSelect2.setItems(names);
            });
            
            // Reset visibility and enabled states
            cond1Button.setVisible(false);
            cond2Button.setVisible(false);
            discountItemSelect2.setEnabled(false);
            discountCategorySelect2.setEnabled(false);
            discountType2Select.setEnabled(false);
            percentage2Field.setEnabled(false);
            cond2Button.setEnabled(false);
        };
        Button clearAllButton = createClearAllButton(clearAllFields);

        // Set up dialog layout
        VerticalLayout content = new VerticalLayout(
            discountKindSelect,
            new HorizontalLayout(discountType1Select, discountItemSelect1, discountCategorySelect1,
            percentage1Field),
            new HorizontalLayout(cond1Button),
            new HorizontalLayout(discountType2Select, discountItemSelect2, discountCategorySelect2,
            percentage2Field),
            new HorizontalLayout(cond2Button),
            saveButton,
            clearAllButton);

        discountDialog.add(content);

        Runnable updateDiscountKindUI = () -> {
            //List<String> names = shop.getItems().values().stream().map(i -> i.getName()).toList();
            boolean isCombined = DiscountKind.COMBINE.equals(discountKindSelect.getValue());
            boolean isMax = DiscountKind.MAX.equals(discountKindSelect.getValue());
            if (discountKindSelect.getValue() == DiscountKind.BASE)
                discountType1Select.setItems(DiscountType.BASE);
            if (discountKindSelect.getValue() == DiscountKind.CONDITIONAL)
                discountType1Select.setItems(DiscountType.CONDITIONAL);
            discountItemSelect2.setEnabled(isCombined || isMax);
            discountCategorySelect2.setEnabled(isCombined || isMax);
            discountType2Select.setEnabled(isCombined || isMax);
            percentage2Field.setEnabled(isCombined || isMax);
            cond2Button.setEnabled(isCombined || isMax);
        };
        discountKindSelect.addValueChangeListener(e -> updateDiscountKindUI.run());
        updateDiscountKindUI.run();

        // Fill item names after shop is loaded
        presenter.getShopInfo(shopId, shop -> {
            List<String> names = shop.getItems().values().stream().map(i -> i.getName()).toList();
            discountItemSelect1.setItems(names);
            discountItemSelect2.setItems(names);
        });

        // Hook up condition buttons
        cond1Button.addClickListener(e -> {
            selectedCondition1 = null; // reset before dialog opens
            onConditionSaved = dto -> selectedCondition1 = dto;
            discountConditionDialog1.open();
        });
        cond2Button.addClickListener(e -> {
            selectedCondition2 = null; // reset before dialog opens

            onConditionSaved = dto -> selectedCondition2 = dto;
            discountConditionDialog2.open();
        });
    }
    private Button createClearAllButton(Runnable clearAction) {
        Button clearAll = new Button("Clear All");
        clearAll.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        clearAll.getStyle().set("margin-top", "15px");
        clearAll.addClickListener(e -> clearAction.run());
        return clearAll;
    }

}
