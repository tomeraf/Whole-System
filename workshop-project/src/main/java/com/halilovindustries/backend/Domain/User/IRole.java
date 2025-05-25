package com.halilovindustries.backend.Domain.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role_type")
public abstract class IRole {
    
    @Column(name = "appointer_id")
    protected int appointerID; //-1 for founder

    @Column(name = "shop_id")
    protected int shopID;
    
    @Transient// This field is not persisted in the database
    protected   List<IRole> appointments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_id")
    protected Registered user;

    abstract boolean hasPermission(Permission permission);
    abstract void addPermission(Permission permission);
    abstract void removePermission(Permission permission);
    abstract void addOwner(IRole role);
    public void setUser(Registered user) {
        this.user = user;
    }
    //abstract void addAppointment(int nomineeID, IRole role );
    public void addManager(IRole role) {
        if (hasPermission(Permission.APPOINTMENT)) {
            appointments.add(role);
        } else {
            throw new IllegalArgumentException("No permission to add appointment");
        }
    }
    
    public List<Integer> removeAppointment(int appointeeID) throws IllegalArgumentException {
        List<Integer> idsToRemove = new ArrayList<>();
        idsToRemove.add(appointeeID);
        for (IRole role : appointments) {
            idsToRemove.addAll(role.removeAllAppointments());
            Registered registered = role.getUser();
            registered.removeRoleFromShop(role.getShopID());
        }
        appointments.clear();
        return idsToRemove;
    }

    public List<Integer> removeAllAppointments() {
        List<Integer> idsToRemove = new ArrayList<>();
        List<IRole> appointees = new ArrayList<>(appointments);
        if(appointees.isEmpty()) {
            return new ArrayList<>();
        }
        for(IRole role : appointees) {
            idsToRemove.addAll(role.removeAllAppointments());
            role.user.removeRoleFromShop(role.shopID);
            idsToRemove.add(role.user.getUserID());
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
    abstract List<Permission> getPermissions();
    public Registered getUser() {
        return user;
    }
    
}
