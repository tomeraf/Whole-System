package com.halilovindustries.backend.Domain.DTOs;

public class UserShopPermissionDTO {
    public int userId;
    public int shopId;
    public String permission; // could also be an enum or serialized string
}