package com.halilovindustries.backend.Domain.Shop.Policies.Discount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.DTOs.DTOtoDomainFactory;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount.CombinedDiscount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import com.halilovindustries.backend.Domain.Shop.Category;

@Entity
public class DiscountPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "discount_types", joinColumns = @JoinColumn(name = "policy_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private List<DiscountType> discountTypes;
    @OneToOne(cascade= CascadeType.ALL, fetch = FetchType.EAGER)
    private CombinedDiscount discounts;
    
    public DiscountPolicy() {
        this.discountTypes = new ArrayList<>();
        this.discountTypes.add(DiscountType.BASE);
        this.discountTypes.add(DiscountType.CONDITIONAL);
        this.discounts = new CombinedDiscount();
    }
    public void updateDiscountType(DiscountType discountType) {
        if(this.discountTypes.contains(discountType)) 
            this.discountTypes.remove(discountType);
        else
            this.discountTypes.add(discountType);
    }
    public void addDiscount(DiscountDTO discountDetails) {
        validateDiscountDetails(discountDetails);
        Discount discount=DTOtoDomainFactory.convertDTO(discountDetails);
        if(discount!=null){
            discounts.addDiscount(discount);
        }
        else{
            throw new IllegalArgumentException("Invalid discount details");
        }
    }
    private void validateDiscountDetails(DiscountDTO discountDetails) {
        if(!discountTypes.contains(discountDetails.getDiscountType())){
            throw new IllegalArgumentException("Invalid discount type: " + discountDetails.getDiscountType());
        }
        if(discountDetails.getDiscountType2()!=null && !discountTypes.contains(discountDetails.getDiscountType2())){
            throw new IllegalArgumentException("Invalid discount type: " + discountDetails.getDiscountType2());
        }
        
    }
    public void removeDiscount(int discountId) {
        discounts.removeDiscount(discountId);
    }
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        return discounts.calculateDiscount(allItems);
    }
    public List<Integer> getDiscountIds() {
        return discounts.getIds();
    }
    public List<DiscountDTO> getDiscounts() {
        return discounts.getDiscountsList().stream().map(discount->{
        DiscountDTO dis=new DiscountDTO(discount.getDiscountKind(),discount.getItemId(),discount.getCategory(),discount.getPercentage(),discount.getCondition(),discount.getItemId2(),discount.getCategory2(),discount.getPercentage2(),discount.getCondition2(),discount.getDiscountType(),discount.getDiscountType2());
        dis.setId(discount.getDiscountId());
        return dis;
    }).toList();  
    }
    public List<DiscountType> getDiscountTypes() {
        return discountTypes;
    }
    public HashMap<Item, Double> getPricePerItem(HashMap<Item, Integer> allItems) {
        HashMap<Item,Double> percentages = discounts.getPercentagePerItem(allItems);
        HashMap<Item, Double> prices = new HashMap<>();
        for (Item item : allItems.keySet()) {
            if (percentages.containsKey(item)) {
                prices.put(item, percentages.get(item) * item.getPrice());
            }
            else {
                prices.put(item, item.getPrice());
            }
        }
        return prices;
    }


}
