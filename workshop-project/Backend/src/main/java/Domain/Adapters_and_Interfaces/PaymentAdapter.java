package Domain.Adapters_and_Interfaces;

import Domain.DTOs.PaymentDetailsDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
public class PaymentAdapter implements IPayment {

    private final IPayment paymentMethod;

    // Dependency injection of the authentication implementation adapter
    // This allows for flexibility in choosing the payment method at runtime
    public PaymentAdapter(@Qualifier("ProxyPayment") IPayment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // checking if the payment details are valid
    @Override
    public boolean validatePaymentDetails() {
        if (paymentMethod.getPaymentDetails() == null || !paymentMethod.getPaymentDetails().fullDetails()) {
            // If payment details are null or not complete, return false
            return false;
        }
        return paymentMethod.validatePaymentDetails();
    }
    
    // return the transactionId for good payment; return null for bad payment
    @Override
    public boolean processPayment(double price) {
        if (validatePaymentDetails()) {
            // If payment details are not valid, return null
            return false;
        }
        return paymentMethod.processPayment(price);
    }

    @Override
    public PaymentDetailsDTO getPaymentDetails() {
        return paymentMethod.getPaymentDetails();
    }
}
