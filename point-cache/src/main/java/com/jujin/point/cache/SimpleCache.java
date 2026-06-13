package com.jujin.point.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A simple thread-safe in-memory cache with TTL support.
 */
public class SimpleCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
    private final long defaultTtlMs;

    private record CacheEntry<V>(V value, long expireAt) {}

    public SimpleCache(long defaultTtlMs) {
        this.defaultTtlMs = defaultTtlMs;
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

    public V getOrCompute(K key, Supplier<V> supplier) {
        var existing = get(key);
        if (existing != null) return existing;
        var value = supplier.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtlMs);
    }

    public void put(K key, V value, long ttlMs) {
        map.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }

    public void invalidate(K key) {
        map.remove(key);
    }

    public void invalidateAll() {
        map.clear();
    }

    public int size() {
        return map.size();
    }
}
