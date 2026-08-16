package com.jujin.point.admin;

import com.jujin.freeway.http.HttpContext;
import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.entity.OperateLog;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.db.Orm;

/**
 * Admin action audit trail — every mutating admin operation writes a row to
 * bbs_operate_log (operator, target, description).
 */
public final class AdminAudit {

    private AdminAudit() {}

    public static void log(
        HttpContext ctx,
        String opType,
        String dataType,
        long dataId,
        String description
    ) {
        try {
            long operatorId = AuthFilter.currentUser() != null
                ? AuthFilter.currentUser().userId()
                : 0L;
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
            org.slf4j.LoggerFactory.getLogger(AdminAudit.class)
                .warn("audit log write failed: {}", e.getMessage());
        }
    }
}
