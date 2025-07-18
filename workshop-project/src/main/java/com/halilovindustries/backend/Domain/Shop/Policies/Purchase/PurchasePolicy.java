package com.halilovindustries.backend.Domain.Shop.Policies.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Generated;
import com.halilovindustries.backend.Domain.DTOs.ConditionDTO;
import com.halilovindustries.backend.Domain.DTOs.DTOtoDomainFactory;
import com.halilovindustries.backend.Domain.Shop.Item;
import com.halilovindustries.backend.Domain.Shop.Policies.Condition.Condition;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

@Entity
public class PurchasePolicy {
    @Id
    private int id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "purchase_types", joinColumns = @JoinColumn(name = "policy_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_type")
    private List<PurchaseType> purchaseTypes;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "policy_id", referencedColumnName = "id")
    private List<Condition> purchaseConditions;


    public PurchasePolicy() {//Default constructor for JPA
    }
    public PurchasePolicy(int id) {
        this.id = id;
        this.purchaseTypes = new ArrayList<>();
        this.purchaseTypes.add(PurchaseType.BID);
        this.purchaseTypes.add(PurchaseType.AUCTION);
        this.purchaseTypes.add(PurchaseType.IMMEDIATE);
        this.purchaseConditions = new ArrayList<>();
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
        if (this.purchaseConditions.stream().anyMatch(c -> c.getId().equals(newCondition.getId()))) {
            throw new IllegalArgumentException("Error: condition with this ID already exists.");
        }
        else{
            this.purchaseConditions.add(newCondition);
        }

    }
    public void removeCondition(String conditionID) {
        this.purchaseConditions.stream()
                .filter(condition -> condition.getId().equals(conditionID))
                .findFirst()
                .ifPresentOrElse(
                        condition -> this.purchaseConditions.remove(condition),
                        () -> { throw new IllegalArgumentException("Error: condition does not exist."); }
                );
        
    }
    
    public boolean checkPurchase(HashMap<Item, Integer> items) {
        if(!allowsPurchaseType(PurchaseType.IMMEDIATE))
        {
            throw new IllegalArgumentException("Error: immediate purchase type not allowed.");
        }
        for (Condition condition : this.purchaseConditions) {
            if (!condition.checkCondition(items)) {
                throw new IllegalArgumentException("Error:"+condition.toString()+" ,not met.");
            }
        }
        return true;
    }
    public List<ConditionDTO> getConditions() {
        return purchaseConditions.stream()
                .map(condition -> {ConditionDTO con=new ConditionDTO(condition.getConditionType(),condition.getItemId(),condition.getCategory(),condition.getConditionLimits(),condition.getMinPrice(),condition.getMaxPrice(),condition.getMinQuantity(),condition.getMaxQuantity(),condition.getConditionLimits2(),condition.getItemId2(),condition.getMinPrice2(),condition.getMaxPrice2(),condition.getMinQuantity2(),condition.getMaxQuantity2(),condition.getCategory2());
                con.setId(condition.getId());
                return con;})
                .toList();
    }
    public List<PurchaseType> getPurchaseTypes() {
        return purchaseTypes;
    }
    public int getId() {
        return id;
    }
}
