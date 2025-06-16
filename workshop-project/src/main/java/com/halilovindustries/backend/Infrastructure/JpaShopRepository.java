package com.halilovindustries.backend.Infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.halilovindustries.backend.Domain.Shop.Shop;

public interface JpaShopRepository extends JpaRepository<Shop, Integer>{
    @Query("SELECT s FROM Shop s WHERE s.name = :name")
    Shop findByName(@Param("name") String name);
    @Query("SELECT s FROM Shop s WHERE :userId IN elements(s.ownerIDs) OR :userId IN elements(s.managerIDs)")
    List<Shop> findByUserId(@Param("userId") int userId);
    @Query("SELECT COALESCE(MAX(s.id), -1) FROM Shop s")
    int getNextId();
    @Query("SELECT COALESCE(MAX(m.id), -1) FROM Message m")
    int getNextMessageId();
}
