package com.halilovindustries.backend.Domain.Shop;

import java.io.Serializable;
import java.util.Objects;


public class ShopKey implements Serializable {
    private int id;
    private int shopId;

    public ShopKey() {}

    public ShopKey(int id, int shopId) {
        this.id = id;
        this.shopId = shopId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopKey)) return false;
        ShopKey that = (ShopKey) o;
        return id == that.id && shopId == that.shopId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shopId);
    }

    public int getId() {
        return id;
    }

    public int getShopId() {
        return shopId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setShopId(int shopId) {
        this.shopId = shopId;
    }
}
