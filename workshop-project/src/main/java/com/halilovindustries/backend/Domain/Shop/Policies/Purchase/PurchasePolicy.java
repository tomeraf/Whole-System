package Domain.Shop.Policies.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import Domain.DTOs.ConditionDTO;
import Domain.DTOs.DTOtoDomainFactory;
import Domain.Shop.Item;
import Domain.Shop.Policies.Condition.Condition;

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
    public void updatePurchaseType(String purchaseType){
        PurchaseType type;
        try{
            type = PurchaseType.fromString(purchaseType);
        }
        catch(Exception e){
            throw new IllegalArgumentException("not valid purchase Type");
        }
        if (this.purchaseTypes.contains(type)) {
            this.purchaseTypes.remove(type);
        } else {
            this.purchaseTypes.add(type);
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
}
