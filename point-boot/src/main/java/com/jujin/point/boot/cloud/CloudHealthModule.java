package com.jujin.point.boot.cloud;

import com.jujin.freeway.cloud.health.CloudHealthContributor;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.http.HealthResult;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;

/**
 * Cloud readiness for dependencies the mesh cannot see.
 *
 * <p>freeway's cloud bundle owns two probes with different questions:
 * {@code /health/live} (is the process serving?) and {@code /health/ready}
 * (can it actually do its job?). Readiness aggregates every
 * {@link CloudHealthContributor}, and the aggregation is empty — therefore
 * always OK — until the application contributes one. This module adds the
 * database check so a cloud instance whose database is unreachable stops
 * reporting ready instead of accepting traffic behind a green probe.</p>
 *
 * <p>This is deliberately separate from the single-machine {@code HealthCheck}
 * bound in {@code PointModule}: that one is served by {@code /healthz} and is
 * unchanged. Only the cloud composition installs this module — the monolith
 * has no {@code /health/ready} route to feed.</p>
 */
public class CloudHealthModule implements ModuleEx {

    /** Contributor name reported by {@code /health/ready}. */
    public static final String DB_CONTRIBUTOR = "point-db";

    @Override
    public void bind(Binder binder) {
        binder
            .contribute(CloudHealthContributor.class)
            .addDeferred("point-db-health", container -> new CloudHealthContributor() {
                @Override
                public String name() {
                    return DB_CONTRIBUTOR;
                }

                @Override
                public HealthResult check() {
                    try {
                        var db = container.get(Database.class);
                        db.query("SELECT 1").list(Row.class);
                        return HealthResult.ok();
                    } catch (Exception e) {
                        return HealthResult.unhealthy("db: unreachable");
                    }
                }
            });
    }
}
