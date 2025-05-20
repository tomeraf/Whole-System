package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DTOtoDomainFactory;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;

public class PurchasePolicy {
    private List<PurchaseType> purchaseTypes;
    private HashMap<Integer,Condition> purchaseConditions;
    

    public PurchasePolicy(){
        this.purchaseTypes = new ArrayList<>();
        this.purchaseTypes.add(PurchaseType.BID);
        this.purchaseTypes.add(PurchaseType.AUCTION);
        this.purchaseTypes.add(PurchaseType.IMMEDIATE);
        this.purchaseConditions = new HashMap<>();
    }
    public void updatePurchaseType(PurchaseType purchaseType){
        if (this.purchaseTypes.contains(purchaseType)) {
            this.purchaseTypes.remove(purchaseType);
        } else {
            this.purchaseTypes.add(purchaseType);
        }

    }
    //need to implement to check that a basket is valid for purchase
    public boolean allowsPurchaseType(PurchaseType type) {
        if (this.purchaseTypes.contains(type)) {
            return true;
        } else {
            throw new IllegalArgumentException("Error: purchase type not allowed.");
        }
    }
    public void addCondition(ConditionDTO condition) {
        Condition newCondition = DTOtoDomainFactory.convertDTO(condition);
        if (this.purchaseConditions.keySet().contains(newCondition.getId())) {
            throw new IllegalArgumentException("Error: condition already exists.");
        } else {
            this.purchaseConditions.put(newCondition.getId(), newCondition);
        }
    }
    public void removeCondition(int conditionID) {
        if (this.purchaseConditions.keySet().contains(conditionID)) {
            this.purchaseConditions.remove(conditionID);
        } else {
            throw new IllegalArgumentException("Error: condition does not exist.");
        }
        
    }
    
    public boolean checkPurchase(HashMap<Item, Integer> items) {
        if(!allowsPurchaseType(PurchaseType.IMMEDIATE))
        {
            throw new IllegalArgumentException("Error: immediate purchase type not allowed.");
        }
        for (Condition condition : this.purchaseConditions.values()) {
            if (!condition.checkCondition(items)) {
                throw new IllegalArgumentException("Error:"+condition.toString()+" ,not met.");
            }
        }
        return true;
    }
    public List<ConditionDTO> getConditions() {
        return purchaseConditions.values().stream()
                .map(condition -> {ConditionDTO con=new ConditionDTO(condition.getConditionType(),condition.getItemId(),condition.getCategory(),condition.getConditionLimits(),condition.getMinPrice(),condition.getMaxPrice(),condition.getMinQuantity(),condition.getMaxQuantity(),condition.getConditionLimits2(),condition.getItemId2(),condition.getMinPrice2(),condition.getMaxPrice2(),condition.getMinQuantity2(),condition.getMaxQuantity2(),condition.getCategory2());
                con.setId(condition.getId());
                return con;})
                .toList();
    }
    public List<PurchaseType> getPurchaseTypes() {
        return purchaseTypes;
    }
}
