package com.jujin.point.cache;

import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.domain.entity.SysConfig;
import com.jujin.point.domain.entity.Tag;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.entity.User;

/**
 * Cache module — binds typed caches.
 *
 * Freeway 1.3.2: instance bindings (.to(instance)) are implicitly SINGLETON.
 */
public class PointCacheModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Instance bindings are implicitly SINGLETON — no .scope(Scope.SINGLETON) needed
        binder.bind(UserCache.class).to(new UserCache(5 * 60 * 1000));
        binder
            .bind(SysConfigCache.class)
            .to(new SysConfigCache(10 * 60 * 1000));
        binder.bind(TopicCache.class).to(new TopicCache(2 * 60 * 1000));
        binder.bind(TagCache.class).to(new TagCache(5 * 60 * 1000));
    }

    // Typed cache wrappers
    public static class UserCache extends SimpleCache<Long, User> {

        public UserCache(long ttlMs) {
            super(ttlMs);
        }
    }

    public static class SysConfigCache extends SimpleCache<String, SysConfig> {

        public SysConfigCache(long ttlMs) {
            super(ttlMs);
        }
    }

    public static class TopicCache extends SimpleCache<Long, Topic> {

        public TopicCache(long ttlMs) {
            super(ttlMs);
        }
    }

    public static class TagCache extends SimpleCache<Long, Tag> {

        public TagCache(long ttlMs) {
            super(ttlMs);
        }
    }
}
