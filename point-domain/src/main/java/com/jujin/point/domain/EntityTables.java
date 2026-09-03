package com.jujin.point.domain;

import java.util.Map;

/**
 * Canonical mapping between the wire-level {@code entityType} strings carried
 * by events/API payloads and the physical tables they denote. Single source of
 * truth — services and handlers must not re-declare this switch inline.
 */
public final class EntityTables {
    private static final Map<String, String> TABLES = Map.of(
        "topic", "bbs_topic",
        "article", "bbs_article",
        "comment", "bbs_comment"
    );

    private EntityTables() {}

    /** Physical table name for an entityType, or null when unknown. */
    public static String tableOf(String entityType) {
        if (entityType == null) return null;
        return TABLES.get(entityType);
    }
}
