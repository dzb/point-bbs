package com.jujin.point.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A simple thread-safe in-memory cache with TTL support and max-size guard.
 */
public class SimpleCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
    private final long defaultTtlMs;
    private final int maxSize;

    private record CacheEntry<V>(V value, long expireAt) {}

    public SimpleCache(long defaultTtlMs) {
        this(defaultTtlMs, 10_000);
    }

    public SimpleCache(long defaultTtlMs, int maxSize) {
        this.defaultTtlMs = defaultTtlMs;
        this.maxSize = maxSize;
    }

    public V get(K key) {
        var entry = map.get(key);
        if (entry == null) return null;
        if (entry.expireAt < System.currentTimeMillis()) {
            map.remove(key);
            return null;
        }
        return entry.value;
    }

    /** Thread-safe compute-if-absent using computeIfAbsent. */
    public V getOrCompute(K key, Supplier<V> supplier) {
        // Fast path: check without lock
        var existing = get(key);
        if (existing != null) return existing;
        // computeIfAbsent ensures only one supplier invocation per key
        var entry = map.computeIfAbsent(key, k -> {
            var value = supplier.get();
            if (value == null) return null;
            return new CacheEntry<>(value, System.currentTimeMillis() + defaultTtlMs);
        });
        // If entry expired after compute, treat as miss
        if (entry != null && entry.expireAt < System.currentTimeMillis()) {
            map.remove(key);
            return null;
        }
        return entry != null ? entry.value : null;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtlMs);
    }

    public void put(K key, V value, long ttlMs) {
        if (value == null) return;
        // Evict if over capacity
        if (map.size() >= maxSize && !map.containsKey(key)) {
            cleanExpired();
            // If still full, skip caching this entry
            if (map.size() >= maxSize) return;
        }
        map.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }

    public void invalidate(K key) {
        map.remove(key);
    }

    public void invalidateAll() {
        map.clear();
    }

    /** Remove all expired entries. Call periodically or on cache pressure. */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        map.forEach((key, entry) -> {
            if (entry.expireAt < now) map.remove(key);
        });
    }

    public int size() {
        return map.size();
    }
}
