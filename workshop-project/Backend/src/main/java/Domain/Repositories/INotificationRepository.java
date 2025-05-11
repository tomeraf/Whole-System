package Domain.Repositories;

import java.util.Queue;

import Domain.DTOs.NotificationDTO;

public interface INotificationRepository{
    void addNotification(int userId ,NotificationDTO notification);
    Queue<NotificationDTO> getUserNotifications(int userId);
    void deleteNotification(int userId, int notificationId);
    int getIdToAssign();
}
    
