package com.halilovindustries.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.halilovindustries.websocket.DatabaseEventBroadcaster;

@Service
public class DatabaseHealthService {
    
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseEventBroadcaster broadcaster;
    private boolean isHealthy = true;
    
    @Autowired
    public DatabaseHealthService(JdbcTemplate jdbcTemplate, DatabaseEventBroadcaster broadcaster) {
        this.jdbcTemplate = jdbcTemplate;
        this.broadcaster = broadcaster;
    }
    
    public boolean isDatabaseConnected() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (!isHealthy) {
                // Only broadcast if state changed from unhealthy to healthy
                isHealthy = true;
                broadcaster.broadcastEvent(true, "Database connection restored!");
            } else {
                isHealthy = true;
            }
            return true;
        } catch (Exception e) {
            if (isHealthy) {
                // Only broadcast if state changed from healthy to unhealthy
                isHealthy = false;
                broadcaster.broadcastEvent(false, "Database connection lost. Please try again later.");
            } else {
                isHealthy = false;
            }
            return false;
        }
    }
    
    public boolean isHealthy() {
        return isHealthy;
    }
    
    // This is called when a user action fails due to DB issues
    public void reportDatabaseError(Exception e) {
        isHealthy = false;
        broadcaster.broadcastEvent(false, "Database connection issue: " + e.getMessage());
    }
}