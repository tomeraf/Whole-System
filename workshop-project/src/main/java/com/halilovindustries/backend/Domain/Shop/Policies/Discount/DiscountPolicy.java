package com.halilovindustries.backend.Domain.Shop.Policies.Discount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.halilovindustries.backend.Domain.DTOs.DTOtoDomainFactory;
import com.halilovindustries.backend.Domain.DTOs.DiscountDTO;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount.CombinedDiscount;
import com.halilovindustries.backend.Domain.Shop.Category;

public class DiscountPolicy {
    private List<DiscountType> discountTypes;
    private CombinedDiscount discounts;
    
    public DiscountPolicy(){
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


}
