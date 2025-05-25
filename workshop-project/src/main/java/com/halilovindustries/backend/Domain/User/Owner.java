package com.halilovindustries.backend.Domain.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

@Entity 
public class Owner extends IRole {

    public enum OwnerType {
        REGULAR,
        FOUNDER
    }

    @Enumerated(EnumType.STRING)
    private OwnerType type = OwnerType.REGULAR;

    public Owner(int appointerID, int shopID) {
        this.appointerID = appointerID;
        this.shopID = shopID;
        this.appointments = new ArrayList<>();
        this.type = (appointerID == -1) ? OwnerType.FOUNDER : OwnerType.REGULAR;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        // Assuming the owner has all permissions
        return true;
    }

    @Override
    public void addPermission(Permission permission) {
        //No implementation needed for owner
    }

    @Override
    public void removePermission(Permission permission) {
        //No implementation needed for owner
    }

    @Override    
    public void addOwner(IRole role) {
        appointments.add(role);
    }

    @Override
    public Map<Integer,IRole> getAppointments() {
        Map<Integer, IRole> appointmentMap = new HashMap<>();
        for (IRole role : appointments) {
            appointmentMap.put(role.getUser().getUserID(), role);
        }
        return appointmentMap;
    }

    public String getPermissionsString() {
        return "Owner - has all permissions";
    }
    public List<Permission> getPermissions(){
        if (appointerID == -1) {
            return List.of(Permission.FOUNDER);
        }
        return List.of(Permission.OWNER);
    }
}

