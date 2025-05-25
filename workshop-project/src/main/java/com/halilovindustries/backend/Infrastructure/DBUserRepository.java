package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.User.Guest;
import com.halilovindustries.backend.Domain.User.Permission;
import com.halilovindustries.backend.Domain.User.Registered;
import com.halilovindustries.backend.Domain.Repositories.IUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public class DBUserRepository implements IUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<Integer, Guest> guestMemoryStore = new HashMap<>();
    private int guestIdCounter = 0;

    @Override
    public void saveUser(Guest user) {
            guestMemoryStore.put(user.getUserID(), user);
    }

    @Override
    public void saveUser(Registered user) {
        List<Registered> existing = entityManager.createQuery(
                "SELECT r FROM Registered r WHERE r.username = :username", Registered.class)
            .setParameter("username", user.getUsername())
            .getResultList();

        if (!existing.isEmpty()) {
            throw new RuntimeException("Username already exists");
        }
        entityManager.persist(user);
    }

    @Override
    public Guest getUserById(int id) {
        Guest guest = guestMemoryStore.get(id);
        if (guest != null) return guest;
        return entityManager.find(Registered.class, (long) id);
    }

    @Override
    public int getIdToAssign() {
        // if the ID counter reaches 0, reset it to the max ID in the database
        // to avoid collisions with existing IDs
        if (guestIdCounter == 0) {
            Long maxId = entityManager.createQuery(
                    "SELECT COALESCE(MAX(r.id), 0) FROM Registered r", Long.class)
                    .getSingleResult();
            guestIdCounter = maxId.intValue() + 1;
        }
        return guestIdCounter++;
    }

    @Override
    public void removeGuestById(int id) {
        if (guestMemoryStore.containsKey(id)) {
            Guest guest = guestMemoryStore.remove(id);
            guest.logout();
        } else {
            Guest user = entityManager.find(Registered.class, (long) id);
            if (user != null) {
                entityManager.remove(user);
            } else {
                throw new RuntimeException("User not found");
            }
        }
    }

    @Override
    public List<Registered> getAllRegisteredUsers() {
        return entityManager.createQuery("SELECT r FROM Registered r", Registered.class).getResultList();
    }

    @Override
    public Registered getUserByName(String username) {
        List<Registered> result = entityManager.createQuery(
                "SELECT r FROM Registered r WHERE r.username = :username",
                Registered.class).setParameter("username", username).getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Integer> getAllRegisteredsByShopAndPermission(int shopID, Permission permission) {
        List<Registered> allRegistered = getAllRegisteredUsers();
        List<Integer> userIds = new ArrayList<>();
        for (Registered reg : allRegistered) {
            if (reg.hasPermission(shopID, permission)) {
                userIds.add(Math.toIntExact(reg.getUserID()));
            }
        }
        return userIds;
    }
}
