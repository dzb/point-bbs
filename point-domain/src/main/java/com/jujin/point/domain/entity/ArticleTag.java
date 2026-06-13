package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_article_tag")
public record ArticleTag(
    @Id @Generated Long id,
    @Column(nullable = false) Long articleId,
    @Column(nullable = false) Long tagId,
    @Column(nullable = false) long status,
    @Column(nullable = false) long createTime
) {
    public ArticleTag(Long articleId, Long tagId, long status, long createTime) {
        this(null, articleId, tagId, status, createTime);
    }
}
