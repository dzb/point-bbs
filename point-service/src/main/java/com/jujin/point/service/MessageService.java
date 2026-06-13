package com.jujin.point.service;

import com.jujin.point.domain.entity.Message;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;

import java.util.List;

public class MessageService {
    private final Database db;
    private final Orm orm;

    public MessageService(Database db) {
        this.db = db;
        this.orm = Orm.of(db);
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
        return msg;
    }

    public List<Message> getUserMessages(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_message WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(Message.class);
    }

    public long unreadCount(long userId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_message WHERE user_id = ? AND status = 0", userId);
    }

    public void markRead(long userId, List<Long> messageIds) {
        if (messageIds.isEmpty()) return;
        var inClause = messageIds.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("0");
        db.execute("UPDATE bbs_message SET status = 1 WHERE user_id = ? AND id IN (" + inClause + ")", userId);
    }

    public void markAllRead(long userId) {
        db.execute("UPDATE bbs_message SET status = 1 WHERE user_id = ?", userId);
    }
}
