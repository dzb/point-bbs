package com.jujin.point.boot.cloud;

import com.jujin.freeway.ioc.annotation.Inject;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;

/**
 * Cross-process session validation — the wire surface of point-bbs auth,
 * exported to the mesh as {@code RpcExport.of("auth", ...)} by
 * {@link AuthRpcExportModule}.
 *
 * <p>Deliberately <b>not</b> the local {@code AuthApi} seam: that one answers
 * "who is the current request" from a ScopedValue bound by AuthFilter on this
 * process's request thread, and an inbound RPC call is a different request —
 * exporting it would silently return {@code null} across the process boundary.
 * Here identity is a parameter: the caller presents the end-user token
 * explicitly and gets the same {@link CurrentUser} the local filter resolves.
 * A pure token→identity function is safe to export; ambient state is not.</p>
 *
 * <p>Only this class's public methods are reachable on the wire (the declared
 * type is the boundary). Peers reaching it ride the mesh's transport security
 * and discovery — this endpoint does not itself authorize callers beyond
 * what the token proves.</p>
 */
public class AuthSessionRpcService {

    private final AuthService authService;

    @Inject
    public AuthSessionRpcService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Resolves a session token to its user, or {@code null} when the token is
     * missing, malformed, expired or otherwise invalid.
     */
    public CurrentUser validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return authService.validateToken(token).orElse(null);
    }
}
