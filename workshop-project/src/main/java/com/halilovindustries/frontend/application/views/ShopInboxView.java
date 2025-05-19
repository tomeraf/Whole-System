package com.halilovindustries.frontend.application.views;

import java.util.List;
import java.util.function.Consumer;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.frontend.application.presenters.ShopInboxPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


@Route(value = "shop-inbox", layout = MainLayout.class)
@PageTitle("Inbox")
public class ShopInboxView extends VerticalLayout implements HasUrlParameter<Integer>{

    private Integer shopID;
    private ShopInboxPresenter presenter;
    private Button back = new Button("← Back");
    

    public ShopInboxView(ShopInboxPresenter presenter) {
        this.presenter = presenter;
        setPadding(true);
        setSpacing(true);

        back.addClickListener(e -> {
            if (shopID > 0) {
                UI.getCurrent().navigate("manage-shop/" + shopID);
            }
        });
        // — Title —
        add(new H2("Inbox"), back);

        // — Search bar (no filter) —
        HorizontalLayout searchRow = new HorizontalLayout(createSearchBar());
        searchRow.setWidthFull();
        searchRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(searchRow);

        // — Conversations list skeleton —
        Grid<Message> grid = new Grid<>(Message.class, false);
        // Shop column
        grid.addColumn(v -> "")
                .setHeader("Shops")
                .setAutoWidth(true);
        // “Open” arrow column
        grid.addComponentColumn(v -> {
                    Button open = new Button(VaadinIcon.CHEVRON_RIGHT.create());
                    open.addThemeVariants(ButtonVariant.LUMO_ICON);
                    return open;
                })
                .setHeader("")  // blank header
                .setAutoWidth(true);

        // no data → only headers show
        add(grid);

        setSizeFull();
    }

    private HorizontalLayout createSearchBar() {
        TextField search = new TextField();
        search.setPlaceholder("Search shops…");
        search.setWidth("400px");
        search.getStyle()
                .set("height", "38px")
                .set("border-radius", "4px 0 0 4px")
                .set("border-right", "none");

        Button btn = new Button(VaadinIcon.SEARCH.create());
        btn.getStyle()
                .set("height", "38px")
                .set("min-width", "38px")
                .set("padding", "0")
                .set("border-radius", "0 4px 4px 0")
                .set("border", "1px solid #ccc")
                .set("background-color", "#F7B05B")
                .set("color", "black");

        HorizontalLayout bar = new HorizontalLayout(search, btn);
        bar.setSpacing(false);
        bar.setPadding(false);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        return bar;
    }
    



    @Override
public void setParameter(BeforeEvent event, Integer shopID) {
    this.shopID = shopID;
    removeAll();
    add(back, new H2("Inbox"));

    // 1️⃣ Build the grid using your real Message class
    Grid<Message> grid = new Grid<>(Message.class, false);
    
    grid.addColumn(msg -> msg.isFromUser() ? msg.getUserName() : msg.getShopName())
        .setHeader("From").setAutoWidth(true);
    grid.addColumn(Message::getTitle)
        .setHeader("Subject")
        .setAutoWidth(true);
    grid.addColumn(msg -> msg.getDateTime().toString().substring(0, 19))
        .setHeader("Date")
        .setAutoWidth(true);

    // 2️⃣ Show the content in a dialog when they click a row
    grid.asSingleSelect().addValueChangeListener(e -> {
        Message msg = e.getValue();
        if (msg != null && msg.needResponse() && msg.isFromUser()) {
            Dialog dlg = new Dialog();
            dlg.add(
                new H2(msg.getTitle()),
                new Paragraph(msg.getContent()),
                new Button("Close", ev -> dlg.close()),
                new Button("Reply", ev -> 
                {
                    dlg.close();
                    Dialog messageDialog = new Dialog();
                        messageDialog.setWidth("400px");

                        TextField subjectField = new TextField("Subject");
                        subjectField.setWidthFull();
                        TextField messageField = new TextField("Message");
                        messageField.setWidthFull();
                        messageField.setHeight("100px");

                        Button sendBtn = new Button("Send", sendEvent -> {
                            String subject = subjectField.getValue();
                            String message = messageField.getValue();

                            presenter.respondToMessage(shopID, msg.getId(), subject, message, success -> {
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
                            new H2("Reply to " + msg.getUserName()),
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
                })
            );
            dlg.open();
        }
        else
        {
            Dialog dlg = new Dialog();
            dlg.add(
                new H2(msg.getTitle()),
                new Paragraph(msg.getContent()),
                new Button("Close", closeEvent -> dlg.close())
            );
            dlg.open();
        }
    });

    add(grid);
    setSizeFull();

    // 3️⃣ Load the real messages
    presenter.getInbox(shopID, messages -> {
        UI.getCurrent().access(() -> grid.setItems(messages));
    });
}

    
}
