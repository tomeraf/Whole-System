package com.halilovindustries.frontend.application.views;

import org.springframework.beans.factory.annotation.Autowired;

import com.halilovindustries.backend.Domain.Message;
import com.halilovindustries.backend.Domain.OfferMessage;
import com.halilovindustries.frontend.application.presenters.InboxPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.halilovindustries.backend.Domain.Message;
import com.vaadin.flow.router.HasUrlParameter;

@Route(value = "inbox", layout = MainLayout.class)
@PageTitle("Inbox")
public class InboxView extends VerticalLayout{ 
    private InboxPresenter presenter;
    private Grid<Message> grid;
    private Integer shopID;
    private final Button back = new Button("← Back",
                                         e -> UI.getCurrent().navigate(""));

    @Autowired
    public InboxView(InboxPresenter presenter) {
        this.presenter = presenter;
        setPadding(true);
        setSpacing(true);

        // — Back button —
        HorizontalLayout backLayout = new HorizontalLayout(back);
        add(backLayout);
        // — Title —
        add(new H2("Inbox"));

        // — Search bar (no filter) —
        HorizontalLayout searchRow = new HorizontalLayout(createSearchBar());
        searchRow.setWidthFull();
        searchRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        add(searchRow);

        // — Conversations list skeleton —
        grid = new Grid<>(Message.class, false);
        // // “Open” arrow column
        // grid.addComponentColumn(v -> {
        //             Button open = new Button(VaadinIcon.CHEVRON_RIGHT.create());
        //             open.addThemeVariants(ButtonVariant.LUMO_ICON);
        //             return open;
        //         })
        //         .setHeader("")  // blank header
        //         .setAutoWidth(true);

        grid.addColumn(msg -> msg.isFromUser() ? msg.getUserName() : msg.getShopName())
            .setHeader("From")
            .setAutoWidth(true);
        grid.addColumn(Message::getTitle)
            .setHeader("Subject")
            .setAutoWidth(true);
        grid.addColumn(msg -> msg.getDateTime().toString().substring(0, 19))
            .setHeader("Date")
            .setAutoWidth(true);

        //Show the content in a dialog when they click a row
        grid.asSingleSelect().addValueChangeListener(e -> {
            Message msg = e.getValue();
            if (msg == null) return;

            Dialog dlg = new Dialog(new H2(msg.getTitle()),
                                    new Paragraph(msg.getContent()));

            if (msg.isOffer() && ((OfferMessage)msg).getDecision() == null) {
                Button accept = new Button(VaadinIcon.CHECK.create(), ev -> {
                    presenter.respondToOffer(
                        msg.getShopName(), 
                        msg.getId(), 
                        true, 
                        success -> {
                            if (success) {
                                ((OfferMessage) msg).setDecision(true);
                                dlg.close();
                                Notification.show("Offer accepted", 2000, Position.MIDDLE);
                                grid.getDataProvider().refreshItem(msg);
                            }
                        }
                    );
                });
                accept.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

                Button reject = new Button(VaadinIcon.CLOSE.create(), ev -> {
                    presenter.respondToOffer(
                        msg.getShopName(),
                        msg.getId(),
                        false,
                        success -> {
                            if (success) {
                                ((OfferMessage) msg).setDecision(false);
                                dlg.close();
                                Notification.show("Offer rejected", 2000, Position.MIDDLE);
                                grid.getDataProvider().refreshItem(msg);
                            }
                        }
                    );
                });
                reject.addThemeVariants(ButtonVariant.LUMO_ERROR);

                dlg.add(new HorizontalLayout(accept, reject));
            }
            else if (msg.isOffer() && ((OfferMessage)msg).getDecision() != null) {
                String decisionText = ((OfferMessage)msg).getDecision() != null && ((OfferMessage)msg).getDecision()
                                  ? "You accepted this offer"
                                  : "You rejected this offer";
                dlg.add(new Paragraph(decisionText));
                dlg.add(new Button("Close", ev -> dlg.close()));
            }
            //User-to-shop conversation → Reply
            else if (msg.needResponse() && msg.isFromUser()) {
                Button reply = new Button("Reply", ev -> {
                    dlg.close();
                    openMessageDialog();  // your existing reply-dialog helper
                });
                reply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                dlg.add(reply);
            }
            // Anything else → just Close
            else {
                dlg.add(new Button("Close", ev -> dlg.close()));
            }

            dlg.open();
        });


        add(grid);
        setSizeFull();

        //Load the real messages
        presenter.getInbox(messages -> {
            UI.getCurrent().access(() -> grid.setItems(messages));
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

        presenter.sendMessege(shopID, subject, message, success -> {
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

    
    
}