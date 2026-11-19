package com.jujin.point.boot;

import com.jujin.freeway.cloud.discovery.StaticModule;

/**
 * point in the cloud — the mesh deployment shape of the same application.
 *
 * Composes the identical base module list as {@link PointApp} plus the cloud
 * bundle and the auth export declaration, serving
 * {@code POST /rpc/auth/validateToken} for peers.
 *
 * <p>The cloud bundle does not install discovery. freeway 2.0.1's config-driven
 * {@link StaticModule} supplies it: {@code F2.cloud.discovery.static.services}
 * maps the service id to the platform's stable endpoint (a Kubernetes Service
 * DNS name or another VIP). There is no registration or heartbeat — the
 * platform Service owns membership. A deployment that needs dynamic membership
 * installs {@code MemberModule} plus freeway-ext's Fleet or Consul adapter.</p>
 *
 * <p>Business code and the local {@code AuthApi} seam (in-process,
 * ScopedValue-based identity) are unchanged — cross-process peers ride RPC,
 * this process keeps answering "who is the current request" from its own
 * request thread.
 *
 * <p>Run with the point-boot fat jar and a profile that defines static
 * discovery: {@code java -cp point-boot.jar com.jujin.point.boot.PointCloudApp
 * --F2.profile=prod}.
 * Discovery and cloud runtime keys live under {@code F2.cloud.*} (canonical
 * {@code freeway.cloud.*}).</p>
 */
public class PointCloudApp {

    public static void main(String[] args) throws Exception {
        // Same base list as the monolith plus the cloud bundle, the auth
        // export and the database readiness contributor (see PointLauncher).
        PointLauncher.run(args, true);
    }
}
