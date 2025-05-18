package com.halilovindustries.backend.Domain.Repositories;

import java.util.Queue;

import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;

public interface INotificationRepository{
    void addNotification(String userId ,NotificationDTO notification);
    Queue<NotificationDTO> getUserNotifications(String userId);
    void deleteNotification(String userId, int notificationId);
    int getIdToAssign();
}
    
