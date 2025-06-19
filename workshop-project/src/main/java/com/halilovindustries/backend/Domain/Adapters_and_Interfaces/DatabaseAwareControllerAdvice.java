package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class DatabaseAwareControllerAdvice {

    private final DatabaseHealthService databaseHealthService;

    @Autowired
    public DatabaseAwareControllerAdvice(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        databaseHealthService.reportDatabaseError(ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", "Database is currently unavailable. System is in maintenance mode.");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
    
    @ExceptionHandler(MaintenanceModeException.class)
    public ResponseEntity<Map<String, Object>> handleMaintenanceModeException(MaintenanceModeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", ex.getMessage());
        body.put("maintenanceMode", true);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}