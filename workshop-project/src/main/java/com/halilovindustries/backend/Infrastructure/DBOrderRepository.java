package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import java.util.*;

@Primary
@Repository
public class DBOrderRepository implements IOrderRepository {
    private final JpaOrderAdapter jpaAdapter;

    @Autowired
    public DBOrderRepository(JpaOrderAdapter jpaAdapter) {
        this.jpaAdapter = jpaAdapter;
    }

    @Override
    public void addOrder(Order order) {
        Optional<Order> existing = jpaAdapter.findById(order.getId());
        if (existing.isPresent())
            throw new RuntimeException("Order already exists in db!");

        jpaAdapter.save(order);
    }

    @Override
    public void removeOrder(int orderId) {
        Optional<Order> existing = jpaAdapter.findById(orderId);
        if (existing.isEmpty())
            throw new RuntimeException("Order doesn't exists in db!");

        jpaAdapter.deleteById(orderId);
    }

    @Override
    public Order getOrder(int orderId) {
        Optional<Order> existing = jpaAdapter.findById(orderId);
        if (existing.isEmpty())
            throw new RuntimeException("Order doesn't exists in db!");

        return existing.get();
    }

    @Override
    public HashMap<Integer, Order> getAllOrders() {
        List<Order> orders = jpaAdapter.findAll();
        HashMap<Integer, Order> map = new HashMap<>();
        for (Order order : orders)
            map.put(order.getId(), order);

        return map;
    }

    @Override
    public List<Order> getOrdersByCustomerId(int userID) {
        List<Order> orders = new ArrayList<>();
        List<Order> all = jpaAdapter.findAll();
        for (Order order : all)
            if(order.getUserID() == userID)
                orders.add(order);

        return orders;
    }

    @Override
    public List<Order> getOrdersByShopId(int shopId) {
        List<Order> orders = new ArrayList<>();
        List<Order> all = jpaAdapter.findAll();
        for (Order order : all)
            if(!order.getShopItems(shopId).isEmpty())
                orders.add(order);

        return orders;
    }
}