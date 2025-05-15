package com.halilovindustries.backend.Infrastructure;

import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.Shop.Shop;
import com.halilovindustries.backend.Domain.Repositories.IShopRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryShopRepository implements IShopRepository {
    private HashMap<Integer, Shop> shops = new HashMap<>();

    @Override
    public void addShop(Shop shop) {
        shops.put(shop.getId(), shop);
    }

    @Override
    public Shop getShopById(int id) {
        if (!shops.containsKey(id)) {
            throw new IllegalArgumentException("Shop with ID " + id + " does not exist.");
        }
        return shops.get(id);
    }

    @Override
    public void updateShop(Shop shop) {
        shops.put(shop.getId(), shop);
    }

    @Override
    public void deleteShop(int id) {
        if (!shops.containsKey(id)) {
            throw new IllegalArgumentException("Shop with ID " + id + " does not exist.");
        }
        shops.remove(id);
    }

    @Override
    public HashMap<Integer, Shop> getAllShops() {
        return shops;
    }
    @Override
    public List<Shop> getUserShops(int userId) {
        List<Shop> userShops = shops.values().stream()
                .filter(shop -> shop.isShopMember(userId))
                .toList();
        return userShops;
    }
    @Override
    public Shop getShopByName(String name) {
        for (Shop shop : shops.values()) {
            if (shop.getName().equals(name)) {
                return shop;
            }
        }
        return null;
    }

}
