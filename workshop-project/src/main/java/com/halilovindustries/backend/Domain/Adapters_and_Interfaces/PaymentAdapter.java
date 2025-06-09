package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import org.springframework.stereotype.Component;

@Component
public class PaymentAdapter {

    private final IPayment paymentMethod;

    // Dependency injection of the authentication implementation adapter
    // This allows for flexibility in choosing the payment method at runtime
    public PaymentAdapter(IPayment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // checking if the payment details are valid
    public boolean validatePaymentDetails(PaymentDetailsDTO paymentDetails) {
        if (paymentDetails == null || paymentDetails.fullDetails()) {
            // If payment details are null or not complete, return false
            return false;
        }
        return paymentMethod.validatePaymentDetails(paymentDetails);
    }
    
    // return the transactionId for good payment; return null for bad payment
    public Integer processPayment(double price, PaymentDetailsDTO paymentDetails) {
        if (validatePaymentDetails(paymentDetails)) {
            // If payment details are not valid, return null
            return null;
        }
        return paymentMethod.processPayment(price, paymentDetails);
    }

    public boolean cancelPayment(int transactionId) {
        return paymentMethod.cancelPayment(transactionId);
    }
}
