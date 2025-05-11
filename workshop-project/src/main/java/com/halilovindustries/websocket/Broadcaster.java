package com.halilovindustries.websocket;

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
     * Registers a listener (usually from a Vaadin UI) for a specific user UUID.
     * @param userUuid the user ID
     * @param listener a consumer to handle messages
     * @return a Registration to remove the listener
     */
    public static synchronized Registration register(String userUuid, Consumer<String> listener) {
        listeners.computeIfAbsent(userUuid, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> removeListener(userUuid, listener);
    }

    /**
     * Broadcasts a message to all listeners registered for the given user UUID.
     * @param userUuid the user ID
     * @param message the message to send
     */
    public static void broadcast(String userUuid, String message) {
        List<Consumer<String>> consumers = listeners.get(userUuid);
        if (CollectionUtils.isNotEmpty(consumers)) {
            for (Consumer<String> consumer : consumers) {
                executor.execute(() -> consumer.accept(message));
            }
        }
    }

    private static void removeListener(String userUuid, Consumer<String> listener) {
        List<Consumer<String>> userListeners = listeners.get(userUuid);
        if (userListeners != null) {
            userListeners.remove(listener);
            if (userListeners.isEmpty()) {
                listeners.remove(userUuid);
            }
        }
    }
}
