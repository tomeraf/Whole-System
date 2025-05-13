package com.halilovindustries.frontend.application.views.homepage;

import com.halilovindustries.frontend.application.views.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

// 1️⃣ Register under “shops” and tie it to MainLayout
@Route(value = "My Shops", layout = MainLayout.class)
public class MyShopsView extends VerticalLayout {
    public MyShopsView() {
        setPadding(true);
        add(new H2("My Shops"));
        // … later: pull your shop-list component here
    }
}
