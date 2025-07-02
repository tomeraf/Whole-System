package com.halilovindustries.backend.Domain;



import java.time.LocalDateTime;
import java.util.Set;

import com.halilovindustries.backend.Domain.User.Permission;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@DiscriminatorValue("OFFER")
public class OfferMessage extends Message {
    private String appointerName;
    private String appointeeName;

    private Boolean decision; // if null = pending, if true = accepted, if false = rejected

    private boolean managerOffer; // if the message is an offer from a manager
    private Set<Permission> offerDetails; // details of the offer, if it is an offer

    
    public OfferMessage(int id, String userName, String shopName, LocalDateTime dateTime, String title, String content, boolean FromUser) {
        super(id, userName, shopName, dateTime, title, content, FromUser);
        this.appointerName = null; // default value for appointerId
        this.appointeeName = null; // default value for appointeeId
        this.decision = false; // default value for decision
        this.managerOffer = false; // default value for managerOffer
        this.offerDetails = null; // default value for offerDetails
    }
    public OfferMessage() {
        super(); // Default constructor for JPA
        this.appointerName = null; // default value for appointerId
        this.appointeeName = null; // default value for appointeeId
        this.decision = null; // default value for decision
        this.managerOffer = false; // default value for managerOffer
        this.offerDetails = null; // default value for offerDetails
    }

    public String getAppointerName() {
        return appointerName;
    }
    public void setAppointerName(String appointerName) {
        this.appointerName = appointerName;
    }
    public String getAppointeeName() {
        return appointeeName;
    }
    public void setAppointeeName(String appointeeName) {
        this.appointeeName = appointeeName;
    }
    public Boolean getDecision() {
        return this.decision;
    }

    public void setDecision(Boolean decision) {
        this.decision = decision;
    }
    
    public boolean isManagerOffer() {
        return managerOffer;
    }
    public void setManagerOffer(boolean managerOffer) {
        this.managerOffer = managerOffer;
    }
    public Set<Permission> getOfferDetails() {
        return offerDetails;
    }
    public void setOfferDetails(Set<Permission> offerDetails) {
        this.offerDetails = offerDetails;
    }
    @Override
    public boolean isOffer() {
        return true;
    }
}