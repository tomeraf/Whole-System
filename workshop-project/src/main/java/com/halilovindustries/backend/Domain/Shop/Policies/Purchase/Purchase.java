package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class Purchase {

    @Id
    private int id;

    private double amount;
    private int itemId;
    private int buyerId;

    public Purchase() {
        // Required by JPA
    }

    public Purchase(int id, double amount, int itemId, int buyerId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        this.id = id;
        this.amount = amount;
        this.itemId = itemId;
        this.buyerId = buyerId;
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public int getItemId() {
        return itemId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }
}
