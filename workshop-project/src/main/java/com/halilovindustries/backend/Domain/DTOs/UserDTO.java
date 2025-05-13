package com.halilovindustries.backend.Domain.DTOs;

import java.time.LocalDate;

public class UserDTO {
    public int id;
    public String username;
    public String password;
    public LocalDate dateOfBirth;
    public UserDTO(int id, String username, String password, LocalDate dateOfBirth) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
    }
}
