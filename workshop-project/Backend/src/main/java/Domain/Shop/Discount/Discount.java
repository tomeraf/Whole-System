package Domain.Shop.Discount;

import java.util.HashMap;

import Domain.Shop.*;

public abstract class Discount {
    private int discountId;
    public Discount(int discountId) {
        this.discountId = discountId;
    }
    public int getDiscountId() {
        return discountId;
    }
    public abstract double calculateDiscount(HashMap<Item,Integer> allItems);


}
