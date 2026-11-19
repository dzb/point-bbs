package com.jujin.point.web;

import com.jujin.freeway.http.websocket.WebSocketEndpoint;
import com.jujin.freeway.http.websocket.WebSocketListener;
import com.jujin.freeway.http.websocket.WebSocketSession;
import com.jujin.point.service.AuthService;

/**
 * WebSocket endpoint at /ws/notify — authenticates with the same HttpOnly
 * session cookie the SPA uses for HTTP and registers the session for push
 * notifications. The browser sends same-origin cookies on the WS handshake,
 * so no token belongs in the URL; a credential in a query string leaks into
 * access logs, proxy logs and browser history. Services arrive via
 * constructor injection: the route is declared as
 * {@code WebSocketRoute.of("/notify", NotificationWsEndpoint.class)} and the
 * container builds this endpoint at startup.
 */
public class NotificationWsEndpoint implements WebSocketEndpoint {

    private final AuthService authService;
    private final NotificationHub hub;

    public NotificationWsEndpoint(AuthService authService, NotificationHub hub) {
        this.authService = authService;
        this.hub = hub;
    }

    @Override
    public WebSocketListener open(WebSocketSession session) {
        // Same-origin WS handshakes carry the HttpOnly session cookie.
        var token = com.jujin.point.service.AuthService.tokenFromCookie(
            session.header("Cookie").orElse(null)
        );
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
