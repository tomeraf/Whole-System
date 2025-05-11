package Domain.Shop.Policies.Discount;

public enum DiscountType {
    BASE,
    CONDITIONAL;

    public static DiscountType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        try {
            return DiscountType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid DiscountType: " + type);
        }
    }
}


