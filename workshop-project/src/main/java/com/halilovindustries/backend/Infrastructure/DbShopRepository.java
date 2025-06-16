package com.halilovindustries.backend.Infrastructure;


import java.util.HashMap;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import com.halilovindustries.backend.Domain.Shop.Shop;
@Primary
@Repository
public class DbShopRepository implements IShopRepository {
    private final JpaShopRepository jpaShopRepository;
    public DbShopRepository(JpaShopRepository jpaShopRepository) {
        this.jpaShopRepository = jpaShopRepository;
    }
    // Implement methods from IShopRepository using jpaShopRepository
    @Override
    public void addShop(Shop shop) {
        jpaShopRepository.save(shop);
    }
    @Override
    public Shop getShopById(int id) {
        return jpaShopRepository.findById(id).orElse(null);
    }
    @Override
    public Shop getShopByName(String name) {
        return jpaShopRepository.findByName(name);
    }
    // @Override
    // public void updateShop(Shop shop) {
    //     jpaShopRepository.save(shop);
    // }
    // @Override
    // public void deleteShop(int id) {
    //     jpaShopRepository.deleteById(id);
    // }
    @Override
    public HashMap<Integer, Shop> getAllShops() {
        List<Shop> shops = jpaShopRepository.findAll();
        HashMap<Integer, Shop> shopMap = new HashMap<>();
        for (Shop shop : shops) {
            shopMap.put(shop.getId(), shop);
        }
        return shopMap;
    }
    @Override
    public List<Shop> getUserShops(int userId) {
        return jpaShopRepository.findByUserId(userId);
    }
    @Override
    public int getNextId() {
        return jpaShopRepository.getNextId()+1;
    }
    @Override
    public int getNextMessageId() {
        return jpaShopRepository.getNextMessageId()+1;
    }


}
