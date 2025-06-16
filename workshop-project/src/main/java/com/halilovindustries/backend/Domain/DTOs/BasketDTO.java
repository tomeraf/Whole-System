package com.halilovindustries.backend.Domain.DTOs;

import java.time.LocalDateTime;
import java.util.List;

public class BasketDTO {
    private int orderId;
    private String username;
    private List<ItemDTO> items;

    public BasketDTO(int orderId,String username, List<ItemDTO> items) {
        this.orderId = orderId;
        this.username = username;
        this.items = items;
    }
    public String getUsername() {
        return username;
    }
    public List<ItemDTO> getItems() {
        return items;
    }
    public int getOrderId() {
        return orderId;
    }


}
