package com.halilovindustries.backend.Infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.halilovindustries.backend.Domain.DTOs.Order;


public interface JpaOrderRepository extends JpaRepository<Order,Integer> {
    @Query("SELECT o FROM Order o WHERE o.userId = ?1")
    public List<Order> findByUserId(int userId);

    @Query("SELECT COALESCE(MAX(o.orderID), -1) FROM Order o")
    public int getNextId();
}
