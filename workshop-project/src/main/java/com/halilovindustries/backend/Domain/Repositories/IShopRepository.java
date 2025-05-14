package com.halilovindustries.backend.Domain.Repositories;

import com.halilovindustries.backend.Domain.Shop.Shop;

import java.util.HashMap;
import java.util.List;

public interface IShopRepository {
    void addShop(Shop shop);
    Shop getShopById(int id);
    void updateShop(Shop shop);
    void deleteShop(int id);
    HashMap<Integer,Shop> getAllShops();
    List<Shop> getUserShops(int userId);

}
