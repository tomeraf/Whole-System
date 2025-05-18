package com.halilovindustries.backend.Domain.DTOs;

import java.time.LocalDateTime;

public class NotificationDTO {

    private int id;
    private String userId;
    private String message;
    private LocalDateTime timestamp;
    public NotificationDTO(int id, String userId, String message, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }
    public String getUserId() {
        return userId;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
