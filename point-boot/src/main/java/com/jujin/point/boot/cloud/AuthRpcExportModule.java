package com.jujin.point.boot.cloud;

import com.jujin.freeway.cloud.rpc.RpcExport;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;

/**
 * Declares point-bbs's auth session service to the mesh: peers in the cloud
 * resolve user tokens through {@code POST /rpc/auth/validateToken} instead of
 * sharing the JWT secret.
 *
 * <p>Inert until the composition root also installs {@code CloudModule} —
 * without it nothing reads the {@link RpcExport} contribution and no
 * {@code /rpc/...} route exists. That is why {@code PointApp} (the local
 * single-machine tree) can carry this module on the classpath and run exactly
 * as before; {@link com.jujin.point.boot.PointCloudApp} is the shape that
 * activates the export.</p>
 */
public class AuthRpcExportModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Container-managed handler: injected, one instance, resolvable —
        // the endpoint serves the container's object, not a per-request copy.
        binder.bind(AuthSessionRpcService.class);
        binder.contribute(RpcExport.class)
            .add(RpcExport.of("auth", AuthSessionRpcService.class));
    }
}
