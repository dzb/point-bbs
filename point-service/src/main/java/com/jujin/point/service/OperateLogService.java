package com.jujin.point.service;

import com.jujin.freeway.db.Orm;
import com.jujin.point.domain.entity.OperateLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write side of {@code bbs_operate_log}.
 *
 * <p>Kept in the service layer so the admin module does not open a database
 * handle of its own; the caller still owns "who" (AuthApi) and "from where"
 * (the request's remote address), which are web concerns.</p>
 */
public class OperateLogService {

    private static final Logger log = LoggerFactory.getLogger(OperateLogService.class);

    private final Orm orm;

    public OperateLogService(Orm orm) {
        this.orm = orm;
    }

    /**
     * Appends an audit row. Never throws: an audit failure must not break the
     * operation being audited.
     */
    public void log(
        long operatorId,
        String opType,
        String dataType,
        long dataId,
        String description,
        String remoteAddress
    ) {
        try {
            orm.insert(new OperateLog(
                operatorId,
                opType,
                dataType,
                dataId,
                description,
                remoteAddress,
                null,
                null,
                System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
