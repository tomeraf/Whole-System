package com.halilovindustries.backend.Domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.halilovindustries.backend.Domain.Message;

import jakarta.persistence.*;

@Entity
public class Registered extends Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "is_system_manager")
    private boolean isSystemManager = false;

    @Embedded
    private Suspension suspension = new Suspension();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IRole> roleInShops = new ArrayList<>();

    @Transient
    private Map<Integer, Message> inbox = new HashMap<>();

    public Registered(String username, String password, LocalDate dateOfBirth) {
        this.username = username;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.roleInShops = new ArrayList<>();
        this.inbox = new HashMap<>();
        this.suspension = new Suspension();
    }

    public IRole getRoleInShop(int shopID) {
        return roleInShops.stream()
            .filter(role -> role.getShopID() == shopID)
            .findFirst()
            .orElse(null);
    }

    public boolean logout() {
        if (!isInSession()) {
            throw new IllegalArgumentException("Unauthorized Action: already logged out.");
        }
        this.sessionToken = null;
        return true;
    }

    public void setRoleToShop(int shopID, IRole newRole) {
        newRole.setUser(this);
        // Remove existing role for the shop if present
        this.roleInShops.removeIf(role -> role.getShopID() == shopID);
        this.roleInShops.add(newRole);
    }

    public boolean removeRoleFromShop(int shopID) {
        return roleInShops.removeIf(r -> r.getShopID() == shopID);
    }

    public boolean hasPermission(int shopID, Permission permission) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            return role.hasPermission(permission);
        }
        throw new IllegalArgumentException("No role found for shop ID: " + shopID);
    }

    public boolean addPermission(int shopID, Permission permission) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            role.addPermission(permission);
            return true;
        } else {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
    }

    public boolean removePermission(int shopID, Permission permission) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            role.removePermission(permission);
            return true;
        } else {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
    }

    public boolean addManager(int shopID, int nomineeID, Manager nominee) {
        IRole role = getRoleInShop(shopID);
        if (role == null) {
            throw new IllegalArgumentException("User has no role in ShopId: " + shopID);
        }
        if (!role.hasPermission(Permission.APPOINTMENT)) {
            throw new IllegalArgumentException("User has no permission to add users.");
        }
        role.addManager(nominee);
        return true;
    }

    public boolean addOwner(int shopID, int nomineeID, Owner nominee) {
        IRole role = getRoleInShop(shopID);
        if (role == null) {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
        if (!role.hasPermission(Permission.APPOINTMENT)) {
            throw new IllegalArgumentException("No permission to add appointment in shop ID: " + shopID);
        }
        role.addOwner(nominee);
        return true;
    }

    public List<Integer> removeAppointment(int shopID, int appointeeID) {
        IRole role = getRoleInShop(shopID);
        if (role == null) {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
        if (!role.hasPermission(Permission.APPOINTMENT)) {
            throw new IllegalArgumentException("No permission to remove appointment in shop ID: " + shopID);
        }
        return role.removeAppointment(appointeeID);
    }

    public Map<Integer, IRole> getAppointments(int shopID) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            return role.getAppointments();
        } else {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
    }

    public int getAppointer(int shopID) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            return role.getAppointer();
        } else {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    
    public List<Permission> getPermissions(int shopID) {
        IRole role = getRoleInShop(shopID);
        if (role != null) {
            return role.getPermissions();
        } else {
            throw new IllegalArgumentException("No role found for shop ID: " + shopID);
        }
    }

    public void setSystemManager(boolean isSystemManager) {
        this.isSystemManager = isSystemManager;
    }

    public void addSuspension(Optional<LocalDateTime> startDate, Optional<LocalDateTime> endDate) {
        if (startDate.isPresent() && endDate.isPresent()) {
            this.suspension.setSuspension(startDate.get(), endDate.get());
        } else {
            this.suspension.setSuspension();
        }
    }

    public void removeSuspension() {
        this.suspension.removeSuspension();
    }

    @Override
    public boolean isSuspended() {
        return this.suspension.isSuspended(LocalDateTime.now());
    }

    public String showSuspension() {
        if (isSuspended()) {
            return "user:" + username + "," + suspension.toString() + "\n";
        }
        return "";
    }

    public void addMessage(Message message) {
        inbox.put(message.getId(), message);
    }

    public List<Message> getInbox() {
        return inbox.values().stream()
                .sorted((m1, m2) -> m2.getDateTime().compareTo(m1.getDateTime()))
                .toList();
    }

    // public Long getId() {
    //     return id;
    // }

    public ShoppingCart getCart() {
        return cart;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSystemManager() {
        return isSystemManager;
    }

    // public void setId(Long id) {
    //     this.id = id;
    // }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
