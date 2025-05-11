package Domain.Shop.Purchase;

import java.util.ArrayList;
import java.util.List;

import Domain.DTOs.ConditionDTO;
import Domain.DTOs.DTOtoDomainFactory;
import Domain.Shop.Condition.Condition;

public class PurchasePolicy {
    private List<PurchaseType> purchaseTypes;
    private List<Condition> purchaseConditions;
    

    public PurchasePolicy(){
        this.purchaseTypes = new ArrayList<>();
        this.purchaseTypes.add(PurchaseType.BID);
        this.purchaseTypes.add(PurchaseType.AUCTION);
        this.purchaseTypes.add(PurchaseType.IMMEDIATE);
        this.purchaseConditions = new ArrayList<>();
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
        if (this.purchaseConditions.contains(newCondition)) {
            throw new IllegalArgumentException("Error: condition already exists.");
        } else {
            this.purchaseConditions.add(newCondition);
        }
    }
    
}
