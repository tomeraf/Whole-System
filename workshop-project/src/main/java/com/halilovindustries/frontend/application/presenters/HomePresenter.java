package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.DTOs.ItemDTO;
import com.halilovindustries.backend.Domain.DTOs.ShopDTO;
import com.halilovindustries.backend.Service.ShopService;
import com.halilovindustries.backend.Service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class HomePresenter {
    UserService userService;
    ShopService shopService;
    List<ShopDTO> RandomShops;


    public HomePresenter(UserService userService, ShopService shopService) {
        this.userService = userService;
        this.shopService = shopService;
        rnd3Shops();
    }

    private void rnd3Shops() {
        List<ShopDTO> shops = shopService.showAllShops().getData();
        Random rand = new Random();
        if (shops.isEmpty())
            return;
        int rndNum = rand.nextInt(shops.size());
        RandomShops = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            RandomShops.add(shops.get(rndNum++ % shops.size()));
    }

    @Bean
    public List<ShopDTO> getRandomShops() {
        return RandomShops;
    }

    @Bean
    public List<ItemDTO> get4rndShopItems(ShopDTO shop) {
        List<ItemDTO> RandomItems = new ArrayList<>();
        Random rand = new Random();
        if (RandomShops.isEmpty())
            return RandomItems;
        Object[] keys = shop.getItems().keySet().toArray();
        int rndNum = rand.nextInt(keys.length);
        for (int i = 0; i < 4; i++)
            RandomItems.add(shop.getItems().get((Integer)(keys[rndNum%keys.length])));
        return RandomItems;
    }



}