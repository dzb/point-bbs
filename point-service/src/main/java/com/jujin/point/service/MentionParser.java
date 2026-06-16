package com.jujin.point.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class MentionParser {
    private static final Pattern MENTION = Pattern.compile("@([\\w一-鿿]{1,32})");

    private MentionParser() {}

    /** Extract unique @username mentions from content, preserving order. */
    public static Set<String> extractMentions(String content) {
        if (content == null || content.isBlank()) return Set.of();
        var matcher = MENTION.matcher(content);
        var mentions = new LinkedHashSet<String>();
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
