package com.halilovindustries.websocket;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class DatabaseEventBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final List<Consumer<DatabaseStatusEvent>> listeners = new CopyOnWriteArrayList<>();

    public static class DatabaseStatusEvent {
        private final boolean connected;
        private final String message;
        
        public DatabaseStatusEvent(boolean connected, String message) {
            this.connected = connected;
            this.message = message;
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    public Registration register(Consumer<DatabaseStatusEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
    
    public void broadcastEvent(boolean connected, String message) {
        DatabaseStatusEvent event = new DatabaseStatusEvent(connected, message);
        executor.execute(() -> 
            listeners.forEach(listener -> listener.accept(event))
        );
    }
}