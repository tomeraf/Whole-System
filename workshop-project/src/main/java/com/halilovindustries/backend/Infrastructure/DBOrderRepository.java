package com.halilovindustries.backend.Infrastructure;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.Repositories.IOrderRepository;

@Primary
@Repository
public class DBOrderRepository implements IOrderRepository{
    private final JpaOrderRepository jpaOrderRepository;
    @Autowired
    public DBOrderRepository(JpaOrderRepository jpaOrderRepository) {
        this.jpaOrderRepository = jpaOrderRepository;
    }
    @Override
    public void addOrder(Order order) {
        jpaOrderRepository.save(order);
    }
    
    // @Override
    // public void removeOrder(int orderId) {
    //     jpaOrderRepository.deleteById(orderId);
    // }
    // @Override
    // public Order getOrder(int orderId) {
    //     return jpaOrderRepository.findById(orderId).orElse(null);
    // }
    // @Override
    // public List<Order> getAllOrders(){
    //     return jpaOrderRepository.findAll();
    // }
    @Override
    public List<Order> getOrdersByCustomerId(int userID){
        return jpaOrderRepository.findByUserId(userID);
    }
    // @Override
    // public List<ItemDTO> getOrdersByShopId(int shopId) {
    //     List<Order> orders = jpaOrderRepository.findAll();
    //     HashMap<Integer, List<ItemDTO>> itemsMap = new HashMap<>();
    //     for (Order order : orders) {
    //         List<ItemDTO> items = order.getShopItems(shopId);
    //         if (items != null && !items.isEmpty()) {
    //             itemsMap.put(order.getId(), items);
    //         }
    //     }
    //     return itemsMap.getOrDefault(shopId, List.of());
    // }
    @Override
    public int getNextId(){
        return jpaOrderRepository.getNextId()+1;
    }



}
