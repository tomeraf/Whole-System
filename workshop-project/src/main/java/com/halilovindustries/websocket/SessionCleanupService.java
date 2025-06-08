package com.halilovindustries.websocket;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionCleanupService {
    // Map of sessionId to last ping timestamp
    private static final Map<String, Long> lastPingTimes = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 60000; // 1 minute timeout for testing

    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void cleanupInactiveSessions() {
        long now = System.currentTimeMillis();
        
        lastPingTimes.entrySet().removeIf(entry -> {
            String sessionId = entry.getKey();
            long lastPing = entry.getValue();
            
            if (now - lastPing > SESSION_TIMEOUT_MS) {
                System.out.println("Session timeout detected for: " + sessionId);
                Broadcaster.removeListenerBySessionId(sessionId);
                return true;
            }
            return false;
        });
    }
}