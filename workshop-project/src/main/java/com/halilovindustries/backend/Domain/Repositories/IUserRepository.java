package com.halilovindustries.backend.Domain.Repositories;

import com.halilovindustries.backend.Domain.Guest;
import com.halilovindustries.backend.Domain.Registered;
import java.util.Map;

public interface IUserRepository {
    void saveUser(Guest user);
    void saveUser(Registered user);
    void removeGuestById(int id);
    Guest getUserById(int id);
    int getIdToAssign();
    Map<Integer, Guest> getAllUsers();
    Registered getUserByName(String username);
}
