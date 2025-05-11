package Domain.Adapters_and_Interfaces;

import Domain.DTOs.ShipmentDetailsDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
public class ShipmentAdapter implements IShipment {
    
    private final IShipment shipmentMethod;

    // Dependency injection of the shipment implementation adapter
    // This allows for flexibility in choosing the shipment method at runtime
    public ShipmentAdapter(@Qualifier("ProxyShipment") IShipment shipmentMethod) {
        this.shipmentMethod = shipmentMethod;
    }

    // checking if the shipment details are valid
    @Override
    public boolean validateShipmentDetails(ShipmentDetailsDTO details) {
        if (details == null || !details.fullShipmentDetails()) {
            // If shipment details are null or not complete, return false
            return false;
        }
        return shipmentMethod.validateShipmentDetails(details);
    }

    // return shipment id for good shipment; return null for bad shipment
    @Override
    public boolean processShipment(double price,ShipmentDetailsDTO details) {
        if (!validateShipmentDetails(details)) {
            // If shipment details are not valid, return null
            return false;
        }
        return shipmentMethod.processShipment(price);
    }

    @Override
    public ShipmentDetailsDTO getShipmentDetails() {
        return shipmentMethod.getShipmentDetails();
    }

}
