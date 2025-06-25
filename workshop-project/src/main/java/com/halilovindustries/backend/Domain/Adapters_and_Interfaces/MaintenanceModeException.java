package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

public class MaintenanceModeException extends RuntimeException {
    
    public MaintenanceModeException(String message) {
        super(message);
    }
    
    public MaintenanceModeException(String message, Throwable cause) {
        super(message, cause);
    }
}