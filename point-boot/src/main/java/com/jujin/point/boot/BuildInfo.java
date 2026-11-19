package com.jujin.point.boot;

import java.io.InputStream;
import java.util.Properties;

/**
 * Build metadata for the startup banner.
 *
 * <p>{@code build-info.properties} is filtered by Maven at build time, so the
 * banner follows the POM instead of a hand-edited string that goes stale on
 * the next version bump. Running from an IDE (no filtered resource) falls back
 * to readable placeholders.</p>
 */
final class BuildInfo {

    private static final Properties VALUES = load();

    private BuildInfo() {}

    /** Application version, e.g. {@code 1.0.7}. */
    static String pointVersion() {
        return value("version", "dev");
    }

    /** The freeway framework the jar was built against. */
    static String freewayVersion() {
        return value("freeway.version", "unknown");
    }

    private static String value(String key, String fallback) {
        var raw = VALUES.getProperty(key);
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return fallback;
        }
        return raw.trim();
    }

    private static Properties load() {
        var properties = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception ignored) {
            // Absent or unreadable metadata is not a startup problem.
        }
        return properties;
    }
}
