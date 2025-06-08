package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.Repositories.INotificationRepository;
import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Repository
public class MemoryNotificationRepository implements INotificationRepository {
    private int counterId = 0;
    private List<Integer> removedIds = new ArrayList<>(); // List of removed IDs
    private final Map<String, Queue<NotificationDTO>> userNotifications = new ConcurrentHashMap<>();

    public void addNotification(String userId, NotificationDTO notification) {
        // Implement logic to add notification to the user's queue
        userNotifications.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>()).add(notification);
    }

    @Override
    public Queue<NotificationDTO> getUserNotifications(String userId) {
        // Implement logic to find notifications by user ID
        Queue<NotificationDTO> notifications = this.userNotifications.get(userId);
        if (notifications == null) {
            throw new RuntimeException("user id:" + userId + " doesn't have any notifications");
        }
        return notifications;
    }

    @Override
    public void deleteNotification(String userId, int notificationId) {
        // Implement logic to delete a notification by user ID and notification ID
        Queue<NotificationDTO> notifications = this.userNotifications.get(userId);
        if (notifications == null) {
            throw new RuntimeException("user id:" + userId + " doesn't have any notifications");
        }
        NotificationDTO notificationToDelete = notifications.element();
        if(notificationToDelete.getId() == notificationId){
            this.userNotifications.get(userId).remove(notificationToDelete);
        }else {
            throw new RuntimeException("user id:" + userId + " oldest notification is not with id:" + notificationId);
        }
    }

    public int getIdToAssign() {
        if (removedIds.isEmpty()) {
            return counterId = counterId + 1; // Increment the counter for a new ID
        } else {
            return removedIds.remove(removedIds.size() - 1); // Reuse a removed ID
        }
    }

}
