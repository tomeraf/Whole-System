package Domain.DTOs;

import org.springframework.stereotype.Component;

@Component
public class PaymentDetailsDTO {

    private String cardNumber;
    private String cardHolderName;
    private String holderID;
    private String expirationDate;
    private String cvv;

    public PaymentDetailsDTO(String cardNumber, String cardHolderName, String holderID, String expirationDate, String cvv) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.holderID = holderID;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
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

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getCvv() {
        return cvv;
    }

    // This method checks if the payment details are complete
    // return true if all fields are filled, false otherwise
    public boolean fullDetails() {
        return cardNumber != null && !cardNumber.isEmpty() &&
               cardHolderName != null && !cardHolderName.isEmpty() &&
               holderID != null && !holderID.isEmpty() &&
               expirationDate != null && !expirationDate.isEmpty() &&
               cvv != null && !cvv.isEmpty();
    }
}
