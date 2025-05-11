package com.halilovindustries.backend.Domain.DTOs;

import java.time.LocalDateTime;

public class NotificationDTO {

    private int id;
    private int userId;
    private String message;
    private LocalDateTime timestamp;
    public NotificationDTO(int id, int userId, String message, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }
    public int getUserId() {
        return userId;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
