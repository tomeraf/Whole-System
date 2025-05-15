package com.halilovindustries.frontend.application.views;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.frontend.application.presenters.CartPresenter;
import com.halilovindustries.frontend.application.presenters.HomePresenter;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import java.util.List;
@Route("cart")
@PageTitle("Your Cart")
public class CartView extends Composite<VerticalLayout>
                       implements BeforeEnterObserver {
  private final CartPresenter presenter;
  private final Grid<ItemDTO> grid = new Grid<>(ItemDTO.class);
  private final Button back = new Button("← Back", e -> UI.getCurrent().navigate(""));

  @Autowired
  public CartView(CartPresenter presenter) {
    this.presenter = presenter;
    grid.removeAllColumns();
    grid.addColumn(ItemDTO::getName).setHeader("Name");
    grid.addColumn(ItemDTO::getQuantity).setHeader("Qty");
    grid.addColumn(ItemDTO::getPrice).setHeader("Price");

    getContent().add(back, grid);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    presenter.getSessionToken(token -> {
      System.out.println("Token: " + token);
      if (token == null || !presenter.validateToken(token)) {
        Notification.show("Please log in first", 2000, Position.MIDDLE);
        event.rerouteTo(HomePageView.class);
        return;
      }

//      List<ItemDTO> items = presenter.getCartContent(token);
//      grid.setItems(items);
//      if (items.isEmpty()) {
//        Notification.show("Your cart is empty", 2000, Position.MIDDLE);
//      }
    }
    );
  }
}