package com.halilovindustries.backend.Domain.Shop.Purchase;

import java.util.ArrayList;
import java.util.List;

public class PurchasePolicy {
    private List<PurchaseType> purchaseTypes;
    

    public PurchasePolicy(){
        this.purchaseTypes = new ArrayList<>();
        this.purchaseTypes.add(PurchaseType.BID);
        this.purchaseTypes.add(PurchaseType.AUCTION);
        this.purchaseTypes.add(PurchaseType.IMMEDIATE);
    }
    public void addPurchaseType(String purchaseType) {
        this.purchaseTypes.add(PurchaseType.fromString(purchaseType));
    }

    public void removePurchaseType(String purchaseType) {
        //this.purchaseTypes.remove(PurchaseType.fromString(purchaseType));
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
    
}
