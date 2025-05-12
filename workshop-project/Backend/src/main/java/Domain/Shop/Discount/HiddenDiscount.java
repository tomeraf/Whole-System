package Domain.Shop.Discount;

import java.sql.Date;
import java.time.LocalDate;

public class HiddenDiscount extends OpenDiscount {
    private String couponCode;

    public HiddenDiscount(int id, int percentage, String startDate, String endDate, String couponCode) {
        super(id, percentage, startDate, endDate); // Call the constructor of OpenDiscount
        validateCouponCode(couponCode);
        this.couponCode = couponCode;
    }

    private void validateCouponCode(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code cannot be null or empty.");
        }
        if (couponCode.length() < 5 || couponCode.length() > 20) {
            throw new IllegalArgumentException("Coupon code must be between 5 and 20 characters.");
        }
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        validateCouponCode(couponCode);
        this.couponCode = couponCode;
    }
    public int applyDiscount(int itemId, int price, String providedCouponCode) {
        if (providedCouponCode.equals(this.couponCode) && inTime(LocalDate.now()) && isApplicable(itemId)) {
            return price - (price * getPercentage() / 100);
        } else {
            return price; // No discount applied
        }
    }
}
