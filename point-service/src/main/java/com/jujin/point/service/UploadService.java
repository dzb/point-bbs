package com.jujin.point.service;

import com.jujin.point.domain.entity.Attachment;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;

import com.jujin.freeway.ioc.annotation.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * File upload service — local filesystem storage.
 */
public class UploadService {
    private final Database db;
    private final Orm orm;
    private final Path uploadDir;

    public UploadService(Database db, @Value("${bbs.upload.dir:./uploads}") String uploadDirPath) {
        this.db = db;
        this.orm = Orm.of(db);
        this.uploadDir = Path.of(uploadDirPath).toAbsolutePath().normalize();
        try { Files.createDirectories(this.uploadDir); } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Upload a file and create an attachment record.
     * Returns the Attachment with download URL populated.
     */
    public Attachment upload(long userId, long topicId, String fileName, InputStream data,
                             long fileSize, String fileType) throws IOException {
        // Create attachment record (PK = stored filename)
        var att = new Attachment();
        var now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        att.setId(id);

        // Save to disk with same ID
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        String storedName = id + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        att.setUserId(userId);
        att.setTopicId(topicId);
        att.setFileName(fileName);
        att.setFileUrl("/api/attachment/download/" + att.getId());
        att.setFileSize(fileSize);
        att.setFileType(fileType);
        att.setDownloadCount(0);
        att.setDownloadScore(0);
        att.setCreateTime(now);

        orm.insert(att);
        return att;
    }

    /**
     * Get attachment by ID.
     */
    public Attachment getAttachment(String attachmentId) {
        return orm.findById(Attachment.class, attachmentId).orElse(null);
    }

    /**
     * Get the file path for an attachment.
     */
    public Path getFilePath(Attachment att) {
        String id = att.getId();
        try (var stream = Files.list(uploadDir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith(id))
                .findFirst().orElse(uploadDir.resolve(id));
        } catch (IOException e) {
            return uploadDir.resolve(id);
        }
    }

    /**
     * Increment download count.
     */
    public void recordDownload(String attachmentId) {
        db.execute("UPDATE bbs_attachment SET download_count = download_count + 1 WHERE id = ?", attachmentId);
    }

    /** Allowed image types for direct display */
    public static boolean isImage(String contentType) {
        return contentType != null && (contentType.startsWith("image/"));
    }
}
