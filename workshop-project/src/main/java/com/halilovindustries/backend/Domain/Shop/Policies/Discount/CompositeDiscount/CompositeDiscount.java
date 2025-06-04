package com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.Discount;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import com.halilovindustries.backend.Domain.Shop.Category;


@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "composite_type", discriminatorType = jakarta.persistence.DiscriminatorType.STRING)
public abstract class CompositeDiscount extends Discount {
    @OneToMany(cascade = CascadeType.ALL)
    private List<Discount> discounts;

    private String firstId;
    private String secondId;
    public CompositeDiscount() {
        this.discounts = new ArrayList();
    }
    public CompositeDiscount(Discount discount1, Discount discount2) {
        this.discounts = new ArrayList<>();
        discounts.add(discount1);
        discounts.add(discount2);
        this.firstId = discount1.getId();
        this.secondId = discount2.getId();
        
    }
    public List<Discount> getDiscounts() {
        return discounts;
    }
    public List<Discount> getDiscountsList() {
        return new ArrayList<>(discounts);
    }
    public void addDiscount(Discount discount) {
        if (discounts.contains(discount)) {
            throw new IllegalArgumentException("Discount already exists");
        }
        discounts.add(discount);
    }
    public void removeDiscount(String discountId) {
        if (!discounts.removeIf(discount -> discount.getId().equals(discountId))) {
            throw new IllegalArgumentException("Discount with id " + discountId + " does not exist");
        }
    }

    public List<String> getIds() {
        return new ArrayList<>(discounts.stream()
                .map(Discount::getId)
                .toList());
    }
    private Discount getDiscountById(String id) {
        return discounts.stream()
                .filter(discount -> discount.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Discount with id " + id + " does not exist"));
    }
    @Override
    public DiscountType getDiscountType() { return getDiscountById(firstId).getDiscountType(); }
    @Override
    public int getItemId() { return getDiscountById(firstId).getItemId(); }
    @Override
    public Category getCategory() { return getDiscountById(firstId).getCategory(); }
    @Override
    public int getPercentage() { return getDiscountById(firstId).getPercentage(); }
    @Override
    public ConditionDTO getCondition() { return getDiscountById(firstId).getCondition(); }
    @Override
    public DiscountType getDiscountType2() { return getDiscountById(secondId).getDiscountType(); }
    @Override
    public int getItemId2() { return getDiscountById(secondId).getItemId(); }
    @Override
    public Category getCategory2() { return getDiscountById(secondId).getCategory(); }
    @Override
    public int getPercentage2() { return getDiscountById(secondId).getPercentage(); }
    @Override
    public ConditionDTO getCondition2() { return getDiscountById(secondId).getCondition(); }
}
