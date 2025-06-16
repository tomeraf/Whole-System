package com.halilovindustries.backend.Domain.Repositories;

import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.DTOs.*;


public interface IOrderRepository {
    void addOrder(Order order);
    // void removeOrder(int orderId);
    Order getOrder(int orderId);
    // List<Order> getAllOrders();
    List<Order> getOrdersByCustomerId(int userID);
    HashMap<Integer,List<ItemDTO>> getOrdersByShopId(int shopId);//<orderId, List<ItemDTO>>
    int getNextId();
}