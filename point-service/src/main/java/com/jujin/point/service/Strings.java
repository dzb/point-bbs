package com.jujin.point.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Small, dependency-free string helpers shared across services. */
public final class Strings {
    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Strings() {}

    /** Truncate to at most {@code maxLen} characters, appending "..." when cut. */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /** Human-readable local time for epoch millis (mute notices etc.). */
    public static String formatTime(long epochMillis) {
        return TIME_FMT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }
}
