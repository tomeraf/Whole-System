package com.halilovindustries.backend.Domain.init;

import com.halilovindustries.backend.Service.DatabaseHealthService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Initializer {

    private final InitService initService;
    private final DatabaseHealthService databaseHealthService;

    public Initializer(InitService initService,
                       DatabaseHealthService databaseHealthService) {
        this.initService = initService;
        this.databaseHealthService = databaseHealthService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        boolean dbAvailable = databaseHealthService.isDatabaseConnected();
        boolean validForMaintenance = isInitFileValidForMaintenanceMode();  // keep your existing logic

        if (dbAvailable) {
            System.out.println("Database is available. Initializing system...");
            initService.initializeSystem();
        }
        else if (validForMaintenance) {
            System.out.println("Database is unavailable but init file is valid for maintenance mode.");
        }
        else {
            throw new RuntimeException("Cannot start system: Database unavailable and init file requires database access");
        }
    }

    private boolean isInitFileValidForMaintenanceMode() {
        return initService.isInitFileValidForMaintenanceMode();
    }
}
