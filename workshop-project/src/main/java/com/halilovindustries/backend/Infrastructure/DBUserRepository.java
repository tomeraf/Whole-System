package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.User.*;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

@Primary
@Repository
public class DBUserRepository implements IUserRepository {

    private final JpaUserAdapter jpaAdapter;

    private final Map<Integer, Guest> guestMemoryStore = new HashMap<>();
    private int guestIdCounter = 0;

    @Autowired
    public DBUserRepository(JpaUserAdapter jpaAdapter) {
        this.jpaAdapter = jpaAdapter;
    }

    @Override
    public void saveUser(Guest user) {
        guestMemoryStore.put(user.getUserID(), user);
    }

    @Override
    public void saveUser(Registered user) {
        Optional<Registered> existing = jpaAdapter.findByUsername(user.getUsername());

        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            throw new RuntimeException("Username already exists");
        }

        jpaAdapter.save(user);
    }

    @Override
    public Guest getUserById(int id) {
        Guest guest = guestMemoryStore.get(id);
        if (guest != null) return guest;
        return jpaAdapter.findById((long) id).orElse(null);
    }
    @Override
    public List<IRole> getAppointmentsOfUserInShop(int appointerId, int shopId) {
        return jpaAdapter.findAppointmentsByAppointerAndShop(appointerId, shopId);
    }

    @Override
    public int getIdToAssign() {
        if (guestIdCounter == 0) {
            Optional<Long> maxId = jpaAdapter.findAll().stream()
                    .map(Registered::getUserID)
                    .map(Long::valueOf)
                    .max(Long::compareTo);
            guestIdCounter = maxId.map(i -> i.intValue() + 1).orElse(1);
        }
        return guestIdCounter++;
    }

    @Override
    public void removeGuestById(int id) {
        if (guestMemoryStore.containsKey(id)) {
            Guest guest = guestMemoryStore.remove(id);
            guest.logout();
        } else {
            jpaAdapter.findById((long) id).ifPresent(jpaAdapter::delete);
        }
    }

    @Override
    public List<Registered> getAllRegisteredUsers() {
        return jpaAdapter.findAll();
    }

    @Override
    public Registered getUserByName(String username) {
        return jpaAdapter.findByUsername(username).orElseThrow();
    }

    @Override
    public List<Integer> getAllRegisteredsByShopAndPermission(int shopID, Permission permission) {
        List<Registered> allRegistered = getAllRegisteredUsers();
        List<Integer> userIds = new ArrayList<>();
        for (Registered reg : allRegistered) {
            try {
                if (reg.hasPermission(shopID, permission)) {
                    userIds.add(Math.toIntExact(reg.getUserID()));
            }
            } catch (Exception e) {
                // Handle the case where the user does not have a role in the shop
                // or any other exception that might occur
            }
            
        }
        return userIds;
    }
}
