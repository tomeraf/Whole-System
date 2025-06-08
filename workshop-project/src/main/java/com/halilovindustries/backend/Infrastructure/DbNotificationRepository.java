package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;
import com.halilovindustries.backend.Domain.Repositories.INotificationRepository;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;

@Primary
@Repository
public class DBNotificationRepository implements INotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepo;

    public DBNotificationRepository(JpaNotificationRepository jpaNotificationRepo) {
        this.jpaNotificationRepo = jpaNotificationRepo;
    }

    @Override
    public int getIdToAssign() {
        // Since JPA handles ID generation, return 0 or any dummy value
        return 0;
    }

    @Override
    public void deleteNotification(String userId, int id) {
        jpaNotificationRepo.deleteById(id);
    }

    @Override
    public Queue<NotificationDTO> getUserNotifications(String userId) {
        List<NotificationDTO> list = jpaNotificationRepo.findByUserId(userId);
        return new LinkedList<>(list);
    }

    @Override
    public void addNotification(String userId, NotificationDTO notification) {
        notification.setUserId(userId);
        jpaNotificationRepo.save(notification);
    }
}