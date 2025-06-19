package com.halilovindustries.websocket;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    
    private final DatabaseHealthService databaseHealthService;
    
    @Autowired
    public SystemStatusController(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        // Perform a fresh check on each status request
        boolean connected = databaseHealthService.isDatabaseConnected();
        
        Map<String, Object> status = new HashMap<>();
        status.put("status", "up");
        status.put("databaseAvailable", connected);
        status.put("maintenanceMode", !connected);
        
        return ResponseEntity.ok(status);
    }
}