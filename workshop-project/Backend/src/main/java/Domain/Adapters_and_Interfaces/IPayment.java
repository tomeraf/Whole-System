package Domain.Adapters_and_Interfaces;

import Domain.DTOs.PaymentDetailsDTO;
import org.springframework.stereotype.Component;

public interface IPayment {

    // checking if the payment details are valid
    boolean validatePaymentDetails(PaymentDetailsDTO details);

    // return the transactionId for good payment; return null for bad payment
    boolean processPayment(double price,PaymentDetailsDTO details);

    //PaymentDetailsDTO getPaymentDetails();

}
