package Tests;

import com.halilovindustries.Application;
import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.ExternalSystems;
import com.halilovindustries.backend.Infrastructure.RealPayment;
import com.halilovindustries.backend.Infrastructure.RealShipment;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
public class ExternalSystemsTest {

    @Autowired
    private RealPayment payment;
    @Autowired
    private RealShipment shipment;
    @Autowired
    private ExternalSystems externalSystems;

    @Test
    public void testHandshake() {
        assertTrue(externalSystems.handshake(), "External systems handshake should return OK");
    }

    @Test
    public void testProcessPaymentSuccess() {
        PaymentDetailsDTO validDetails = new PaymentDetailsDTO(
                "1234123412341234",
                "John Doe",
                "123456789",
                "123",
                "12",
                "2025"
        );

        Integer transactionId = payment.processPayment(50.0, validDetails);
        assertNotNull(transactionId, "Payment transaction ID should not be null");
        assertTrue(transactionId >= 10000 && transactionId <= 100000, "Payment transaction ID should be valid");
    }

    @Test
    public void testCancelPaymentSuccess() {
        PaymentDetailsDTO validDetails = new PaymentDetailsDTO(
                "1234123412341234",
                "John Doe",
                "123456789",
                "123",
                "12",
                "2025"
        );

        Integer transactionId = payment.processPayment(30.0, validDetails);
        assertNotNull(transactionId, "Payment must succeed before canceling");

        boolean cancelOk = payment.cancelPayment(transactionId);
        assertTrue(cancelOk, "Canceling payment should succeed");
    }

    @Test
    public void testProcessShipmentSuccess() {
        ShipmentDetailsDTO details = new ShipmentDetailsDTO(
                "123456789", "John Doe", "john@example.com", "0541234567",
                "Israel", "Tel Aviv", "Ben Yehuda 10", "61000"
        );

        Integer transactionId = shipment.processShipment(details);
        assertNotNull(transactionId, "Shipment transaction ID should not be null");
        assertTrue(transactionId >= 10000 && transactionId <= 100000, "Shipment transaction ID should be valid");
    }

    @Test
    public void testCancelShipmentSuccess() {
        ShipmentDetailsDTO details = new ShipmentDetailsDTO(
                "123456789", "John Doe", "john@example.com", "0541234567",
                "Israel", "Tel Aviv", "Ben Yehuda 10", "61000"
        );

        Integer transactionId = shipment.processShipment(details);
        assertNotNull(transactionId, "Shipment must succeed before canceling");

        boolean cancelOk = shipment.cancelShipment(transactionId);
        assertTrue(cancelOk, "Canceling shipment should succeed");
    }
}