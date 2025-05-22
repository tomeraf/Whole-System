package com.halilovindustries.backend.Domain.Repositories;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashMap;
import java.util.List;

public interface IOrderRepository extends JpaRepository<Order, Integer> {

    // You already get these from JpaRepository:
    // - save(Order order)
    // - deleteById(Integer id)
    // - findById(Integer id)
    // - findAll()

    //past:
    //void addOrder(Order order);
    //void removeOrder(int orderId);
    //Order getOrder(int orderId);
    //HashMap<Integer,Order> getAllOrders();
    //List<Order> getOrdersByCustomerId(int userID);
    //<ItemDTO> getOrdersByShopId(int shopId);

    // Now define only custom query methods:

    List<Order> findByCustomerId(int userID); // Spring will auto-implement this

    @Query("SELECT new com.halilovindustries.backend.Domain.DTOs.ItemDTO(i) " +
            "FROM Order o JOIN o.items i WHERE o.shop.id = :shopId")
    List<ItemDTO> findItemsByShopId(@Param("shopId") int shopId);
}
