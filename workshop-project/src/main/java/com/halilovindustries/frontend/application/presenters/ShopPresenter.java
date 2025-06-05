package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Response;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.JWTAdapter;
import com.halilovindustries.backend.Domain.DTOs.AuctionDTO;
import com.halilovindustries.backend.Domain.DTOs.BidDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Service.OrderService;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import com.vaadin.flow.component.UI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Component
public class ShopPresenter extends AbstractPresenter {
    @Autowired
    public ShopPresenter(
        UserService userService,
        ShopService shopService,
        JWTAdapter jwtAdapter,
        OrderService orderService     // ← add here
    ) {
        this.userService   = userService;
        this.shopService   = shopService;
        this.jwtAdapter    = jwtAdapter;
        this.orderService  = orderService;           // ← assign
    }
    public void getItems(int shopId, String itemName, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }
            // 2) Call the backend
            Response<List<ItemDTO>> response = shopService.showShopItems(token, shopId);
            
            if (!response.isOk()) {
                Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
                return;
            }
            else {
                    Notification.show("Items loaded successfully!", 2000, Position.MIDDLE);
            }
            onFinish.accept(response.getData());
            });
        });
    }

    public void getItemByFilter(int shopId, String itemName, HashMap<String, String> filters, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }
            Response<List<ItemDTO>> response = shopService.filterItemsInShop(token, 0, filters);
            if (!response.isOk()) {
                UI.getCurrent().access(() ->
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE)
                );
                onFinish.accept(null);
                return;
            }
            else {
                    Notification.show("Items loaded successfully!", 2000, Position.MIDDLE);
            }
            onFinish.accept(response.getData());
            });
        });
    }

    public void submitBidOffer(int shopId, int itemID, double offerPrice, Consumer<Boolean> onFinish) {
       getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = orderService.submitBidOffer(token, shopId, itemID, offerPrice);
            if (!response.isOk()) {
                    Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            else {
                    Notification.show("Bid offer submitted successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
            }
            });
        });
    }

    public void purchaseBidItem(int shopId, int bidId, PaymentDetailsDTO paymentDetalis, ShipmentDetailsDTO shipmentDetalis, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = orderService.purchaseBidItem(token, shopId, bidId, paymentDetalis, shipmentDetalis);
            if (!response.isOk()) {
                Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            else {
                    Notification.show("Bid item purchased successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
            }
            });
        });
    }

    
    public void answerOnCounterBid(int shopId, int bidId, boolean accept, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = orderService.answerOnCounterBid(token, shopId, bidId, accept);
            if (!response.isOk()) {
                Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            else {
                    Notification.show("Answered On Counter Bid successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
            }
            });
        });
    }
      
    public void submitAuctionOffer(int shopId, int auctionID, double offerPrice, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = orderService.submitAuctionOffer(token, shopId, auctionID, offerPrice);
            if (!response.isOk()) {

                Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);

                onFinish.accept(false);
            }
            else {
                    Notification.show("Auction offer submitted successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
            }
            });
        });
    }
    
    public void purchaseAuctionItem(int shopId, int auctionID, PaymentDetailsDTO paymentDetalis, ShipmentDetailsDTO shipmentDetalis, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }

            Response<Void> resp = orderService.purchaseAuctionItem(token, shopId, auctionID, paymentDetalis, shipmentDetalis);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
            } else {
                Notification.show("Auction item purchased successfully!", 2000, Position.MIDDLE);
                onFinish.accept(true);
            }
            });
        });        
    } 

    public void addItemToCart(int shopId,int itemId,int quantity, Consumer<Boolean> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            Response<Void> response = orderService.addItemToCart(token, shopId, itemId,quantity);
            if (!response.isOk()) {
                Notification.show("Error: " + response.getError(), 2000, Position.MIDDLE);
                onFinish.accept(false);
                return;
            }
            else {
                    Notification.show("Items added to cart successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(true);
            }
            });
        });
    }

    public void showShopItems(int shopId, Consumer<List<ItemDTO>> onFinish) {
        getSessionToken(token -> {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.access(() -> {
            if (token == null || !validateToken(token)) {
                Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                onFinish.accept(null);
                return;
            }

            Response<List<ItemDTO>> resp = shopService.showShopItems(token, shopId);
            if (!resp.isOk()) {
                Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                onFinish.accept(null);
            } else {
                Notification.show("Items retrieved successfully!", 2000, Position.MIDDLE);
                onFinish.accept(resp.getData());
            }
            });
        });        
    }
    
    public void getShopInfo(int shopID, Consumer<ShopDTO> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token)) {
                    Notification.show("No session token found, please reload.", 2000, Notification.Position.MIDDLE);
                    onFinish.accept(null);
                    return;
                }

                Response<ShopDTO> resp = shopService.getShopInfo(token, shopID);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(null);
                } else {
                    Notification.show("Shop retrieved successfully!", 2000, Position.MIDDLE);
                    onFinish.accept(resp.getData());
                }
            });
        });        
    }

    public void getActiveAuctions(int shopID, Consumer<List<AuctionDTO>> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;
            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    onFinish.accept(List.of());
                    return;
                }
                Response<List<AuctionDTO>> res = shopService.getActiveAuctions(token, shopID);
                if (!res.isOk()) {
                    Notification.show("Error: " + res.getError(), 2000, Position.MIDDLE);
                    onFinish.accept(List.of());
                } else {
                    onFinish.accept(res.getData());
                }
            });
        });
    }
    public void getWonAuctions(int shopId, Consumer<List<AuctionDTO>> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    return;
                }

                Response<List<AuctionDTO>> resp = shopService.getWonAuctions(token, shopId);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                } else {
                    onFinish.accept(resp.getData());
                }
            });
        });
    }

    public void getUserBids(int shopId, Consumer<List<BidDTO>> onFinish) {
        getSessionToken(token -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;

            ui.access(() -> {
                if (token == null || !validateToken(token) || !isLoggedIn(token)) {
                    Notification.show("No session token found, please reload.", 2000, Position.MIDDLE);
                    return;
                }

                Response<List<BidDTO>> resp = shopService.getUserBids(token, shopId);
                if (!resp.isOk()) {
                    Notification.show("Error: " + resp.getError(), 2000, Position.MIDDLE);
                } else {
                    onFinish.accept(resp.getData());
                }
            });
        });
    }
}