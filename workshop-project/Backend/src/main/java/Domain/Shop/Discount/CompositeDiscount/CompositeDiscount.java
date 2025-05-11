package Domain.Shop.Discount.CompositeDiscount;

import java.util.HashMap;

import Domain.Shop.Discount.Discount;

public abstract class CompositeDiscount extends Discount {
    private HashMap<Integer,Discount> discounts;
    public CompositeDiscount(int discountId) {
        super(discountId);
        this.discounts = new HashMap<>();
    }
    public CompositeDiscount(int id,Discount discount1, Discount discount2) {
        super(id);
        this.discounts = new HashMap<>();
        discounts.put(discount1.getDiscountId(), discount1);
        discounts.put(discount2.getDiscountId(), discount2);
        
    }
    public HashMap<Integer, Discount> getDiscounts() {
        return discounts;
    }
    public void addDiscount(Discount discount) {
        if(discounts.containsKey(discount.getDiscountId())){
            throw new IllegalArgumentException("Discount already exists");
        }
        discounts.put(discount.getDiscountId(), discount);
    }
    public void removeDiscount(int discountId) {
        if(discounts.containsKey(discountId)){
            discounts.remove(discountId);
        }
        else{
            throw new IllegalArgumentException("Discount not found");
        }
    }


}
