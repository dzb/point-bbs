package com.jujin.point.boot;

import com.jujin.freeway.db.DbModule;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.admin.AdminWebModule;
import com.jujin.point.service.ServiceModule;
import com.jujin.point.web.WebModule;

/**
 * The base modules shared by every point deployment.
 *
 * <p>freeway 1.5.6: module composition is a flat declaration list at the
 * entry point — instances and class declarations mix directly, no tree
 * assembly. Kept in one place so the monolith launcher ({@link PointApp})
 * and the cloud launcher cannot drift on which modules are composed — the
 * cloud shape only appends the mesh modules on top of this list.
 */
final class PointModules {

    static ModuleEx[] base() {
        return new ModuleEx[] {
            new DbModule(),
            new HttpModule(),
            new ServiceModule(),
            new WebModule(),
            new AdminWebModule(),
            new PointModule(),
        };
    }

    private PointModules() {}
}
