package Domain.Shop.Policies.Discount;

public enum DiscountKind {
    BASE,
    CONDITIONAL,
    MAX,
    COMBINE;

    public static DiscountKind fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        try {
            return DiscountKind.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid DiscountKind: " + type);
        }
    }

}
