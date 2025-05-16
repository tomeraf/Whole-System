package com.halilovindustries.backend.Domain.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Owner extends IRole {

    public Owner(int appointerID, int shopID) {
        this.appointerID = appointerID;
        this.shopID = shopID;
        this.appointments = new HashMap<>();
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
    public void addOwner(int nomineeID, IRole role) {
        appointments.put(nomineeID, role);
    }

    @Override
    public Map<Integer,IRole> getAppointments() {
        return appointments;
    }

    public String getPermissionsString() {
        return "Owner - has all permissions";
    }
    public List<Permission> getPermissions(){
        return List.of(Permission.OWNER);
    }
}

