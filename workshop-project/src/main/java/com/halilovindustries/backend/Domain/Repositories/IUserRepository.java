package com.halilovindustries.backend.Domain.Repositories;

import com.halilovindustries.backend.Domain.User.*;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserRepository {
    void saveUser(Guest user);
    void saveUser(Registered user);
    void removeGuestById(int id);
    Guest getUserById(int id);
    
    int getIdToAssign(); // It will give a unique ID for the user
    
    Registered getUserByName(String username);

    List<Registered> getAllRegisteredUsers();

    List<Integer> getAllRegisteredsByShopAndPermission(int shopID, Permission permission);
}
