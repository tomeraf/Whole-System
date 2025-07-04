package com.halilovindustries.backend.Infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.DTOs.BasketDTO;
import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryOrderRepository implements IOrderRepository {
    private HashMap<Integer, Order> orders = new HashMap<>();

    @Override
    public void addOrder(Order order) {
        orders.put(order.getId(), order);
    }

    // @Override
    // public void removeOrder(int orderId) {
    //     if(!orders.containsKey(orderId)) {
    //         throw new IllegalArgumentException("Order with ID " + orderId + " does not exist.");
    //     }
    //     orders.remove(orderId);
    // }

    @Override
    public Order getOrder(int orderId) {
        if(!orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order with ID " + orderId + " does not exist.");
        }
        return orders.get(orderId);
    }

    // @Override
    // public List<Order> getAllOrders() {
    //     return orders.values().stream().toList();
    // }

    @Override
    public HashMap<Integer,List<ItemDTO>> getOrdersByShopId(int shopId) {
        HashMap<Integer, List<ItemDTO>> shopOrders = new HashMap<>();
        for (Order order : orders.values()) {
            List<ItemDTO> items = order.getItemsByShopId(shopId);
            if (!items.isEmpty()) {
                shopOrders.put(order.getId(), items);
            }
        }
        return shopOrders;
    }
    public List<Order> getOrdersByCustomerId(int userID) {
        List<Order> orderList = new ArrayList<>();
        for (Order order : orders.values()) {
            if (order.getUserID() == userID) {
                orderList.add(order);
            }
        }
        return orderList;
    }
    @Override
    public int getNextId() {
        return orders.size();
    }


}