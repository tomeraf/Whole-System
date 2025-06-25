package com.halilovindustries.backend.Service;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.MaintenanceModeException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DatabaseAwareService {
    
    @Autowired
    protected DatabaseHealthService databaseHealthService;
    
    /**
     * Check database health before performing operations
     * @param action Description of the action being performed
     * @throws MaintenanceModeException if system is in maintenance mode
     */
    protected void checkDatabaseHealth(String action) throws MaintenanceModeException {
        databaseHealthService.checkBeforeAction(action);
    }
    
    /**
     * Handle database exceptions consistently
     * @param e The exception that occurred
     */
    protected void handleDatabaseException(Exception e) {
        // Report to health service if it's a database connectivity issue
        if (isDatabaseException(e)) {
            databaseHealthService.reportDatabaseError(e);
        }
    }
    
    /**
     * Determine if the exception is related to database connectivity
     */
    private boolean isDatabaseException(Exception e) {
        // Check for common database exception types
        return e instanceof org.springframework.dao.DataAccessException ||
               e.getMessage() != null && (
                   e.getMessage().contains("database") ||
                   e.getMessage().contains("connection") ||
                   e.getMessage().contains("sql")
               );
    }
    
    /**
     * Execute an operation that can skip database checks during maintenance
     */
    protected void executeSkippableOperation(Runnable operation) {
        try {
            // Check if we're in maintenance mode first
            if (!databaseHealthService.isInMaintenanceMode()) {
                operation.run();
            } else {
                // Skip operation in maintenance mode
                System.out.println("Skipping database operation in maintenance mode");
            }
        } catch (Exception e) {
            handleDatabaseException(e);
        }
    }
}