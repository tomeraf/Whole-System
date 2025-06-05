package com.halilovindustries.backend.Domain.DTOs;

public class PaymentDetailsDTO {

    private String cardNumber;
    private String cardHolderName;
    private String holderID;
    private String cvv;
    private String month;
    private String year;


    public PaymentDetailsDTO(String cardNumber, String cardHolderName, String holderID, String cvv, String month, String year) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.holderID = holderID;
        this.cvv = cvv;
        this.month = month;
        this.year = year;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getHolderID() {
        return holderID;
    }

    public String getCvv() {
        return cvv;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    // This method checks if the payment details are complete
    // return true if all fields are filled, false otherwise
    public boolean fullDetails() {
        return cardNumber != null && !cardNumber.isEmpty() &&
               cardHolderName != null && !cardHolderName.isEmpty() &&
               holderID != null && !holderID.isEmpty() &&
               cvv != null && !cvv.isEmpty() &&
                month != null && !month.isEmpty() &&
                year != null && !year.isEmpty();
    } 
}
