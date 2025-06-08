package com.halilovindustries.websocket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    // Change this from GetMapping to PostMapping
    @PostMapping("/unregister-notification")
    //@GetMapping("/unregister-notification")
    public void unregisterNotification(@RequestParam String sessionId) {
        System.out.println("Browser closed for session: " + sessionId + " - cleaning up broadcaster");
        
        // Use the session ID to remove just this specific listener
        Broadcaster.removeListenerBySessionId(sessionId);
    }
}