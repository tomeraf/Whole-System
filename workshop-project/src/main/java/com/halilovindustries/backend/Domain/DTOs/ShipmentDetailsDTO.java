package com.halilovindustries.backend.Domain.DTOs;

public class ShipmentDetailsDTO {

    private String ID;
    private String name;
    private String email;
    private String phone;
    private String country;
    private String city;
    private String address;
    private String zipcode;

    public ShipmentDetailsDTO(String ID, String name, String email, String phone, String country, String city,
                              String address, String zipcode) {
        this.ID = ID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.country = country;
        this.city = city;
        this.address = address;
        this.zipcode = zipcode;
    }

    public String getID() { return ID; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getZipcode() { return zipcode; }

    public boolean fullShipmentDetails() {
        return ID != null && !ID.isEmpty() &&
               name != null && !name.isEmpty() &&
               email != null && !email.isEmpty() &&
               phone != null && !phone.isEmpty() &&
               country != null && !country.isEmpty() &&
               city != null && !city.isEmpty() &&
               address != null && !address.isEmpty() &&
               zipcode != null && !zipcode.isEmpty();
    }
}
