package Domain.Shop.Discount.CompositeDiscount;

import java.util.HashMap;

import Domain.Shop.Item;
import Domain.Shop.Discount.Discount;

public class CombinedDiscount extends CompositeDiscount {
    public CombinedDiscount(int discountId) {
        super(discountId);
    }
    public CombinedDiscount(int id, Discount discount1, Discount discount2) {
        super(id, discount1, discount2);
    }

    @Override
    public double calculateDiscount(HashMap<Item, Integer> allItems) {
        double totalDiscount = 0;
        for (Discount discount : getDiscounts().values()) {
            totalDiscount += discount.calculateDiscount(allItems);
        }
        return totalDiscount;
    }
}
