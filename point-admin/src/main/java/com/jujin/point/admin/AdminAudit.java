package com.jujin.point.admin;

import com.jujin.freeway.http.HttpContext;
import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.domain.entity.OperateLog;
import com.jujin.freeway.db.Orm;

/**
 * Admin action audit trail — every mutating admin operation writes a row to
 * bbs_operate_log (operator, target, description).
 *
 * Operator identity comes from the AuthApi CallBus consumer (served by the
 * web layer) — no compile-time dependency on point-web.
 */
public final class AdminAudit {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(AdminAudit.class);

    private AdminAudit() {}

    public static void log(
        HttpContext ctx,
        String opType,
        String dataType,
        long dataId,
        String description
    ) {
        try {
            long operatorId = AppContext.get(AuthApi.class).currentUserId();
            var log = new OperateLog(
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
            AppContext.get(Orm.class).insert(log);
        } catch (Exception e) {
            // Auditing must never break the admin action itself
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
