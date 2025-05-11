package com.halilovindustries.backend.Domain.Repositories;

import com.halilovindustries.backend.Domain.Shop;

import java.util.HashMap;

public interface IShopRepository {
    void addShop(Shop shop);
    Shop getShopById(int id);
    void updateShop(Shop shop);
    void deleteShop(int id);
    HashMap<Integer,Shop> getAllShops();

}
