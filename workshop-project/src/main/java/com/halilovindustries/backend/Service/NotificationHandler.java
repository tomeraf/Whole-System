package com.halilovindustries.backend.Service;

import com.halilovindustries.backend.Domain.Repositories.INotificationRepository;
import com.halilovindustries.websocket.INotifier;
import com.halilovindustries.backend.Domain.DTOs.NotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;

@Service
public class NotificationHandler {
    private final INotificationRepository repo;
    private final INotifier notifier;

    @Autowired
    public NotificationHandler(INotificationRepository repo,INotifier notifier) {
        this.notifier = notifier;
        this.repo = repo;
    }

    /**
     * Create a new notification for the given user, persist it,
     * and immediately push it over WebSocket.
     *
     * @param userId  your internal user ID
     * @param message the text or payload you want to send
     */
    public void notifyUser(String userId, String message) {
        if(!notifier.notifyUser(userId,message)){
            NotificationDTO dto = new NotificationDTO(repo.getIdToAssign(), userId, message, LocalDateTime.now());
            repo.addNotification(userId, dto);
        }
    }
    public void notifyUser(String userId){
        Queue<NotificationDTO> notifications = repo.getUserNotifications(userId);
        if(notifications.isEmpty()){
            return;
        }
        for(NotificationDTO notification : notifications){
            if(!notifier.notifyUser(userId,notification.getMessage()))
                break;
            deleteNotification(userId, notification.getId());
        }
    }

    /**
     * Fetch all pending notifications for that user.
     */
    public Queue<NotificationDTO> getNotifications(String userId) {
        return repo.getUserNotifications(userId);
    }

    /**
     * Remove one notification after the client has seen it (or clicked it).
     */
    public void deleteNotification(String userId, int notificationId) {
        repo.deleteNotification(userId, notificationId);
    }
    
    public void notifyUsers(List<Integer> userIds,String message) {
        for (int userId : userIds) {
               notifyUser(userId+"", message);
        }

    }
}