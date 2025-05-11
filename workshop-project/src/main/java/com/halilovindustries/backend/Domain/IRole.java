package com.halilovindustries.backend.Domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public abstract class IRole {
    protected int appointerID; //-1 for founder
    protected int shopID;
    protected Map<Integer,IRole> appointments;
    protected Registered user; // The user that has this role

    abstract boolean hasPermission(Permission permission);
    abstract void addPermission(Permission permission);
    abstract void removePermission(Permission permission);
    abstract void addOwner(int nomineeID, IRole role);
    public void setUser(Registered user) {
        this.user = user;
    }
    //abstract void addAppointment(int nomineeID, IRole role );
    public void addManager(int nomineeID, IRole role)  {
        if (hasPermission(Permission.APPOINTMENT)) {
            appointments.put(nomineeID, role);
        } else {
            throw new IllegalArgumentException("No permission to add appointment");
        }
    }
    
    public List<Integer> removeAppointment(int appointeeID) throws IllegalArgumentException {
        if (!appointments.containsKey(appointeeID)) {
            throw new IllegalArgumentException("No appointment found for user ID: " + appointeeID);
        }
        List<Integer> idsToRemove = new ArrayList<>();
        List<Integer> ids = appointments.get(appointeeID).removeAllAppointments();
        idsToRemove.addAll(ids);
        Registered registered = appointments.get(appointeeID).getUser();
        registered.removeRoleFromShop(appointments.get(appointeeID).shopID);
        appointments.remove(appointeeID);
        idsToRemove.add(appointeeID);
        return idsToRemove;
    }

    public List<Integer> removeAllAppointments() {
        List<Integer> idsToRemove = new ArrayList<>();
        List<Integer> appointeeIDs = new ArrayList<>(appointments.keySet());
        if(appointeeIDs.isEmpty()) {
            return new ArrayList<>();
        }
        for (int appointeeID : appointeeIDs) {
            List<Integer> ids = appointments.get(appointeeID).removeAllAppointments();
            idsToRemove.addAll(ids);
            appointments.get(appointeeID).user.removeRoleFromShop(appointments.get(appointeeID).shopID);
            appointments.remove(appointeeID);
            idsToRemove.add(appointeeID);
        }
        return idsToRemove;
    }

    
    abstract Map<Integer, IRole> getAppointments(); // Returns a list of all the appointments the role has made  
    public int getShopID() {
        return shopID;
    }
    public int getAppointer() {
        return appointerID;
    }
    abstract String getPermissionsString();
    public Registered getUser() {
        return user;
    }
    
}
