package com.jujin.point.web;

import com.jujin.freeway.http.websocket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks authenticated WebSocket sessions per user and pushes small JSON
 * notifications (currently just a "refresh" ping that makes the SPA re-fetch
 * the unread count — the 15s polling stays as a fallback).
 */
public class NotificationHub {

    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);

    private final Map<Long, Set<WebSocketSession>> byUser = new ConcurrentHashMap<>();

    public void register(long userId, WebSocketSession session) {
        byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(long userId, WebSocketSession session) {
        var sessions = byUser.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) byUser.remove(userId);
    }

    /** Push a text frame to every live session of the user; drops dead sessions. */
    public void notifyUser(long userId, String text) {
        var sessions = byUser.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        for (var session : sessions) {
            try {
                session.sendText(text);
            } catch (IOException e) {
                log.debug("ws send failed, dropping session: {}", e.getMessage());
                sessions.remove(session);
            }
        }
        if (sessions.isEmpty()) byUser.remove(userId);
    }

    /** Push the standard "refresh" ping. */
    public void pingUnread(long userId) {
        notifyUser(userId, "{\"type\":\"refresh\"}");
    }
}
