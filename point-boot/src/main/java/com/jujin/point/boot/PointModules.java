package com.jujin.point.boot;

import com.jujin.freeway.db.DbModule;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.ioc.ModuleNode;
import com.jujin.point.admin.AdminWebModule;
import com.jujin.point.service.ServiceModule;
import com.jujin.point.web.WebModule;

/**
 * The base {@link ModuleNode} children shared by every point deployment.
 *
 * Kept in one place so the monolith launcher ({@link PointApp}) and the cloud
 * launcher cannot drift on which modules are composed — the cloud shape only
 * appends the mesh modules on top of this list.
 */
final class PointModules {

    static ModuleNode[] base() {
        return new ModuleNode[] {
            ModuleNode.of(new DbModule()),
            ModuleNode.of(new HttpModule()),
            ModuleNode.of(new ServiceModule()),
            ModuleNode.of(new WebModule()),
            ModuleNode.of(new AdminWebModule()),
            ModuleNode.of(new PointModule()),
        };
    }

    private PointModules() {}
}
