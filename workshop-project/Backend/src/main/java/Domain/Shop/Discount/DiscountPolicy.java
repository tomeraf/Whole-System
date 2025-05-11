package Domain.Shop.Discount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Domain.DTOs.DTOtoDomainFactory;
import Domain.DTOs.DiscountDTO;
import Domain.Shop.Item;
import Domain.Shop.Discount.BaseDiscount.*;
import Domain.Shop.Discount.CompositeDiscount.CombinedDiscount;
import Domain.Shop.Category;

public class DiscountPolicy {
    private int discountId;
    private List<DiscountType> discountTypes;
    private CombinedDiscount discounts;
    
    public DiscountPolicy(){
        this.discountTypes = new ArrayList<>();
        this.discounts = new CombinedDiscount(0);
        this.discountId = 1;
    }
    public void updateDiscountType(DiscountType discountType) {
        if(this.discountTypes.contains(discountType)) 
            this.discountTypes.remove(discountType);
        else
            this.discountTypes.add(discountType);
    }
    public void addDiscount(DiscountDTO discountDetails) {
        Discount discount=DTOtoDomainFactory.convertDTO(discountId, discountDetails);
        if(discount!=null){
            discounts.addDiscount(discount);
            discountId++;
        }
        else{
            throw new IllegalArgumentException("Invalid discount details");
        }
    }
    public void removeDiscount(int discountId) {
        discounts.removeDiscount(discountId);
    }


}
