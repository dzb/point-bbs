package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable vote record.
 */
@Table("bbs_vote_record")
public record VoteRecord(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) Long voteId,
    @Column(type = "TEXT") String optionIds,
    @Column(nullable = false) long createTime
) {
    public VoteRecord(Long userId, Long voteId, String optionIds, long createTime) {
        this(null, userId, voteId, optionIds, createTime);
    }
}
