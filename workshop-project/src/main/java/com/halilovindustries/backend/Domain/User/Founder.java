package com.halilovindustries.backend.Domain.User;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("FOUNDER")
public class Founder extends Owner {

    public Founder(int shopID) {
        super(-1, shopID);
    }
    public Founder() {
        super(-1, 0); // Default constructor for JPA
    }
}