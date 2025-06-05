package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

public interface IShipment {

    boolean validateShipmentDetails(ShipmentDetailsDTO details);

    Integer processShipment(ShipmentDetailsDTO details);

    boolean cancelShipment(int transactionId);
}