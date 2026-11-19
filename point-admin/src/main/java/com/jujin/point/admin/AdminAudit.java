package com.jujin.point.admin;

import com.jujin.freeway.http.HttpExchange;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.service.OperateLogService;

/**
 * Admin action audit trail — every mutating admin operation writes a row to
 * bbs_operate_log (operator, target, description).
 *
 * <p>This is the admin-layer adapter: it resolves the operator from the
 * AuthApi service (served by the web layer, no compile-time dependency on
 * point-web) and the request's remote address, then hands both to
 * {@link OperateLogService}. The database write itself lives in the service
 * layer.</p>
 */
public final class AdminAudit {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(AdminAudit.class);

    private final AuthApi authApi;
    private final OperateLogService operateLogService;

    public AdminAudit(AuthApi authApi, OperateLogService operateLogService) {
        this.authApi = authApi;
        this.operateLogService = operateLogService;
    }

    public void log(
        HttpExchange ctx,
        String opType,
        String dataType,
        long dataId,
        String description
    ) {
        try {
            operateLogService.log(
                authApi.currentUserId(),
                opType,
                dataType,
                dataId,
                description,
                ctx.remoteAddress()
            );
        } catch (Exception e) {
            // Auditing must never break the admin action itself
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
