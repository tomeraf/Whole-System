package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ProxyShipment implements IShipment {
    /*private final ShipmentDetailsDTO details;
    public ProxyShipment() {
        this.details = details;
      }

     */

    // This method checks if the shipment details are valid
    @Override
    public boolean validateShipmentDetails(ShipmentDetailsDTO details) {
        return true;
    }

    // This method processes the shipment and returns a shipment ID if successful
    @Override
    public boolean processShipment(double price, ShipmentDetailsDTO details) {
        return true;
    }
    /*
    @Override
    public ShipmentDetailsDTO getShipmentDetails() {
        return null;
    }
    */
}
