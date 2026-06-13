package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_attachment")
public class Attachment {
    @Id
    private String id;           // UUID string
    @Column(nullable = false) Long topicId;
    @Column(nullable = false) Long userId;
    @Column(length = 256) String fileName;
    @Column(type = "TEXT") String fileUrl;
    @Column Long fileSize;
    @Column(length = 32) String fileType;
    @Column int downloadScore;
    @Column(nullable = false) long downloadCount;
    @Column(nullable = false) long createTime;

    public Attachment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public int getDownloadScore() { return downloadScore; }
    public void setDownloadScore(int downloadScore) { this.downloadScore = downloadScore; }
    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long downloadCount) { this.downloadCount = downloadCount; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
