package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.DTOs.Order;
import com.halilovindustries.backend.Domain.User.IRole;
import com.halilovindustries.backend.Domain.User.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOrderAdapter extends JpaRepository<Order, Integer>{
    List<Order> findByUserId(int userID);
}
