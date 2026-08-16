package com.jujin.point.web;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory sliding-window rate limiter for auth endpoints
 * (signin/signup). Bounded: entries are evicted when the window slides.
 */
public final class LoginRateLimiter {

    private final Map<String, ArrayDeque<Long>> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMs;

    public LoginRateLimiter(int maxAttempts, long windowMs) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /** Returns true when the key may proceed; false when it is rate-limited. */
    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        var deque = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() <= now - windowMs) {
                deque.pollFirst();
            }
            if (deque.size() >= maxAttempts) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
