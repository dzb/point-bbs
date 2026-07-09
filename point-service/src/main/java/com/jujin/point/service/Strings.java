package com.jujin.point.service;

/** Small, dependency-free string helpers shared across services. */
public final class Strings {
    private Strings() {}

    /** Truncate to at most {@code maxLen} characters, appending "..." when cut. */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
