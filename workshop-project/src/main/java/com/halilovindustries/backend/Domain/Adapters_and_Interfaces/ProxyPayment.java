package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ProxyPayment implements IPayment {
    /*
    private final PaymentDetailsDTO details;

    public ProxyPayment(PaymentDetailsDTO details) {
        this.details = details;
      }
     */

    // This method checks if the payment method is valid
    @Override
    public boolean validatePaymentDetails(PaymentDetailsDTO details) {
        return true;
    }

    // This method processes the payment and returns a transaction ID if successful
    @Override
    public boolean processPayment(double price,PaymentDetailsDTO details) {
        return true;
    }


    /*
    @Override
    public PaymentDetailsDTO getPaymentDetails() {
        return null;
    }
     */
}
