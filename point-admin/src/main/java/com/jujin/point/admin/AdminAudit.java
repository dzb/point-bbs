package com.jujin.point.admin;

import com.jujin.freeway.db.Orm;
import com.jujin.freeway.http.HttpContext;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.domain.entity.OperateLog;

/**
 * Admin action audit trail — every mutating admin operation writes a row to
 * bbs_operate_log (operator, target, description).
 *
 * Operator identity comes from the AuthApi service (served by the
 * web layer) — no compile-time dependency on point-web. AuthApi and Orm
 * arrive via constructor injection; admin route handlers receive this
 * bound instance.
 */
public final class AdminAudit {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(AdminAudit.class);

    private final AuthApi authApi;
    private final Orm orm;

    public AdminAudit(AuthApi authApi, Orm orm) {
        this.authApi = authApi;
        this.orm = orm;
    }

    public void log(
        HttpContext ctx,
        String opType,
        String dataType,
        long dataId,
        String description
    ) {
        try {
            long operatorId = authApi.currentUserId();
            var entry = new OperateLog(
                operatorId,
                opType,
                dataType,
                dataId,
                description,
                ctx.remoteAddress(),
                null,
                null,
                System.currentTimeMillis()
            );
            orm.insert(entry);
        } catch (Exception e) {
            // Auditing must never break the admin action itself
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
