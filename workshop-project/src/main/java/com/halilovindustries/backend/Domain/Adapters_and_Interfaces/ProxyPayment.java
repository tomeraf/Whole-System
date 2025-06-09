package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
public class ProxyPayment implements IPayment {

    // This method checks if the payment method is valid
    @Override
    public boolean validatePaymentDetails(PaymentDetailsDTO details) {
        return true;
    }

    // This method processes the payment and returns a transaction ID if successful
    @Override
    public Integer processPayment(double price,PaymentDetailsDTO details) {
        return 0;
    }

    @Override
    public boolean cancelPayment(int transactionId) {
        return true;
    }
}
