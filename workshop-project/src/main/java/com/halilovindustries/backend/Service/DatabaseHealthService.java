package com.halilovindustries.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.MaintenanceModeException;
//import com.halilovindustries.websocket.DatabaseEventBroadcaster;

import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DatabaseHealthService {
    
    private final JdbcTemplate jdbcTemplate;
    //private final DatabaseEventBroadcaster broadcaster;
    private boolean isHealthy = true;
    private final AtomicBoolean inMaintenanceMode = new AtomicBoolean(false);
    
    @Autowired
    public DatabaseHealthService(JdbcTemplate jdbcTemplate
    //, DatabaseEventBroadcaster broadcaster
    ) {
        this.jdbcTemplate = jdbcTemplate;
        //this.broadcaster = broadcaster;
    }
    
    @PostConstruct
    public void init() {
        // Check database connectivity on startup
        isDatabaseConnected();
        System.out.println("Initial database health check: " + (isHealthy ? "Connected" : "Maintenance mode"));
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Broadcast initial state when application is fully ready
        if (!isHealthy) {
            //broadcaster.broadcastEvent(false, "System is in maintenance mode: Database is unavailable. You can browse but cannot perform operations.");
            inMaintenanceMode.set(true);
        }
    }
    
    public boolean isDatabaseConnected() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (!isHealthy) {
                // Only broadcast if state changed from unhealthy to healthy
                isHealthy = true;
                inMaintenanceMode.set(false);
                //broadcaster.broadcastEvent(true, "Database connection restored! System is fully operational.");
            } else {
                isHealthy = true;
            }
            return true;
        } catch (Exception e) {
            if (isHealthy) {
                // Only broadcast if state changed from healthy to unhealthy
                isHealthy = false;
                inMaintenanceMode.set(true);
                //broadcaster.broadcastEvent(false, "System is in maintenance mode: Database is unavailable. You can browse but cannot perform operations.");
            } else {
                isHealthy = false;
            }
            return false;
        }
    }
    
    public boolean isHealthy() {
        return isHealthy;
    }
    
    public boolean isInMaintenanceMode() {
        return inMaintenanceMode.get();
    }
    
    public void reportDatabaseError(Exception e) {
        isHealthy = false;
        inMaintenanceMode.set(true);
        // Always broadcast for each user action
        //broadcaster.broadcastEvent(false, "Database connection issue: The system cannot complete your request because the database is currently unavailable.");
    }

    public void reportActionFailure(String action) {
        if (!isHealthy) {
            //broadcaster.broadcastEvent(false, "Cannot " + action + " - Database is currently unavailable. Please try again later.");
        }
    }
    
    public void checkBeforeAction(String action) throws MaintenanceModeException {
        if (!isDatabaseConnected()) {
            reportActionFailure(action);
            throw new MaintenanceModeException("System is in maintenance mode. Cannot " + action);
        }
    }
}