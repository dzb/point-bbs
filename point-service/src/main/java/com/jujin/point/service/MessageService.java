package com.jujin.point.service;

import com.jujin.point.domain.entity.Message;
import com.jujin.point.domain.event.NotificationSentEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.ioc.EventBus;

import java.util.List;

public class MessageService {
    private final Database db;
    private final Orm orm;
    private final EventBus eventBus;

    public MessageService(Database db, Orm orm, EventBus eventBus) {
        this.db = db;
        this.orm = orm;
        this.eventBus = eventBus;
    }

    public Message send(long fromId, long toUserId, String title, String content,
                        String quoteContent, int type, String extraData) {
        if (fromId == toUserId) return null;
        var msg = new Message();
        var now = System.currentTimeMillis();
        msg.setFromId(fromId);
        msg.setUserId(toUserId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setQuoteContent(quoteContent);
        msg.setType(type);
        msg.setExtraData(extraData);
        msg.setStatus(0);
        msg.setCreateTime(now);
        orm.insert(msg);
        // Single push signal: the WebSocket layer listens for this instead of
        // re-resolving recipients per business event. Buffered until the
        // surrounding transaction commits (EventBus Defer), so a rollback
        // never produces a phantom ping.
        eventBus.publish(new NotificationSentEvent(fromId, toUserId, now));
        return msg;
    }

    public List<Message> getUserMessages(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_message WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(Message.class);
    }

    public long unreadCount(long userId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_message WHERE user_id = ? AND status = 0", userId);
    }

    public long count(long userId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_message WHERE user_id = ?", userId);
    }

    public void markRead(long userId, List<Long> messageIds) {
        if (messageIds.isEmpty()) return;
        var placeholders = messageIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        var params = new Object[messageIds.size() + 1];
        params[0] = userId;
        for (int i = 0; i < messageIds.size(); i++) params[i + 1] = messageIds.get(i);
        db.execute("UPDATE bbs_message SET status = 1 WHERE user_id = ? AND id IN (" + placeholders + ")", params);
    }

    public void markAllRead(long userId) {
        db.execute("UPDATE bbs_message SET status = 1 WHERE user_id = ?", userId);
    }
}
