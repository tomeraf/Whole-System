package Domain.Shop.Policies.Discount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Domain.DTOs.DTOtoDomainFactory;
import Domain.DTOs.DiscountDTO;
import Domain.Shop.Item;
import Domain.Shop.Policies.Discount.BaseDiscount.*;
import Domain.Shop.Policies.Discount.CompositeDiscount.CombinedDiscount;
import Domain.Shop.Category;

public class DiscountPolicy {
    private List<DiscountType> discountTypes;
    private CombinedDiscount discounts;
    
    public DiscountPolicy(){
        this.discountTypes = new ArrayList<>();
        this.discounts = new CombinedDiscount();
    }
    public void updateDiscountType(DiscountType discountType) {
        if(this.discountTypes.contains(discountType)) 
            this.discountTypes.remove(discountType);
        else
            this.discountTypes.add(discountType);
    }
    public void addDiscount(DiscountDTO discountDetails) {
        Discount discount=DTOtoDomainFactory.convertDTO(discountDetails);
        if(discount!=null){
            discounts.addDiscount(discount);
        }
        else{
            throw new IllegalArgumentException("Invalid discount details");
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


}
