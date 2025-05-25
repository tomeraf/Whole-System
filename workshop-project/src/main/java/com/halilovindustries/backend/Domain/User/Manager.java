package com.halilovindustries.backend.Domain.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.*;

@Entity 
public class Manager extends IRole {

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permission; //hashSet-to prevents duplication

    public Manager(int appointerID, int shopID, Set<Permission> permission) {
        this.appointerID = appointerID;
        this.shopID = shopID;
        this.permission = permission;
        this.appointments = new ArrayList<>();
    }
    @Override
    public void addOwner(IRole role)  {
        throw new IllegalArgumentException("Menager can not appoint owner");
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return this.permission.contains(permission);
    }

    @Override
    public void addPermission(Permission permission) {
        this.permission.add(permission);
    }

    @Override
    public void removePermission(Permission permission) {
        if (this.permission.contains(permission)) {
            this.permission.remove(permission);
        }
    }

    @Override
    public Map<Integer, IRole> getAppointments() {
        // no implementation needed for manager
        Map<Integer, IRole> appointmentMap = new HashMap<>();
        return appointmentMap;
    }

    public List<Permission> getPermissions() {
        return permission.stream().toList();
    }

    public String getPermissionsString() {
        StringBuilder sb = new StringBuilder();
        for (Permission p : permission) {
            sb.append(p.toString()).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // remove last comma and space
        }
        return sb.toString();
    }
    
}
