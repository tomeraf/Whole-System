package com.halilovindustries.backend.Infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.halilovindustries.backend.Domain.User.Guest;
import com.halilovindustries.backend.Domain.User.IRole;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;

@Repository
public class MemoryUserRepository implements IUserRepository {

    private Map<Integer, Guest> users = new HashMap<>(); // Map to store users by cartID

    private int idCounter = 0; // Unique ID for each user
    private List<Integer> removedIds = new ArrayList<>(); // List of removed IDs

    @Override
    public void saveUser(Guest user) {
        if (users.containsKey(user.getUserID())) {
            throw new RuntimeException("User already exists");
        }
        users.put(user.getUserID(), user); // Add the user to the list 
    }
    public void saveUser(Registered user) {
        users.put(user.getUserID(), user); // Add the user to the list 
    }

    @Override
    // Get user by ID - which is not active and has session token = null
    public Guest getUserById(int id) {
        Guest user = users.get(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        return user;
    }
    
    @Override
    public int getIdToAssign() {
        if (removedIds.isEmpty()) {
            return idCounter++;
        } else {
            return removedIds.remove(removedIds.size() - 1); // Reuse a removed ID
        }
    }

    @Override
    public void removeGuestById(int id) throws RuntimeException {
            if (!users.containsKey(id)) {
                throw new RuntimeException("User is not logged in");
            }
            Guest user = users.get(id);
            users.remove(id); // Remove the user from the list    
            user.logout(); 
    }

    @Override
    public List<Registered> getAllRegisteredUsers() {
        List<Registered> registeredUsers = new ArrayList<>();
        for (Guest user : users.values()) {
            if (user instanceof Registered) {
                registeredUsers.add((Registered) user);
            }
        }
        return registeredUsers;
    }

    @Override
    public Registered getUserByName(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null for Regiestered user");
        }
        List<Guest> allUsers = new ArrayList<>(users.values());
        for (Guest user : allUsers) {
            if (user instanceof Registered && ((Registered) user).getUsername().equals(username)) {
                return (Registered) user;
            }
        }
        return null;
    }

    @Override
    public List<Integer> getAllRegisteredsByShopAndPermission(int shopID, Permission permission) {
        List<Integer> registeredUsers = new ArrayList<>();
        for (Guest user : users.values()) {
            try {
                if (user instanceof Registered && ((Registered) user).hasPermission(shopID, permission)) {
                    registeredUsers.add(user.getUserID());
                }
            }
            catch (Exception e) {
                // Handle the case where the user does not have a role in the shop
                // or any other exception that might occur            
                }
        }
        return registeredUsers;
    }
    @Override
    public List<IRole> getAppointmentsOfUserInShop(int appointerId, int shopId) {
        List<IRole> appointments = new ArrayList<>();
        for (Guest user : users.values()) {
            if (user instanceof Registered) {
                Registered registeredUser = (Registered) user;
                if (registeredUser.getUserID() == appointerId) {
                    appointments.addAll(registeredUser.getAppointments(shopId).values());
                }
            }
        }
        return appointments;
    }
}
