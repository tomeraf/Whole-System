package com.halilovindustries.backend.Domain.DTOs;


public class ShipmentDetailsDTO {

    private String ID; // ID of the user
    private String name; // name of the user
    private String email; // email of the user
    private String phone; // phone number of the user
    private String contry; // country of the shipment
    private String city; // city of the shipment
    private String address; // address of the shipment
    private String zipcode; // zipcode of the shipment

    public ShipmentDetailsDTO(String ID, String name, String email, String phone, String contry, String city,
            String address, String zipcode) {
        this.ID = ID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.contry = contry;
        this.city = city;
        this.address = address;
        this.zipcode = zipcode;
    }
    public String getID() {
        return ID;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public String getContry() {
        return contry;
    }
    public String getCity() {
        return city;
    }
    public String getAddress() {
        return address;
    }
    public String getZipcode() {
        return zipcode;
    }

    // checking if the shipment details are fully filled
    public boolean fullShipmentDetails() {
        return ID != null && name != null && email != null && phone != null && contry != null && city != null
                && address != null && zipcode != null && !ID.isEmpty() && !name.isEmpty() && !email.isEmpty()
                && !phone.isEmpty() && !contry.isEmpty() && !city.isEmpty() && !address.isEmpty();
    }
}
