
package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for NotificationDTO entities.
 */
public interface JpaNotificationRepository extends JpaRepository<NotificationDTO, Integer> {
    /**
     * Find all notifications for a given user.
     *
     * @param userId the ID of the user (as String)
     * @return a list of NotificationDTO instances belonging to that user
     */
    List<NotificationDTO> findByUserId(String userId);
}