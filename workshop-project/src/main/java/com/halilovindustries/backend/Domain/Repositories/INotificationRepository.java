package com.halilovindustries.backend.Domain.Repositories;

import java.util.Queue;

import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;

public interface INotificationRepository{
    void addNotification(int userId ,NotificationDTO notification);
    Queue<NotificationDTO> getUserNotifications(int userId);
    void deleteNotification(int userId, int notificationId);
    int getIdToAssign();
}
    
