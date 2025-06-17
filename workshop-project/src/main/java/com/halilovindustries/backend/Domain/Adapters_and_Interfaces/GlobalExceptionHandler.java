package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

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
    @ResponseBody
    public ResponseEntity<String> handleDatabaseConnectionIssue(Exception ex) {
        logger.warning("Database connection issue detected: " + ex.getMessage());
        // Report the error to the health service which will broadcast to active UIs
        dbHealthService.reportDatabaseError(ex);
        
        // Return an error response for REST API calls
        return new ResponseEntity<>("Database connection issue. Please try again later.", 
                                    HttpStatus.SERVICE_UNAVAILABLE);
    }
}