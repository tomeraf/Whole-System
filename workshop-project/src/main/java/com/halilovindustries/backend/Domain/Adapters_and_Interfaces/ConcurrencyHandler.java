package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

/**
 * Central registry for shop- and item-level locks.
 * Shared as a singleton bean via Spring.
 */
@Component
public class ConcurrencyHandler {
    // Read/write locks per shop
    private final ConcurrentHashMap<Integer, ReentrantReadWriteLock> shopLocks = new ConcurrentHashMap<>();
    // Fine-grained locks per item
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();
    private final ReentrantLock globalShopCreationLock = new ReentrantLock();
    private final ConcurrentHashMap<String, ReentrantLock> usernameLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> shopUserLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> bidLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> messageLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> conditionLocks = new ConcurrentHashMap<>();

    private ReentrantReadWriteLock getShopRWLock(int shopId) {
        return shopLocks.computeIfAbsent(shopId, id -> new ReentrantReadWriteLock());
    }

    /**
     * For operations that only need to read/lock a single item concurrently
     * while blocking only when a full-shop write is in progress.
     */
    public Lock getShopReadLock(int shopId) {
        return getShopRWLock(shopId).readLock();
    }

    /**
     * For full-shop operations (e.g., supply updates, close-shop) that must be exclusive.
     */
    public Lock getShopWriteLock(int shopId) {
        return getShopRWLock(shopId).writeLock();
    }

    /**
     * For operations targeting a single item within a shop.
     */
    public ReentrantLock getItemLock(int shopId, int itemId) {
        String key = "shop:" + shopId + ":item:" + itemId;
        return itemLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * For operations that need to create a new shop.
     * This is a global lock to prevent multiple threads from creating the same shop at the same time.
     */
    public Lock getGlobalShopCreationLock() {
        return globalShopCreationLock;
    }

    /**
     * For operations that need to lock a username.
     * This is a global lock to prevent multiple threads from creating the same user at the same time.
     */
    public ReentrantLock getUsernameLock(String username) {
        return usernameLocks.computeIfAbsent(username, k -> new ReentrantLock());
    }

    /**
     * For operations that need to lock a user for specific shop.
     * This is a global lock to prevent multiple threads from creating the same user at the same time.
     */
    public ReentrantLock getShopUserLock(int shopId, String userName) {
        String key = "shop:" + shopId + ":user:" + userName;
        return shopUserLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * For operations that need to lock a bid for specific shop.
     * This is a global lock to prevent multiple threads from creating the same user at the same time.
     */
    public ReentrantLock getBidLock(int shopId, int bidId) {
        String key = "shop:" + shopId + ":bid:" + bidId;
        return bidLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * For operations that need to lock a message for specific shop.
     * This is a global lock to prevent multiple threads from creating the same user at the same time.
     */
    public ReentrantLock getMessageLock(int shopId, int messageId) {
        String key = "shop:" + shopId + ":bid:" + messageId;
        return messageLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public ReentrantLock getAuctionLock(int shopId, int auctionID) {
        String key = "shop:" + shopId + ":auction:" + auctionID;
        return auctionLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public ReentrantLock getConditionLock(int shopID, String conditionID) {
        String key = "shop:" + shopID + ":auction:" + conditionID;
        return conditionLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}
