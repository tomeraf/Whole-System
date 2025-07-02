package com.halilovindustries.frontend.application.views;

import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.frontend.application.presenters.BidPresenter;
import com.halilovindustries.frontend.application.presenters.InboxPresenter;
import com.halilovindustries.frontend.application.presenters.ShopPresenter;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;

@Route(value = "shop/", layout = MainLayout.class)
@PageTitle("Shop Details")
public class ShopView extends VerticalLayout implements HasUrlParameter<Integer> {

    private final ShopPresenter shopPresenter;
    private final InboxPresenter inboxPresenter;
    private int shopId;
    private Button msgBtn;

    @Autowired
    public ShopView(ShopPresenter shopPresenter, InboxPresenter inboxPresenter) {
        this.shopPresenter = shopPresenter;
        this.inboxPresenter = inboxPresenter;
        setPadding(true);
        setSpacing(true);

        // Basic header rows (will be replaced on data load)
        add(new H2("Loading shop..."));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer shopId) {
        this.shopId = shopId;
        if (shopId != null) {
            loadShopData(shopId);
        } else {
            removeAll();
            add(new Span("Invalid shop ID."));
        }
    }

    private void loadShopData(int shopId) {
        shopPresenter.getShopInfo(shopId, shop -> {
            if (shop == null) return;
            UI.getCurrent().access(() -> {
                removeAll(); // clear placeholder

                // Shop header
                H2 title = new H2(shop.getName());
                Span desc = new Span("Description: " + shop.getDescription());
                Span rating = new Span("⭐ " + shop.getRating() + " (" + shop.getRatingCount() + " raters)");
                VerticalLayout actions = buildActionsBar();
                add(title, actions);

                // Search and filter bar (optional reuse)
                add(desc, rating);

                // Items grid
                shopPresenter.showShopItems(shopId, items -> {
                    FlexLayout itemsLayout = new FlexLayout();
                    itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                    itemsLayout.getStyle().set("gap", "1rem");
                    items.forEach(item -> itemsLayout.add(createItemCard(item)));
                    add(itemsLayout);
                });

                // Auctions grid
                shopPresenter.getActiveAuctions(shopId, auctions -> {
                    if (auctions != null && !auctions.isEmpty()) {
                        VerticalLayout auctionSection = new VerticalLayout();
                        auctionSection.add(new H2("Auctions"));
                        FlexLayout auctionLayout = new FlexLayout();
                        auctionLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                        auctionLayout.getStyle().set("gap", "1rem");

                        auctions.forEach(auction -> {
                            createAuctionCard(shopId, auction, auctionCard -> {
                                auctionLayout.add(auctionCard);
                            });
                        });
                        auctionSection.add(auctionLayout);
                        add(auctionSection);
                    }
                });
                shopPresenter.getWonAuctions(shopId, auctions -> {
                    if (auctions != null && !auctions.isEmpty()) {
                        VerticalLayout auctionSection = new VerticalLayout();
                        auctionSection.add(new H2("Won Auctions"));
                        FlexLayout auctionLayout = new FlexLayout();
                        auctionLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                        auctionLayout.getStyle().set("gap", "1rem");

                        auctions.forEach(auction -> {
                            createWonAuctionCard(shopId, auction, auctionCard -> {
                                auctionLayout.add(auctionCard);
                            });
                        });
                        auctionSection.add(auctionLayout);
                        add(auctionSection);
                    }
                });
            
                // — “Counter” bids —
                shopPresenter.getUserBids(shopId, "Counter", bids -> {
                    UI.getCurrent().access(() -> {
                        if (bids != null && !bids.isEmpty()) {
                            add(new H2("Bids Awaiting Your Response"));
                            FlexLayout counterLayout = new FlexLayout();
                            counterLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                            counterLayout.getStyle().set("gap", "1rem");

                            for (BidDTO bid : bids) {
                                // Here counterAmount != -1 and isAccepted == 0, so createCustomerBidCard adds buttons.
                                counterLayout.add(createCustomerBidCard(bid, shop));
                            }
                            add(counterLayout);
                        }
                    });
                });
            
            
            // — “Accepted” bids (for example) —
            shopPresenter.getUserBids(shopId, "Accepted", bids -> {
                UI.getCurrent().access(() -> {
                    if (bids != null && !bids.isEmpty()) {
                        add(new H2("Accepted Bids"));
                        FlexLayout acceptedLayout = new FlexLayout();
                        acceptedLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                        acceptedLayout.getStyle().set("gap", "1rem");

                        for (BidDTO bid : bids) {
                            // If isAccepted==1 ⇒ our if‐block (bid.getIsAccepted()==0) fails, so no buttons again.
                            acceptedLayout.add(createCustomerBidCard(bid, shop));
                        }
                        add(acceptedLayout);
                    }
                });
            });

            // ─── “My Rejected Bids” SECTION ────────────────────────────────────────────────
            shopPresenter.getUserBids(shopId, "Rejected", bids -> {
                UI.getCurrent().access(() -> {
                    if (bids != null && !bids.isEmpty()) {
                        add(new H2("Rejected Bids"));
                        FlexLayout rejectedLayout = new FlexLayout();
                        rejectedLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                        rejectedLayout.getStyle().set("gap", "1rem");

                        for (BidDTO bid : bids) {
                            rejectedLayout.add(createCustomerBidCard(bid, shop));
                        }
                        add(rejectedLayout);
                    }
                });
            });

            // — “In Progress” bids —
            shopPresenter.getUserBids(shopId, "In Progress", bids -> {
                UI.getCurrent().access(() -> {
                    if (bids != null && !bids.isEmpty()) {
                        add(new H2("Bids In Progress"));
                        FlexLayout inProgressLayout = new FlexLayout();
                        inProgressLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                        inProgressLayout.getStyle().set("gap", "1rem");

                        for (BidDTO bid : bids) {
                            // Because counterAmount == -1 here, createCustomerBidCard will NOT add buttons.
                            inProgressLayout.add(createCustomerBidCard(bid, shop));
                        }
                        add(inProgressLayout);
                    }
                });
            });

        });
    });
    }

    private void openMessageDialog() {
    Dialog messageDialog = new Dialog();
    messageDialog.setWidth("400px");

    TextField subjectField = new TextField("Subject");
    subjectField.setWidthFull();
    TextField messageField = new TextField("Message");
    messageField.setWidthFull();
    messageField.setHeight("100px");

    Button sendBtn = new Button("Send", event -> {
        String subject = subjectField.getValue();
        String message = messageField.getValue();

        inboxPresenter.sendMessege(shopId, subject, message, success -> {
            if (success) {
                messageDialog.close();
                UI.getCurrent().access(() -> {
                    Dialog confirmation = new Dialog(new Span("Message sent successfully!"));
                    confirmation.setCloseOnOutsideClick(true);
                    confirmation.open();
                });
            } else {
                UI.getCurrent().access(() -> {
                    Dialog errorDialog = new Dialog(new Span("Failed to send message. Please try again."));
                    errorDialog.setCloseOnOutsideClick(true);
                    errorDialog.open();
                });
            }
        });
        messageDialog.close();
    });

sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    VerticalLayout dialogLayout = new VerticalLayout(
        new H2("Send a Message to the Shop"),
        subjectField,
        messageField,
        sendBtn
    );
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(true);
    messageDialog.add(dialogLayout);

    messageDialog.setCloseOnOutsideClick(true);
    messageDialog.setCloseOnEsc(true);
        subjectField.clear();
        messageField.clear();
        messageDialog.open();
    // // Button click opens dialog
    // msgBtn.addClickListener(e -> {
    
    // });
        }

    private VerticalLayout buildActionsBar() {
    // 2️⃣ Filter button
    Button filterBtn = new Button("", VaadinIcon.FILTER.create());
    filterBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
    filterBtn.getStyle()
        .set("height", "36px").set("min-width", "36px").set("padding", "0")
        .set("border-radius", "4px 0 0 4px").set("border", "1px solid #ccc").set("border-right", "none")
        .set("background-color", "lightblue").set("color", "black");
    filterBtn.addClickListener(e -> openFilterDialog());

    // 3️⃣ Search bar
    TextField searchBar = new TextField();
    searchBar.setPlaceholder("Search here…");
    searchBar.setWidth("400px");
    searchBar.getStyle()
        .set("height", "38px").set("border-radius", "0").set("border-left", "none").set("border-right", "none");

    // 4️⃣ Search button
    Button searchBtn = new Button(VaadinIcon.SEARCH.create());
    searchBtn.getStyle()
        .set("height", "36px").set("min-width", "36px").set("padding", "0")
        .set("border-radius", "0 4px 4px 0").set("border", "1px solid #ccc").set("border-left", "none")
        .set("background-color", "#F7B05B").set("color", "black");

        searchBtn.addClickListener(e -> {
                    String itemName = searchBar.getValue().trim();
                    shopPresenter.getItemByFilter(shopId,itemName, new HashMap<>(), filteredItems -> {
                        UI.getCurrent().access(() -> {
                            if (filteredItems == null || filteredItems.isEmpty()) {
                                Notification.show("No items matched your filters.", 3000, Position.MIDDLE);
                            } else {
                                Notification.show("Filter applied.", 2000, Position.BOTTOM_START);
                            }
                            removeAll();
                            FlexLayout itemsLayout = new FlexLayout();
                            itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                            itemsLayout.getStyle().set("gap", "1rem");
                            for (ItemDTO item : filteredItems) {
                                itemsLayout.add(createItemCard(item)); // assuming you have this method
                            }
                            add(itemsLayout); // finally add it to the view

                        });
                    });
                });





    // 5️⃣ Message button
    msgBtn = new Button("Message", VaadinIcon.ENVELOPE.create());
    msgBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    msgBtn.getStyle()
        .set("background-color", "#6200EE").set("color", "white")
        .set("border", "none").set("border-radius", "8px")
        .set("padding", "0.6em 1.2em");

    msgBtn.addClickListener(e -> 
        openMessageDialog()
    );

    // 6️⃣ Search group
    HorizontalLayout searchBarGroup = new HorizontalLayout(filterBtn, searchBar, searchBtn);
    searchBarGroup.setWidthFull();
    searchBarGroup.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    searchBarGroup.setAlignItems(FlexComponent.Alignment.CENTER);
    searchBarGroup.setSpacing(false);
    searchBarGroup.setPadding(false);

    // 7️⃣ Actions row (center search + right message)
    HorizontalLayout actionsRow = new HorizontalLayout(searchBarGroup, msgBtn);
    actionsRow.setWidthFull();
    actionsRow.setAlignItems(FlexComponent.Alignment.CENTER);
    actionsRow.expand(searchBarGroup);

    // ✅ Wrap in a container
    VerticalLayout wrapper = new VerticalLayout();
    wrapper.setPadding(false);
    wrapper.setSpacing(false);
    wrapper.setWidthFull();
    wrapper.add(actionsRow);

    return wrapper;
}

    private void openFilterDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");


        NumberField minPrice = new NumberField("Min Price");
        NumberField maxPrice = new NumberField("Max Price");
        ComboBox<String> category = new ComboBox<>("Category");
        category.setItems("All", "Electronics", "Clothing", "Books");
        ComboBox<Integer> rating = new ComboBox<>("Rating");
        rating.setItems(1,2,3,4,5);

        Button apply = new Button("Apply", e -> {
        HashMap<String, String> filters = new HashMap<>();

        if (minPrice.getValue() != null) {
            filters.put("minPrice", String.valueOf(minPrice.getValue()));
        }
        if (maxPrice.getValue() != null) {
            filters.put("maxPrice", String.valueOf(maxPrice.getValue()));
        }
        if (category.getValue() != null && !"All".equals(category.getValue())) {
            filters.put("category", category.getValue());
        }
        if (rating.getValue() != null) {
            filters.put("rating", String.valueOf(rating.getValue()));
        }

        shopPresenter.getItemByFilter(shopId,null, filters, filteredItems -> {
            UI.getCurrent().access(() -> {
                if (filteredItems == null || filteredItems.isEmpty()) {
                    Notification.show("No items matched your filters.", 3000, Position.MIDDLE);
                } else {
                    Notification.show("Filter applied.", 2000, Position.BOTTOM_START);
                }
                removeAll();
                FlexLayout itemsLayout = new FlexLayout();
                itemsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
                itemsLayout.getStyle().set("gap", "1rem");
                for (ItemDTO item : filteredItems) {
                    itemsLayout.add(createItemCard(item)); // assuming you have this method
                }
                add(itemsLayout); // finally add it to the view

            });
        });
        dialog.close();
    });

        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
            new Text("Filter by:"),
            minPrice, maxPrice, category, rating, apply
        );
        layout.setPadding(false);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    private VerticalLayout createItemCard(ItemDTO item) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
            .set("border", "1px solid #ddd")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("width", "200px");

        Span name = new Span(item.getName());
        name.getStyle().set("font-size", "0.9em");
        Span price = new Span("$" + item.getPrice());
        price.getStyle().set("font-weight", "bold");

        // Quantity controls (as before)
        AtomicInteger qty = new AtomicInteger(1);
        Span qtyLabel = new Span(String.valueOf(qty.get()));
        Button minus = new Button("-", e -> {
            if (qty.get() > 1) qty.decrementAndGet();
            qtyLabel.setText(String.valueOf(qty.get()));
        });
        Button plus = new Button("+", e -> {
            qty.incrementAndGet();
            qtyLabel.setText(String.valueOf(qty.get()));
        });
        minus.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");
        plus.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");
        HorizontalLayout qtyControls = new HorizontalLayout(minus, qtyLabel, plus);
        qtyControls.setAlignItems(FlexComponent.Alignment.CENTER);

        // “Add to Cart” button (as before)
        Button add = new Button("Add to Cart", e ->
            shopPresenter.saveInCart(new ItemDTO(
                item.getName(), item.getCategory(), item.getPrice(),
                item.getShopId(), item.getItemID(), qty.get(),
                item.getRating(), item.getDescription(), item.getNumOfOrders()
            ))
        );
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setWidthFull();
        add.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");

        Button bidBtn = new Button("Place Bid", e -> openBidDialog(item));
        bidBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        bidBtn.setWidthFull();
        bidBtn.getElement().addEventListener("click", ev -> {}).addEventData("event.stopPropagation()");

        card.add(name, price, qtyControls, add, bidBtn);

        // Show item details on card click (same as before)
        card.addClickListener(e -> {
            Dialog details = new Dialog();
            details.setWidth("400px");
            details.add(
                new H2(item.getName()),
                new Paragraph("Category: " + item.getCategory().name()),
                new Paragraph("Description: " + item.getDescription()),
                new Paragraph("Price: $" + item.getPrice()),
                new Paragraph("Rating: ⭐ " + item.getRating()),
                new Paragraph("Available: " + item.getQuantity()),
                new Button("Close", ev -> details.close())
            );
            details.open();
        });

        return card;
    }

      private void createAuctionCard(int shopID, AuctionDTO auction, Consumer<VerticalLayout> onCardReady) {
        shopPresenter.getShopInfo(shopID, shop -> {
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

            Span currentPrice = new Span("Current: $" + (auction.getHighestBid() > auction.getStartingBid() ? auction.getHighestBid() : auction.getStartingBid()));
            currentPrice.getStyle().set("color", "#d35400");

            Span endsAt = new Span("Ends: " + auction.getAuctionEndTime().toString());
            endsAt.getStyle().set("font-size", "0.85em").set("color", "#888");

            Button offerButton = new Button("Place offer", e -> openOfferDialog(shopID, auction, currentPrice));
            offerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            card.add(name, currentPrice, endsAt, offerButton);
            onCardReady.accept(card);
        });
    }

    private void createWonAuctionCard(int shopID, AuctionDTO auction, Consumer<VerticalLayout> onCardReady) {
        shopPresenter.getShopInfo(shopID, shop -> {
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

            Span Price = new Span("Current: $" + auction.getHighestBid());
            Price.getStyle().set("color", "#d35400");

            Span endedAt = new Span("Ended at: " + auction.getAuctionEndTime().toString());
            endedAt.getStyle().set("font-size", "0.85em").set("color", "#888");

            Button purchaseButton = new Button("Purchase", e -> openAuctionPurchaseDialog(shopID, auction, Price));
            purchaseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            card.add(name, Price, endedAt, purchaseButton);
            onCardReady.accept(card);
        });
    }

    private void openOfferDialog(int shopID, AuctionDTO auction, Span currentPriceLabel) {
        Dialog dialog = new Dialog();
        dialog.setWidth("300px");

        NumberField bidField = new NumberField("Your offer");
        bidField.setStep(0.01);
        bidField.setMin(auction.getHighestBid());
        bidField.setValue(auction.getHighestBid());

        Button confirm = new Button("Confirm offer", event -> {
            double bid = bidField.getValue();
            // Submit bid through presenter
            shopPresenter.submitAuctionOffer(shopID, auction.getId(), bid, success -> {
                if (success) {
                    currentPriceLabel.setText("Current: $" + bid);
                    dialog.close();
                } else {
                    bidField.setInvalid(true);
                    bidField.setErrorMessage("Offer failed. Try again.");
                }
            });
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
            new H2("Place an offer"),
            bidField,
            confirm
        );
        dialog.add(layout);
        dialog.open();
    }

    private void openAuctionPurchaseDialog(int shopID, AuctionDTO auction, Span currentPriceLabel) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.START);

        // —— Header ——
        H2 title = new H2("Checkout");
        Button back = new Button("x", e -> dialog.close()); // Close dialog instead of navigating
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        HorizontalLayout header = new HorizontalLayout(title, back);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        dialogLayout.add(header);

        // —— Shipment Details ——
        dialogLayout.add(new H3("Shipment Details"));
        FormLayout shipForm = new FormLayout();
        shipForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        shipForm.getElement().setAttribute("autocomplete", "off");

        TextField fullName = new TextField("Full Name");
        TextField email    = new TextField("Email");
        TextField phone    = new TextField("Phone");
        TextField address  = new TextField("Address");
        TextField city     = new TextField("City");
        TextField zipCode  = new TextField("Zip Code");
        ComboBox<String> country = new ComboBox<>("Country");
        country.setItems(
            Arrays.stream(Locale.getISOCountries())
                .map(c -> new Locale("",c).getDisplayCountry())
                .sorted()
                .collect(Collectors.toList())
        );

        shipForm.add(fullName, email, phone, address, city, country, zipCode);
        dialogLayout.add(shipForm);

        // —— Payment Details ——
        dialogLayout.add(new H3("Payment Details"));
        FormLayout payForm = new FormLayout();
        payForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        payForm.getElement().setAttribute("autocomplete", "off");

        TextField cardHolderName = new TextField("Card Holder Name");
        TextField holderId       = new TextField("Card Holder ID");
        TextField cardNumber     = new TextField("Card Number");
        ComboBox<String> expMonth = new ComboBox<>("Exp. Month");
        expMonth.setItems("01","02","03","04","05","06","07","08","09","10","11","12");

        ComboBox<String> expYear  = new ComboBox<>("Exp. Year");
        int currentYear = Year.now().getValue();
        List<String> years = IntStream
            .range(currentYear, currentYear + 41)
            .mapToObj(String::valueOf)
            .collect(Collectors.toList());
        expYear.setItems(years);

        PasswordField cvv = new PasswordField("CVV");
        expYear.getElement().setAttribute("autocomplete", "new-password");
        cvv.getElement().setAttribute("autocomplete", "new-password");

        payForm.add(cardHolderName, holderId, cardNumber, expMonth, expYear, cvv);
        dialogLayout.add(payForm);

        // —— Place Order ——
        Button placeOrder = new Button("Place Order", VaadinIcon.CART.create());
        placeOrder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        placeOrder.getStyle().set("margin-top", "1rem");

        placeOrder.addClickListener(e -> {
            shopPresenter.getSessionToken(token -> {
                UI ui = UI.getCurrent();
                if (ui == null) return;
                ui.access(() -> {
                    if (token == null || !shopPresenter.validateToken(token)) {
                        Notification.show("Session expired, please log in again", 2000, Notification.Position.MIDDLE);
                        return;
                    }

                    ShipmentDetailsDTO shipDto = new ShipmentDetailsDTO(
                        holderId.getValue(),
                        fullName.getValue(),
                        email.getValue(),
                        phone.getValue(),
                        country.getValue(),
                        city.getValue(),
                        address.getValue(),
                        zipCode.getValue()
                    );

                    PaymentDetailsDTO payDto = new PaymentDetailsDTO(
                        cardNumber.getValue(),
                        cardHolderName.getValue(),
                        holderId.getValue(),
                        cvv.getValue(),
                        expMonth.getValue(), expYear.getValue()
                    );

                    shopPresenter.purchaseAuctionItem(shopID, auction.getId(), payDto, shipDto, order -> {
                        ui.access(() -> {
                            if (order) {
                                Notification.show("Order placed! 🎉", 2000, Position.MIDDLE);
                                dialog.close(); // Close on success
                                UI.getCurrent().navigate(""); // Or refresh, or go somewhere
                            } else {
                                Notification.show("Failed to place order", 2000, Position.MIDDLE);
                            }
                        });
                    });
                });
            });
        });

        dialogLayout.add(placeOrder);
        dialogLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, placeOrder);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void openBidPurchaseDialog(int shopID, BidDTO bid, Span currentPriceLabel) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.START);

        // —— Header ——
        H2 title = new H2("Checkout");
        Button back = new Button("x", e -> dialog.close()); // Close dialog instead of navigating
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        HorizontalLayout header = new HorizontalLayout(title, back);
        header.setWidthFull();
        header.expand(title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        dialogLayout.add(header);

        // —— Shipment Details ——
        dialogLayout.add(new H3("Shipment Details"));
        FormLayout shipForm = new FormLayout();
        shipForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        shipForm.getElement().setAttribute("autocomplete", "off");

        TextField fullName = new TextField("Full Name");
        TextField email    = new TextField("Email");
        TextField phone    = new TextField("Phone");
        TextField address  = new TextField("Address");
        TextField city     = new TextField("City");
        TextField zipCode  = new TextField("Zip Code");
        ComboBox<String> country = new ComboBox<>("Country");
        country.setItems(
            Arrays.stream(Locale.getISOCountries())
                .map(c -> new Locale("",c).getDisplayCountry())
                .sorted()
                .collect(Collectors.toList())
        );

        shipForm.add(fullName, email, phone, address, city, country, zipCode);
        dialogLayout.add(shipForm);

        // —— Payment Details ——
        dialogLayout.add(new H3("Payment Details"));
        FormLayout payForm = new FormLayout();
        payForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );
        payForm.getElement().setAttribute("autocomplete", "off");

        TextField cardHolderName = new TextField("Card Holder Name");
        TextField holderId       = new TextField("Card Holder ID");
        TextField cardNumber     = new TextField("Card Number");
        ComboBox<String> expMonth = new ComboBox<>("Exp. Month");
        expMonth.setItems("01","02","03","04","05","06","07","08","09","10","11","12");

        ComboBox<String> expYear  = new ComboBox<>("Exp. Year");
        int currentYear = Year.now().getValue();
        List<String> years = IntStream
            .range(currentYear, currentYear + 41)
            .mapToObj(String::valueOf)
            .collect(Collectors.toList());
        expYear.setItems(years);

        PasswordField cvv = new PasswordField("CVV");
        expYear.getElement().setAttribute("autocomplete", "new-password");
        cvv.getElement().setAttribute("autocomplete", "new-password");

        payForm.add(cardHolderName, holderId, cardNumber, expMonth, expYear, cvv);
        dialogLayout.add(payForm);

        // —— Place Order ——
        Button placeOrder = new Button("Place Order", VaadinIcon.CART.create());
        placeOrder.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        placeOrder.getStyle().set("margin-top", "1rem");

        placeOrder.addClickListener(e -> {
            shopPresenter.getSessionToken(token -> {
                UI ui = UI.getCurrent();
                if (ui == null) return;
                ui.access(() -> {
                    if (token == null || !shopPresenter.validateToken(token)) {
                        Notification.show("Session expired, please log in again", 2000, Notification.Position.MIDDLE);
                        return;
                    }

                    ShipmentDetailsDTO shipDto = new ShipmentDetailsDTO(
                        holderId.getValue(),
                        fullName.getValue(),
                        email.getValue(),
                        phone.getValue(),
                        country.getValue(),
                        city.getValue(),
                        address.getValue(),
                        zipCode.getValue()
                    );

                    PaymentDetailsDTO payDto = new PaymentDetailsDTO(
                        cardNumber.getValue(),
                        cardHolderName.getValue(),
                        holderId.getValue(),
                        cvv.getValue(),
                        expMonth.getValue(), expYear.getValue()
                    );

                    shopPresenter.purchaseBidItem(shopID, bid.getId(), payDto, shipDto, success -> {
                    ui.access(() -> {
                        if (success) {  // Check for boolean true/false
                            Notification.show("Order placed! 🎉", 2000, Position.MIDDLE);
                            dialog.close();
                            // Consider delaying navigation slightly
                            UI.getCurrent().getPage().executeJs("setTimeout(() => {$0.navigate('');}, 300)", UI.getCurrent());
                        } else {
                            Notification.show("Failed to place order", 2000, Position.MIDDLE);
                        }
                    });
                });
                });
            });
        });

        dialogLayout.add(placeOrder);
        dialogLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, placeOrder);

        dialog.add(dialogLayout);
        dialog.open();
    }


    /**
    * Opens a modal dialog that lets the user enter a bid amount
    * and submit it via ShopPresenter.submitBidOffer(…).
    */
    private void openBidDialog(ItemDTO item) {
        Dialog dialog = new Dialog();
        dialog.setWidth("320px");

        // Title
        H2 title = new H2("Place a Bid");
        title.getStyle().set("margin-bottom", "0.5rem");

        // NumberField for bid amount, starting at current price
        NumberField bidField = new NumberField("Your bid (USD)");
        bidField.setStep(0.01);
        bidField.setWidthFull();
        // Minimum: item.getPrice() (so bid must exceed “list price”)
        bidField.setMin(item.getPrice());
        bidField.setValue(item.getPrice());

        // “Confirm Bid” button
        Button confirm = new Button("Confirm Bid", event -> {
            Double bidAmount = bidField.getValue();
            if (bidAmount == null || bidAmount <= 0) {
                bidField.setInvalid(true);
                bidField.setErrorMessage("Bid must be ≥ 0$");
                return;
            }

            // 1) Obtain a session token & call presenter
            shopPresenter.getSessionToken(token -> {
                UI ui = UI.getCurrent();
                if (ui == null) return;

                ui.access(() -> {
                    // Validate token / login status
                    if (token == null || !shopPresenter.validateToken(token) || !shopPresenter.isLoggedIn(token)) {
                        Notification.show("Please log in before bidding.", 2000, Position.MIDDLE);
                        return;
                    }

                    // 2) Actually submit the bid to the backend
                    shopPresenter.submitBidOffer(item.getShopId(), item.getItemID(), bidAmount, success -> {
                        if (success) {
                            Notification.show("Bid submitted: $" + String.format("%.2f", bidAmount),
                                            2000, Position.TOP_END);
                            dialog.close();
                        } else {
                            Notification.show("Bid failed. Try again.", 2000, Position.MIDDLE);
                        }
                    });
                });
            });
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Layout everything vertically
        VerticalLayout layout = new VerticalLayout(
            title,
            bidField,
            confirm
        );
        layout.setSpacing(true);
        layout.setPadding(true);
        dialog.add(layout);

        // Clear any prior values & open
        bidField.clear();
        dialog.open();
    }

    /**
     * A small card for each BidDTO where the shop has already countered.
     * If counterAmount == -1 (i.e. “In Progress”), we’ll show only basic info—
     * no accept/reject buttons.  If counterAmount != -1 and isAccepted == 0,
     * we add the Accept/Reject buttons.
     */
    private VerticalLayout createCustomerBidCard(BidDTO bid, ShopDTO shop) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.START);
        card.getStyle()
            .set("border", "1px solid #aaa")
            .set("border-radius", "6px")
            .set("padding", "0.75rem")
            .set("width", "240px")
            .set("background-color", "#f0f8ff");

        // 1) Look up item name
        ItemDTO item = shop.getItems().get(bid.getItemId());
        String itemName = (item != null ? item.getName() : "Unknown Item");

        // 2) Display item, original bid, and the shop’s “counter” field (could be -1)
        Span itemSpan = new Span("Item: " + itemName);
        itemSpan.getStyle().set("font-weight", "bold");

        Span you = new Span("First Price: $" + String.format("%.2f", bid.getAmount()));
        you.getStyle().set("margin-top", "0.3rem");

        card.add(itemSpan, you);

        // 3) Only if counterAmount != -1 AND isAccepted == 0 do we add “Accept/Reject”
        if (bid.getCounterAmount() != -1 && bid.getIsAccepted() == 0) {
            // Show the shop’s counter amount (which might be -1 if “in progress”)
            Span counter = new Span("Shop's Counter: $" + String.format("%.2f", bid.getPrice()));
            counter.getStyle()
                .set("margin-top", "0.2rem")
                .set("color", "#c0392b")
                .set("font-weight", "bold");

            card.add(counter);

            Button acceptBtn = new Button("Accept", VaadinIcon.CHECK.create());
            acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            acceptBtn.addClickListener(e -> {
                Dialog d = new Dialog();
                d.setHeaderTitle("Accept Counter Offer");

                Span msg = new Span("Do you want to accept $" +
                String.format("%.2f", bid.getPrice()) + "?");
                msg.getStyle().set("margin-bottom", "0.5rem");

                Button yes = new Button("Yes, Accept", ev -> {
                    shopPresenter.answerOnCounterBid(
                        item.getShopId(),
                        bid.getId(),
                        true,
                        success -> {
                            UI.getCurrent().access(() -> {
                                if (success) {
                                    Notification.show("Counter accepted!", 2000, Position.TOP_CENTER);
                                    d.close();
                                    // reload everything
                                    loadShopData(item.getShopId());
                                } else {
                                    Notification.show("Failed to accept. Try again.", 2000, Position.MIDDLE);
                                }
                            });
                        }
                    );
                });
                yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                Button no = new Button("Cancel", ev2 -> d.close());
                HorizontalLayout h = new HorizontalLayout(yes, no);
                h.setSpacing(true);

                d.add(new VerticalLayout(msg, h));
                d.open();
            });

            Button rejectBtn = new Button("Reject", VaadinIcon.CLOSE.create());
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            rejectBtn.addClickListener(e -> {
                Dialog d = new Dialog();
                d.setHeaderTitle("Reject Counter Offer");

                Span msg = new Span("Reject the shop's counter of $" +
                String.format("%.2f", bid.getPrice()) + "?");
                msg.getStyle().set("margin-bottom", "0.5rem");

                Button yes = new Button("Yes, Reject", ev -> {
                    shopPresenter.answerOnCounterBid(
                        item.getShopId(),
                        bid.getId(),
                        false,
                        success -> {
                            UI.getCurrent().access(() -> {
                                if (success) {
                                    Notification.show("Counter rejected.", 2000, Position.TOP_CENTER);
                                    d.close();
                                    loadShopData(item.getShopId());
                                } else {
                                    Notification.show("Failed to reject. Try again.", 2000, Position.MIDDLE);
                                }
                            });
                        }
                    );
                });
                yes.addThemeVariants(ButtonVariant.LUMO_ERROR);

                Button no = new Button("Cancel", ev2 -> d.close());
                HorizontalLayout h = new HorizontalLayout(yes, no);
                h.setSpacing(true);

                d.add(new VerticalLayout(msg, h));
                d.open();
            });

            HorizontalLayout buttons = new HorizontalLayout(acceptBtn, rejectBtn);
            buttons.setSpacing(true);
            buttons.setPadding(false);

            card.add(buttons);
        }
        if (bid.getIsAccepted() == 1 && !bid.isDone()) {
            Span price = new Span("Bid price: $" + String.format("%.2f", bid.getPrice()));
            price.getStyle().set("font-weight", "bold").set("color", "#27ae60");
            Button purchaseButton = new Button("Purchase", e -> openBidPurchaseDialog(shopId, bid, price));
            purchaseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            card.add(price, purchaseButton);
        }
        // If counterAmount == -1 (or isAccepted != 0), we never added Accept/Reject.
        return card;
    }
}
