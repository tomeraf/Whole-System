package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.logging.Logger;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());
    
    @Autowired
    private DatabaseHealthService dbHealthService;
    
    @ExceptionHandler({
        JDBCConnectionException.class,
        CannotCreateTransactionException.class,
        DataAccessResourceFailureException.class
    })
    public void handleDatabaseConnectionIssue(Exception ex) {
        logger.warning("Database connection issue detected: " + ex.getMessage());
        // Trigger a health check
        dbHealthService.isDatabaseConnected();
    }
}