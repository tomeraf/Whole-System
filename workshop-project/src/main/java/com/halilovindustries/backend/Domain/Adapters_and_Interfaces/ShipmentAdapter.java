// package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

// import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
// import org.springframework.stereotype.Component;

// @Component
// public class ShipmentAdapter {

//     private final IShipment shipmentMethod;

//     public ShipmentAdapter(IShipment shipmentMethod) {
//         this.shipmentMethod = shipmentMethod;
//     }

//     public boolean validateShipmentDetails(ShipmentDetailsDTO details) {
//         return details != null && details.fullShipmentDetails() &&
//                shipmentMethod.validateShipmentDetails(details);
//     }

//     public Integer processShipment(ShipmentDetailsDTO details) {
//         if (!validateShipmentDetails(details)) {
//             return null;
//         }
//         return shipmentMethod.processShipment(details);
//     }

//     public boolean cancelShipment(int transactionId) {
//         return shipmentMethod.cancelShipment(transactionId);
//     }
// }