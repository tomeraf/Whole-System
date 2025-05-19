package com.halilovindustries.websocket;

import org.springframework.stereotype.Component;

@Component
public class VaadinNotifier implements INotifier {

    @Override
    public boolean notifyUser(String userId,String message) {
        return Broadcaster.broadcast(userId, message);
        
    }
}

