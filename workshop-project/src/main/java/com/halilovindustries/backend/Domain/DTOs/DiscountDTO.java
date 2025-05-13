package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Category;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountKind;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.DiscountType;

public class DiscountDTO {
    private DiscountKind discount_kind;
    private DiscountType discountType1;
    private int itemId;
    private Category category;
    private int percentage;
    private ConditionDTO condition;
    private DiscountType discountType2;
    private int itemId2;
    private Category category2;
    private int percentage2;
    private ConditionDTO condition2;

    public DiscountDTO(DiscountKind discount_kind, int itemId, Category category, int percentage, ConditionDTO condition, int itemId2, Category category2, int percentage2, ConditionDTO condition2, DiscountType discountType1, DiscountType discountType2) {
        this.discount_kind = discount_kind;
        this.itemId = itemId;
        this.category = category;
        this.percentage = percentage;
        this.condition = condition;
        this.itemId2 = itemId2;
        this.category2 = category2;
        this.percentage2 = percentage2;
        this.condition2 = condition2;
        this.discountType1 = discountType1;
        this.discountType2 = discountType2;
    }
    public DiscountDTO(DiscountKind discount_kind, int itemId, Category category, int percentage, ConditionDTO condition, DiscountType discountType1) {
        this.discount_kind = discount_kind;
        this.itemId = itemId;
        this.category = category;
        this.percentage = percentage;
        this.condition = condition;
        this.discountType1 = discountType1;
        this.itemId2 = -1;
        this.category2 = null;
        this.percentage2 = -1;
        this.condition2 = null;
    }
    public DiscountKind getDiscountKind() {
        return discount_kind;
    }
    public int getItemId() {
        return itemId;
    }
    public Category getCategory() {
        return category;
    }

    public int getPercentage() {
        return percentage;
    }
    public ConditionDTO getCondition() {
        return condition;
    }
    public int getItemId2() {
        return itemId2;
    }
    public Category getCategory2() {
        return category2;
    }
    public int getPercentage2() {
        return percentage2;
    }
    public ConditionDTO getCondition2() {
        return condition2;
    }
    public DiscountType getDiscountType1() {
        return discountType1;
    }
    public DiscountType getDiscountType2() {
        return discountType2;
    }
    



}
