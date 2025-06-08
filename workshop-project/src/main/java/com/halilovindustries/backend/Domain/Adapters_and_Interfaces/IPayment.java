package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import org.springframework.stereotype.Component;

public interface IPayment {

    // checking if the payment details are valid
    boolean validatePaymentDetails(PaymentDetailsDTO details);

    // return the transactionId for good payment; return null for bad payment
    Integer processPayment(double price, PaymentDetailsDTO details);

    boolean cancelPayment(int transactionId);

    //boolean handShake()
}