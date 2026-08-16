package com.jujin.point.web;

import com.jujin.freeway.http.websocket.WebSocketEndpoint;
import com.jujin.freeway.http.websocket.WebSocketListener;
import com.jujin.freeway.http.websocket.WebSocketSession;
import com.jujin.point.domain.AppContext;
import com.jujin.point.service.AuthService;

/**
 * WebSocket endpoint at /ws/notify?token=&lt;jwt&gt; — authenticates with the
 * same JWT the SPA uses for HTTP (query param, since browsers cannot set
 * headers on the WS handshake) and registers the session for push
 * notifications. Services resolve lazily via AppContext (same idiom as route
 * handlers), because the endpoint is constructed at module-bind time.
 */
public class NotificationWsEndpoint implements WebSocketEndpoint {

    @Override
    public WebSocketListener open(WebSocketSession session) {
        var authService = AppContext.get(AuthService.class);
        var hub = AppContext.get(NotificationHub.class);
        var token = session.queryParam("token").orElse(null);
        if (token == null) {
            // Same-origin WS handshakes carry the HttpOnly session cookie
            token = com.jujin.point.service.AuthService.tokenFromCookie(
                session.header("Cookie").orElse(null)
            );
        }
        Long userId = (token == null)
            ? null
            : authService.validateToken(token).map(u -> u.userId()).orElse(null);
        if (userId == null) {
            try {
                session.close(4401, "unauthorized");
            } catch (Exception e) {
                // session already gone
            }
            return WebSocketListener.NOOP;
        }
        hub.register(userId, session);
        return new WebSocketListener() {
            @Override
            public void onClose(int code, String reason, boolean remote) {
                hub.unregister(userId, session);
            }

            @Override
            public void onError(Throwable error) {
                hub.unregister(userId, session);
            }
        };
    }
}
