package com.halilovindustries.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import com.halilovindustries.backend.Domain.Response;

import java.util.function.Supplier;

public abstract class DatabaseAwareService {
    
    @Autowired
    protected DatabaseHealthService dbHealthService;
    
    /**
     * Execute a database operation with proper error handling for Response-based services
     */
    protected <T> Response<T> executeDbOperationWithResponse(Supplier<Response<T>> operation) {
        try {
            return operation.get();
        } catch (Exception e) {
            // Check if it's a database connectivity issue
            if (isDbConnectionException(e)) {
                dbHealthService.reportDatabaseError(e);
            }
            return Response.error("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Handle database exception and report it through the health service
     */
    protected void handleDatabaseException(Exception e) {
        if (isDbConnectionException(e)) {
            dbHealthService.reportDatabaseError(e);
        }
    }
    
    private boolean isDbConnectionException(Exception e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }
        
        String message = e.getMessage().toLowerCase();
        String className = e.getClass().getName();
        
        return className.contains("SQL") ||
               className.contains("Database") ||
               className.contains("Connection") ||
               className.contains("JDBCConnection") ||
               className.contains("DataAccessResource") ||
               className.contains("CannotCreateTransaction") ||
               message.contains("database") ||
               message.contains("connection") ||
               message.contains("sql") ||
               message.contains("timeout") ||
               message.contains("could not open connection");
    }

    protected boolean isDatabaseAvailable() {
        try {
            return dbHealthService.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Execute a database operation that can be skipped if database is not available
     * (useful for scheduled tasks)
     */
    protected void executeSkippableOperation(Runnable operation) {
        if (!isDatabaseAvailable()) {
            // Skip operation if database is not available
            return;
        }
        
        try {
            operation.run();
        } catch (Exception e) {
            handleDatabaseException(e);
            // Don't propagate exception for skippable operations
        }
    }
}