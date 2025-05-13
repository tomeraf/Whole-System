package com.halilovindustries.backend.Domain.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Manager extends IRole {
    private Set<Permission> permission; //hashSet-to prevents duplication

    public Manager(int appointerID, int shopID, Set<Permission> permission) {
        this.appointerID = appointerID;
        this.shopID = shopID;
        this.permission = permission;
        this.appointments = new HashMap<>();
    }
    @Override
    public void addOwner(int nomineeID, IRole role)  {
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
        return appointments;
    }

    public Set<Permission> getPermissions() {
        return permission;
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
