package com.halilovindustries.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {
    
    private final JdbcTemplate jdbcTemplate;
    private boolean isHealthy = true;
    
    @Autowired
    public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public boolean isDatabaseConnected() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            isHealthy = true;
            return true;
        } catch (Exception e) {
            isHealthy = false;
            return false;
        }
    }
    
    public boolean isHealthy() {
        return isHealthy;
    }
}