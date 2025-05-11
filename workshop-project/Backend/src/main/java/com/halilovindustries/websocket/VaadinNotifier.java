package com.halilovindustries.websocket;

public class VaadinNotifier implements INotifier {

    @Override
    public boolean notifyUser(String userId, String message) {
        Broadcaster.broadcast(userId, message);
        return true;
    }
}

