package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

public interface IShipment {

    // checking if the shipment details are valid
    boolean validateShipmentDetails(ShipmentDetailsDTO details);

    // return the transactionId for good shipment; return null for bad shipment
    boolean processShipment(double price,ShipmentDetailsDTO details);

    //ShipmentDetailsDTO getShipmentDetails();
}
