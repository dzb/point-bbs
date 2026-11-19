package com.jujin.point.boot;

/**
 * point application entry point — the single-machine monolith.
 *
 * <p>freeway 2.0: module composition is a flat declaration list built at the
 * entry point — the app holds every feature module in bind order — with
 * {@code autoDiscovery(false)} so nothing is silently installed a second time.
 * {@link PointCloudApp} is the mesh deployment shape; it composes this same
 * base list plus the cloud modules.</p>
 *
 * <p>The startup choreography (logging, banner, port report) lives in
 * {@link PointLauncher}. The two mains stay separate on purpose: {@code
 * point.sh} detects the deployment shape from the process command line.</p>
 */
public class PointApp {

    public static void main(String[] args) throws Exception {
        PointLauncher.run(args, false);
    }
}
