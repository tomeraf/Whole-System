package com.halilovindustries.backend.Domain.DTOs;

import com.halilovindustries.backend.Domain.Shop.Policies.Condition.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.BaseCondition.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.CompositeCondition.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.BaseDiscount.*;
import com.halilovindustries.backend.Domain.Shop.Policies.Discount.CompositeDiscount.*;

public class DTOtoDomainFactory {
    public static Discount convertDTO(DiscountDTO discountDTO) {
        switch (discountDTO.getDiscountKind()) {
            case BASE:
                return convertBase(discountDTO,true);
            case CONDITIONAL:
                return convertBase(discountDTO, true);
            case MAX:
                return new MaxDiscount(convertBase(discountDTO,true),convertBase(discountDTO, false));
            case COMBINE:
                return new CombinedDiscount(convertBase(discountDTO,true),convertBase(discountDTO,false));
            default:
                throw new IllegalArgumentException("Invalid Discount Type");
        }
    }
    private static BaseDiscount convertBase(DiscountDTO discountDTO,boolean first) {
        if(first){
            switch (discountDTO.getDiscountType1()) {
                case BASE:
                    return new BaseDiscount(discountDTO.getPercentage(), discountDTO.getCategory(), discountDTO.getItemId());
                case CONDITIONAL:
                    return new ConditionalDiscount(convertDTO(discountDTO.getCondition()), discountDTO.getPercentage(), discountDTO.getItemId(), discountDTO.getCategory());
                default:
                    throw new IllegalArgumentException("Invalid Discount Type");
            }
        }
        else {
            switch (discountDTO.getDiscountType2()) {
                case BASE:
                    return new BaseDiscount(discountDTO.getPercentage2(), discountDTO.getCategory2(), discountDTO.getItemId2());
                case CONDITIONAL:
                    return new ConditionalDiscount(convertDTO(discountDTO.getCondition2()), discountDTO.getPercentage2(), discountDTO.getItemId2(), discountDTO.getCategory2());
                default:
                    throw new IllegalArgumentException("Invalid Discount Type");
            }

        }
    }

    public static Condition convertDTO(ConditionDTO conditionDTO) {
        switch (conditionDTO.getConditionType()) {
            case BASE:
                return convertBase(conditionDTO,true);
            case OR:
                return new OrCondition(convertBase(conditionDTO,true), convertBase(conditionDTO,false));
            case AND:
                return new AndCondition(convertBase(conditionDTO,true), convertBase(conditionDTO,false));
            case XOR:
                return new XorCondition(convertBase(conditionDTO,true), convertBase(conditionDTO,false));
            default:
                throw new IllegalArgumentException("Invalid Condition Type");
        }
    }
    private static BaseCondition convertBase(ConditionDTO conditionDTO,boolean first) {
        if (first) {
        switch (conditionDTO.getConditionLimits()) {
            case PRICE:
                return new PriceCondition(conditionDTO.getItemId(),conditionDTO.getCategory(),conditionDTO.getMinPrice(),conditionDTO.getMaxPrice());
            case QUANTITY:
                return new QuantityCondition(conditionDTO.getItemId(),conditionDTO.getCategory(),conditionDTO.getMinQuantity(),conditionDTO.getMaxQuantity());
            default:
                throw new IllegalArgumentException("Invalid Condition Limits");
        }
        }
        else {
            switch (conditionDTO.getConditionLimits2()) {
                case PRICE:
                    return new PriceCondition(conditionDTO.getItemId2(),conditionDTO.getCategory2(),conditionDTO.getMinPrice2(),conditionDTO.getMaxPrice2());
                case QUANTITY:
                    return new QuantityCondition(conditionDTO.getItemId2(),conditionDTO.getCategory2(),conditionDTO.getMinQuantity2(),conditionDTO.getMaxQuantity2());
                default:
                    throw new IllegalArgumentException("Invalid Condition Limits");
            }
        }
    }
}
