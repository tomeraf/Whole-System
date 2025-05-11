package Domain.Adapters_and_Interfaces;

import Domain.DTOs.ShipmentDetailsDTO;
import org.springframework.stereotype.Component;

public interface IShipment {

    // checking if the shipment details are valid
    boolean validateShipmentDetails(ShipmentDetailsDTO details);

    // return the transactionId for good shipment; return null for bad shipment
    boolean processShipment(double price,ShipmentDetailsDTO details);

    //ShipmentDetailsDTO getShipmentDetails();
}
