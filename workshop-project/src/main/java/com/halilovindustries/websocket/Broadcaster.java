package com.halilovindustries.websocket;

import com.halilovindustries.backend.Domain.DTOs.Pair;
import com.vaadin.flow.component.UI;

// Functional interface for a registration that can be removed.

import com.vaadin.flow.shared.Registration;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;


/**
 * Handles broadcasting messages to registered Vaadin clients.
 */
public class Broadcaster {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Map<String, List<Consumer<String>>> listeners = new ConcurrentHashMap<>();
    // Store mapping of sessionId to userUuid and listener
    private static final Map<String, Pair<String, Consumer<String>>> sessionListeners = new ConcurrentHashMap<>();
    private static Broadcaster instance = null;

    private Broadcaster() {
        // Private constructor to prevent instantiation
    }
    public static synchronized Broadcaster getInstance() {
        if (instance == null) {
            instance = new Broadcaster();
        }
        return instance;
    }

    /**
     * Registers a listener for a specific user UUID.
     * @param userUuid the user ID
     * @param listener the listener to register
     * @return a Registration that can be used to remove the listener
     */
    public static synchronized Registration register(String sessionId, String userUuid, Consumer<String> listener) {
        // Generate unique session ID
        //String sessionId = UI.getCurrent().getUIId() + "-" + System.currentTimeMillis();
        
        // Store in both maps
        listeners.computeIfAbsent(userUuid, k -> new CopyOnWriteArrayList<>()).add(listener);
        sessionListeners.put(sessionId, new Pair<>(userUuid, listener));
        
        System.out.println("Listener registered for user: " + userUuid + " with session: " + sessionId);
        
        // Return a registration that can remove from both maps
        return () -> removeListenerBySessionId(sessionId);
    }

    /**
     * Broadcasts a message to all listeners registered for the given user UUID.
     * @param userUuid the user ID
     * @param message the message to send
     */
    public static boolean broadcast(String userUuid, String message) {
    System.out.println("Broadcasting message to user: " + userUuid);
    List<Consumer<String>> consumers = listeners.get(userUuid);
    if (CollectionUtils.isNotEmpty(consumers)) {
        System.out.println("Found " + consumers.size() + " listeners for user: " + userUuid);
        for (Consumer<String> consumer : consumers) {
            executor.execute(() -> {
                try {
                    consumer.accept(message);
                    System.out.println("Message successfully delivered to user: " + userUuid);
                } catch (Exception e) {
                    System.out.println("Error delivering message to user " + userUuid + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        return true;
    } else {
        System.out.println("No listeners found for user: " + userUuid);
        return false;
    }
}

    private static void removeListener(String userUuid, Consumer<String> listener) {
        List<Consumer<String>> userListeners = listeners.get(userUuid);
        if (userListeners != null) {
            userListeners.remove(listener);
            System.out.println("Listener removed for user: " + userUuid);
            if (userListeners.isEmpty()) {
                listeners.remove(userUuid);
            }
        }
    }

    public static int getListenerCount(String userUuid) {
        List<Consumer<String>> userListeners = listeners.get(userUuid);
        return userListeners != null ? userListeners.size() : 0;
    }

    public static synchronized void removeListenerBySessionId(String sessionId) {
        Pair<String, Consumer<String>> mapping = sessionListeners.remove(sessionId);
        if (mapping != null) {
            String userUuid = mapping.getKey();
            Consumer<String> listener = mapping.getValue();
            removeListener(userUuid, listener);
            System.out.println("Removed specific listener for session: " + sessionId);
        }
    }
}