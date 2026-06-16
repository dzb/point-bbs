package com.jujin.point.cache;

import com.jujin.point.domain.entity.SysConfig;
import com.jujin.point.domain.entity.Tag;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.entity.User;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Module2;
import com.jujin.freeway.ioc.Scope;

/**
 * Cache module — binds typed caches as singletons.
 */
public class PointCacheModule implements Module2 {

    @Override
    public void bind(Binder binder) {
        // 5 minute TTL for user cache
        binder.bind(UserCache.class).to(new UserCache(5 * 60 * 1000)).scope(Scope.SINGLETON);
        // 10 minute TTL for sys config cache
        binder.bind(SysConfigCache.class).to(new SysConfigCache(10 * 60 * 1000)).scope(Scope.SINGLETON);
        // 2 minute TTL for topic cache
        binder.bind(TopicCache.class).to(new TopicCache(2 * 60 * 1000)).scope(Scope.SINGLETON);
        // 5 minute TTL for tag cache
        binder.bind(TagCache.class).to(new TagCache(5 * 60 * 1000)).scope(Scope.SINGLETON);
    }

    // Typed cache wrappers
    public static class UserCache extends SimpleCache<Long, User> {
        public UserCache(long ttlMs) { super(ttlMs); }
    }

    public static class SysConfigCache extends SimpleCache<String, SysConfig> {
        public SysConfigCache(long ttlMs) { super(ttlMs); }
    }

    public static class TopicCache extends SimpleCache<Long, Topic> {
        public TopicCache(long ttlMs) { super(ttlMs); }
    }

    public static class TagCache extends SimpleCache<Long, Tag> {
        public TagCache(long ttlMs) { super(ttlMs); }
    }
}
