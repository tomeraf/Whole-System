package Domain.Adapters_and_Interfaces;

import Domain.DTOs.ShipmentDetailsDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("ProxyShipment")
public class ProxyShipment implements IShipment {
    /*private final ShipmentDetailsDTO details;
    public ProxyShipment() {
        this.details = details;
      }

     */

    // This method checks if the shipment details are valid
    @Override
    public boolean validateShipmentDetails() {
        return true;
    }

    // This method processes the shipment and returns a shipment ID if successful
    @Override
    public boolean processShipment(double price) {
        return true;
    }
    /*
    @Override
    public ShipmentDetailsDTO getShipmentDetails() {
        return null;
    }
    */
}
