package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class HomePresenter {
    private final UserService userService;
    private final ShopService shopService;
    private List<ShopDTO> randomShops = new ArrayList<>();

    @Autowired
    public HomePresenter(UserService userService, ShopService shopService) {
        this.userService = userService;
        this.shopService = shopService;
        rnd3Shops();
    }

    private void rnd3Shops() {
//        List<ShopDTO> shops = shopService.showAllShops().getData();
//        Random rand = new Random();
//        if (shops.isEmpty())
//            return;
//        int rndNum = rand.nextInt(shops.size());
//        RandomShops = new ArrayList<>();
//        for (int i = 0; i < 3; i++)
//            RandomShops.add(shops.get(rndNum++ % shops.size()));

        List<ShopDTO> shops = shopService.showAllShops().getData();
        randomShops.clear();
        if (shops == null || shops.isEmpty()) return;

        Random rand = new Random();
        int start = rand.nextInt(shops.size());
        for (int i = 0; i < 3; i++) {
            randomShops.add(shops.get((start + i) % shops.size()));
        }
    }


    public List<ShopDTO> getRandomShops() {
        return randomShops;
    }


    public List<ItemDTO> get4rndShopItems(ShopDTO shop) {
//        List<ItemDTO> randomItems = new ArrayList<>();
//        Random rand = new Random();
//        if (randomShops.isEmpty())
//            return randomItems;
//        Object[] keys = shop.getItems().keySet().toArray();
//        int rndNum = rand.nextInt(keys.length);
//        for (int i = 0; i < 4; i++)
//            randomItems.add(shop.getItems().get((Integer)(keys[rndNum%keys.length])));
//        return randomItems;

        List<ItemDTO> randomItems = new ArrayList<>();
        if (shop.getItems() == null || shop.getItems().isEmpty())
            return randomItems;

        Object[] keys = shop.getItems().keySet().toArray();
        Random rand = new Random();
        int start = rand.nextInt(keys.length);
        for (int i = 0; i < 4; i++) {
            int idx = (start + i) % keys.length;
            randomItems.add(shop.getItems().get((Integer)keys[idx]));
        }
        return randomItems;
    }



}