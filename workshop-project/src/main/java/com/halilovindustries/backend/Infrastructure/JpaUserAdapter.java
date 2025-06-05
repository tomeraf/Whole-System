package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.User.IRole;
import com.halilovindustries.backend.Domain.User.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaUserAdapter extends JpaRepository<Registered, Long> {
    Optional<Registered> findByUsername(String username);

    @Query("SELECT r FROM IRole r WHERE r.appointerID = :appointerId AND r.shopID = :shopId")
    List<IRole> findAppointmentsByAppointerAndShop(@Param("appointerId") int appointerId, @Param("shopId") int shopId);
}