package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.frontend.application.presenters.BidPresenter;
import com.halilovindustries.frontend.application.presenters.MyShopPresenter;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Route(value = "shop-supply/", layout = MainLayout.class)
@PageTitle("Shop Supply")
public class SupplyView extends Composite<VerticalLayout> implements HasUrlParameter<Integer> {

    private final MyShopPresenter presenter;
    private final BidPresenter bidPresenter;
    private final FlexLayout itemsLayout = new FlexLayout();
    private final FlexLayout activeAuctionsLayout = new FlexLayout();
    private final FlexLayout futureAuctionsLayout = new FlexLayout();
    private final FlexLayout bidsLayout = new FlexLayout();

    private Button back = new Button("← Back");
    private final H3 shopTitle = new H3();
    private Button addItemButton;
    private Button createAuctionButton;

    @Autowired
    public SupplyView(MyShopPresenter presenter, BidPresenter bidPresenter) {
        this.presenter = presenter;
        this.bidPresenter = bidPresenter;

        addItemButton = new Button("Add Item", VaadinIcon.PLUS.create());
        addItemButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        createAuctionButton = new Button("Create Auction", VaadinIcon.PLUS.create());
        createAuctionButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(shopTitle, back, addItemButton);
        header.setWidthFull();
        header.expand(shopTitle);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().add(header);

        // ─── ITEMS LAYOUT ──────────────────────────────────────────────────────────
        itemsLayout.setWidthFull();
        itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        itemsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        itemsLayout.getStyle().set("gap", "1rem");
        getContent().add(itemsLayout);
        // ───────────────────────────────────────────────────────────────────────────

        // ─── ONGOING AUCTIONS ────────────────────────────────────────────────────
        getContent().add(new H3("Ongoing Auctions"));
        activeAuctionsLayout.setWidthFull();
        activeAuctionsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        activeAuctionsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        activeAuctionsLayout.getStyle().set("gap", "1rem");
        getContent().add(activeAuctionsLayout);
        // ───────────────────────────────────────────────────────────────────────────

        // ─── FUTURE AUCTIONS ──────────────────────────────────────────────────────
        getContent().add(new H3("Future Auctions"));
        futureAuctionsLayout.setWidthFull();
        futureAuctionsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        futureAuctionsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        futureAuctionsLayout.getStyle().set("gap", "1rem");
        getContent().add(futureAuctionsLayout);
        // ───────────────────────────────────────────────────────────────────────────

        getContent().add(new H3("Bids"));
        bidsLayout.setWidthFull();
        bidsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        bidsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        bidsLayout.getStyle().set("gap", "1rem");
        getContent().add(bidsLayout);
    }

    /**
     * Vaadin will call this *after* matching “shop-supply/{someInt}”
     */
    @Override
    public void setParameter(BeforeEvent event, Integer shopID) {
        loadItems(shopID);

        addItemButton.addClickListener(e -> openAddItemDialog(shopID));
        back.addClickListener(e -> UI.getCurrent().navigate("manage-shop/" + shopID));

        createAuctionButton.addClickListener(e -> {
            Dialog auctionDialog = new Dialog();
            auctionDialog.setHeaderTitle("Create New Auction");

            ComboBox<ItemDTO> itemComboBox = new ComboBox<>("Select Item");
            presenter.getShopInfo(shopID, shop -> {
                UI.getCurrent().access(() -> {
                    itemComboBox.setItems(shop.getItems().values());
                    itemComboBox.setItemLabelGenerator(ItemDTO::getName);
                });
            });

            NumberField startingPriceField = new NumberField("Starting Price");
            startingPriceField.setMin(0.01);
            startingPriceField.setStep(0.1);
            startingPriceField.setValue(10.0);

            DateTimePicker startDateField = new DateTimePicker("Start Date & Time");
            startDateField.setValue(LocalDateTime.now());

            DateTimePicker endDateField = new DateTimePicker("End Date & Time");
            endDateField.setValue(LocalDateTime.now().plusHours(24));

            VerticalLayout layout = new VerticalLayout(
                itemComboBox, startingPriceField, startDateField, endDateField
            );

            Button createBtn = new Button("Create", ev -> {
                ItemDTO selectedItem = itemComboBox.getValue();
                Double price = startingPriceField.getValue();
                LocalDateTime start = startDateField.getValue();
                LocalDateTime end = endDateField.getValue();

                presenter.createAuction(shopID, selectedItem.getItemID(), price, start, end, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            Notification.show("Auction created", 2000, Position.TOP_CENTER);
                            auctionDialog.close();
                            loadItems(shopID);
                        } else {
                            Notification.show("Failed to create auction", 2000, Position.MIDDLE);
                        }
                    });
                });
            });

            Button cancelBtn = new Button("Cancel", ev -> auctionDialog.close());
            HorizontalLayout buttons = new HorizontalLayout(createBtn, cancelBtn);
            layout.add(buttons);

            auctionDialog.add(layout);
            auctionDialog.open();
        });
    }

    /**
     * Loads all items, auctions, and (NEW) bids for this shop.
     */
    private void loadItems(int shopID) {
        itemsLayout.removeAll();
        activeAuctionsLayout.removeAll();
        futureAuctionsLayout.removeAll();
        bidsLayout.removeAll(); // <— clear previous bids

        presenter.getShopInfo(shopID, shop -> {
            UI.getCurrent().access(() -> {
                if (shop == null) {
                    Notification.show("Failed to load shop", 2000, Position.MIDDLE);
                    return;
                }
                // ─── Update header ───────────────────────────────────────────────────
                shopTitle.setText(shop.getName());

                // ─── Render each ITEM card ────────────────────────────────────────────
                shop.getItems().values()
                    .forEach(item -> itemsLayout.add(createItemCard(item)));

                // ─── Render ongoing auctions ───────────────────────────────────────────
                presenter.getActiveAuctions(shopID, (List<AuctionDTO> auctions) -> {
                    for (AuctionDTO auction : auctions) {
                        createAuctionCard(shopID, auction, card -> {
                            UI.getCurrent().access(() -> activeAuctionsLayout.add(card));
                        });
                    }

                    // ─── Render future auctions ────────────────────────────────────────
                    presenter.getFutureAuctions(shopID, (List<AuctionDTO> futureAuctions) -> {
                        for (AuctionDTO auction : futureAuctions) {
                            createAuctionCard(shopID, auction, card -> {
                                UI.getCurrent().access(() -> futureAuctionsLayout.add(card));
                            });
                        }

                        presenter.getBids(shopID, (List<BidDTO> bids) -> {
                            UI.getCurrent().access(() -> {
                                bidsLayout.removeAll();
                                for (BidDTO bid : bids) {
                                    bidsLayout.add(createBidCard(bid, shop));
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    /**
     * Opens a dialog to add a new item to the shop.
     */
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

        Button save = new Button("Add", e -> {
            String itemName = name.getValue();
            String catValue = category.getValue();
            Double itemPrice = price.getValue();
            String itemDesc = desc.getValue();

            if (itemName.isEmpty() || catValue == null || itemPrice == null) {
                Notification.show("Please fill in all required fields", 2000, Position.MIDDLE);
                return;
            }

            presenter.addItemToShop(
                shopID,
                itemName,
                Category.valueOf(catValue),
                itemPrice,
                itemDesc,
                newItem -> {
                    UI.getCurrent().access(() -> {
                        if (newItem == null) {
                            Notification.show("Failed to add item", 2000, Position.MIDDLE);
                        } else {
                            Notification.show("Item added successfully!", 2000, Position.MIDDLE);
                            loadItems(shopID);
                            dlg.close();
                        }
                    });
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

    /**
     * Creates a simple card for each item (same as before).
     */
    private VerticalLayout createItemCard(ItemDTO item) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");

        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "1.9em");
        name.getStyle().set("font-weight", "bold");

        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");

        Span rating = new Span("⭐ " + item.getRating());
        rating.getStyle().set("font-size", "0.85em");
        rating.getStyle().set("color", "#333");

        card.addClickListener(e -> editItemDetails(item));
        card.add(name, price, rating);
        return card;
    }

    /**
     * Creates a small auction card (same as before).
     */
    private void createAuctionCard(int shopID, AuctionDTO auction, Consumer<VerticalLayout> onCardReady) {
        presenter.getShopInfo(shopID, shop -> {
            ItemDTO item = shop.getItems().get(auction.getItemId());
            VerticalLayout card = new VerticalLayout();
            card.setAlignItems(FlexComponent.Alignment.CENTER);
            card.getStyle()
                .set("border", "2px dashed #555")
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("width", "200px")
                .set("background-color", "#fef6e4");

            Span name = new Span(item.getName());
            name.getStyle().set("font-size", "1.5em").set("font-weight", "bold");

            Span currentPrice = new Span("Current: $" + auction.getHighestBid());
            currentPrice.getStyle().set("color", "#d35400");

            Span startsAt = new Span("Starts: " + auction.getAuctionStartTime().toString());
            startsAt.getStyle().set("font-size", "0.85em").set("color", "#888");

            Span endsAt = new Span("Ends: " + auction.getAuctionEndTime().toString());
            endsAt.getStyle().set("font-size", "0.85em").set("color", "#888");

            card.add(name, currentPrice, startsAt, endsAt);
            onCardReady.accept(card);
        });
    }

    /**
     * Opens a dialog to edit an existing item (quantity/price/description + create auctions).
     */
    private void editItemDetails(ItemDTO item) {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");

        Div ratingDiv = new Div();
        ratingDiv.getStyle()
            .set("font-size", "0.85em")
            .set("color", "#333")
            .set("margin-bottom", "0.5rem");
        ratingDiv.setText("⭐ " + item.getRating());

        NumberField qtyField = new NumberField("Quantity");
        qtyField.setPlaceholder(String.valueOf(item.getQuantity()));
        qtyField.setMin(0);

        NumberField priceField = new NumberField("Price");
        priceField.setValue(item.getPrice());
        priceField.setMin(0);

        TextArea descArea = new TextArea("Description");
        descArea.setValue(item.getDescription());
        descArea.setWidthFull();

        // ─── “Create Auction” button inside item dialog ────────────────────────
        Button createAuction = new Button("Create Auction", e -> {
            Dialog auctionDialog = new Dialog();
            auctionDialog.setHeaderTitle("Create Auction");

            NumberField startingPriceField = new NumberField("Starting Price");
            startingPriceField.setMin(0.01);
            startingPriceField.setStep(0.1);
            startingPriceField.setValue(10.0);

            DateTimePicker startDateField = new DateTimePicker("Start Date & Time");
            startDateField.setValue(LocalDateTime.now());

            DateTimePicker endDateField = new DateTimePicker("End Date & Time");
            endDateField.setValue(LocalDateTime.now().plusHours(24));

            VerticalLayout layout = new VerticalLayout(
                startingPriceField, startDateField, endDateField
            );

            Button confirmBtn = new Button("Create", event -> {
                Double price = startingPriceField.getValue();
                LocalDateTime startDate = startDateField.getValue();
                LocalDateTime endDate = endDateField.getValue();

                if (price == null || startDate == null || endDate == null || !endDate.isAfter(startDate)) {
                    Notification.show("Please provide valid inputs", 3000, Position.MIDDLE);
                    return;
                }

                int currentShopID = item.getShopId();
                int itemID = item.getItemID();

                presenter.createAuction(currentShopID, itemID, price, startDate, endDate, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            Notification.show("Auction created", 2000, Position.TOP_CENTER);
                            auctionDialog.close();
                            dlg.close();
                            loadItems(currentShopID);
                        } else {
                            Notification.show("Failed to create auction", 2000, Position.MIDDLE);
                        }
                    });
                });
            });

            Button cancelBtn = new Button("Cancel", e2 -> auctionDialog.close());
            HorizontalLayout buttons = new HorizontalLayout(confirmBtn, cancelBtn);
            layout.add(buttons);

            auctionDialog.add(layout);
            auctionDialog.open();
        });
        createAuction.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        // ───────────────────────────────────────────────────────────────────────

        Button save = new Button("Save", e -> {
            int currentShopID = item.getShopId();
            int itemID = item.getItemID();
            int newQty = qtyField.getValue().intValue();
            double newPrice = priceField.getValue();
            String newDesc = descArea.getValue();

            presenter.changeItemQuantityInShop(currentShopID, itemID, newQty, qtyOk -> {
                if (!qtyOk) {
                    UI.getCurrent().access(() ->
                        Notification.show("Failed to update quantity", 2000, Position.MIDDLE)
                    );
                    return;
                }
                presenter.changeItemPriceInShop(currentShopID, itemID, newPrice, priceOk -> {
                    if (!priceOk) {
                        UI.getCurrent().access(() ->
                            Notification.show("Failed to update price", 2000, Position.MIDDLE)
                        );
                        return;
                    }
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

        HorizontalLayout actions = new HorizontalLayout(createAuction, save, delete, cancel);
        actions.setSpacing(true);

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

    /**
     * Builds a card for each BidDTO. In addition to showing “Bidder / Item / Offer”,
     * this card now contains:
     *   • “Answer” button → opens a simple accept/reject dialog
     *   • “Counter” button → opens a dialog to enter a new counter‐price
     */
    private VerticalLayout createBidCard(BidDTO bid, ShopDTO shop) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.START);
        card.getStyle()
            .set("border", "1px solid #ccc")
            .set("border-radius", "8px")
            .set("padding", "0.75rem")
            .set("width", "260px")
            .set("background-color", "#f9f9f9");

        // Get item name
        ItemDTO item = shop.getItems().get(bid.getItemId());
        String itemName = (item != null ? item.getName() : "Unknown Item");

        Span bidderName = new Span("Bidder: " + bid.getSubmitterId());
        Span itemNameSpan = new Span("Item: " + itemName);
        Span offerPrice = new Span("Offer: $" + String.format("%.2f", bid.getPrice()));

        bidderName.getStyle().set("font-weight", "bold");
        itemNameSpan.getStyle().set("margin-top", "0.25rem");
        offerPrice.getStyle().set("margin-top", "0.25rem").set("color", "#d35400");

        // ── “Answer” button: Accept / Reject ─────────────────────────────────────
        Button answerBtn = new Button("Answer", VaadinIcon.CHECK.create());
        answerBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        answerBtn.addClickListener(e -> {
            Dialog ansDialog = new Dialog();
            ansDialog.setHeaderTitle("Answer Bid");

            Span msg = new Span("Reject or accept this bid?");
            Button reject = new Button("Reject", ev -> {
                bidPresenter.answerBid(shop.getId(), bid.getId(), false, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            Notification.show("Bid rejected", 2000, Position.TOP_CENTER);
                            ansDialog.close();
                            loadItems(shop.getId());
                        } else {
                            Notification.show("Failed to answer bid", 2000, Position.MIDDLE);
                        }
                    });
                });
            });
            Button accept = new Button("Accept", ev -> {
                bidPresenter.answerBid(shop.getId(), bid.getId(), true, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            Notification.show("Bid accepted", 2000, Position.TOP_CENTER);
                            ansDialog.close();
                            loadItems(shop.getId());
                        } else {
                            Notification.show("Failed to answer bid", 2000, Position.MIDDLE);
                        }
                    });
                });
            });

            HorizontalLayout buttons = new HorizontalLayout(accept, reject);
            ansDialog.add(new VerticalLayout(msg, buttons));
            ansDialog.open();
        });
        // ─────────────────────────────────────────────────────────────────────────

        // ── “Counter” button: Enter a new price ─────────────────────────────────
        Button counterBtn = new Button("Counter", VaadinIcon.EXCHANGE.create());
        counterBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        counterBtn.addClickListener(e -> {
            Dialog ctrDialog = new Dialog();
            ctrDialog.setHeaderTitle("Submit Counter Bid");

            NumberField newOfferField = new NumberField("New Offer Price");
            newOfferField.setStep(0.01);
            newOfferField.setMin(0.01);
            newOfferField.setValue(bid.getPrice() + 1.0); // default

            Button submitCounter = new Button("Submit", ev -> {
                Double newOffer = newOfferField.getValue();
                if (newOffer == null || newOffer <= bid.getPrice()) {
                    Notification.show("Counter must exceed current offer", 2000, Position.MIDDLE);
                    return;
                }
                bidPresenter.submitCounterBid(shop.getId(), bid.getId(), newOffer, success -> {
                    UI.getCurrent().access(() -> {
                        if (success) {
                            Notification.show("Counter submitted", 2000, Position.TOP_CENTER);
                            ctrDialog.close();
                            loadItems(shop.getId());
                        } else {
                            Notification.show("Failed to submit counter", 2000, Position.MIDDLE);
                        }
                    });
                });
            });

            Button cancelCtr = new Button("Cancel", ev2 -> ctrDialog.close());
            HorizontalLayout ctrButtons = new HorizontalLayout(submitCounter, cancelCtr);
            ctrDialog.add(new VerticalLayout(newOfferField, ctrButtons));
            ctrDialog.open();
        });
        // ─────────────────────────────────────────────────────────────────────────

        card.add(bidderName, itemNameSpan, offerPrice, new HorizontalLayout(answerBtn, counterBtn));

        // ─── Status & Actions ───────────────────────────────────────────────────
        if (bid.getCounterAmount() == -1 && bid.getIsAccepted() == 0) {
            // initial state: show Answer & Counter
            card.add(new HorizontalLayout(answerBtn, counterBtn));
        }
        else if (bid.getCounterAmount() != -1 && bid.getIsAccepted() == 0) {
            // you’ve countered but customer hasn’t responded yet
            counterBtn.setEnabled(false);
            answerBtn.setEnabled(false);
            Span counterSpan = new Span("Counter: $" + String.format("%.2f", bid.getCounterAmount()));
            counterSpan.getStyle()
                .set("margin-top", "0.5rem")
                .set("color", "#c0392b")
                .set("font-weight", "bold");
            card.add(counterSpan);
        }
        else if (bid.getIsAccepted() == 1) {
            // bid accepted
            answerBtn.setEnabled(false);
            counterBtn.setEnabled(false);
            Span accepted = new Span("Accepted");
            accepted.getStyle()
                .set("margin-top", "0.5rem")
                .set("color", "#27ae60")
                .set("font-weight", "bold");
            card.add(accepted);
        }
        else if (bid.getIsAccepted() == -1)
        {
            // bid rejected (isAccepted != 0 or counter logic)
            answerBtn.setEnabled(false);
            counterBtn.setEnabled(false);
            Span rejected = new Span("Rejected");
            rejected.getStyle()
                .set("margin-top", "0.5rem")
                .set("color", "#e74c3c")
                .set("font-weight", "bold");
            card.add(rejected);
        }

        return card;
    }
}