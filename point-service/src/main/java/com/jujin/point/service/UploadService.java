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
    private final long maxSizeBytes;

    public UploadService(
        Database db,
        Orm orm,
        @Value("${bbs.upload.dir:./uploads}") String uploadDirPath,
        @Value("${bbs.upload.max-size:10485760}") long maxSizeBytes
    ) {
        this.db = db;
        this.orm = orm;
        this.maxSizeBytes = maxSizeBytes > 0 ? maxSizeBytes : 10 * 1024 * 1024;
        this.uploadDir = Path.of(uploadDirPath).toAbsolutePath().normalize();
        try { Files.createDirectories(this.uploadDir); } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    /** Enforce the configured upload size cap. */
    public void checkSize(long fileSize) {
        if (fileSize > maxSizeBytes) {
            throw new ServiceException(
                "文件过大，最大支持 " + (maxSizeBytes / 1024 / 1024) + "MB"
            );
        }
    }

    /**
     * Upload a file and create an attachment record.
     * Returns the Attachment with download URL populated.
     */
    public Attachment upload(long userId, long topicId, String fileName, InputStream data,
                             long fileSize, String fileType) throws IOException {
        // The BBS only displays images inline; serving anything else from the
        // same origin is a stored-XSS vector (HTML/SVG). SVG is rejected too:
        // it is image/* yet carries scripts that run when opened directly.
        if (!isImage(fileType) || "image/svg+xml".equalsIgnoreCase(fileType)) {
            throw new ServiceException("仅支持图片文件（png/jpeg/gif/webp）");
        }
        // Create attachment record (PK = stored filename)
        var att = new Attachment();
        var now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        att.setId(id);

        // Save to disk with same ID. The extension is sanitized: only a safe
        // suffix from the original name is kept, and the name stored for
        // Content-Disposition is stripped of quotes/CR/LF and path separators.
        String safeName = sanitizeFileName(fileName);
        String ext = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : "";
        String storedName = id + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        att.setUserId(userId);
        att.setTopicId(topicId);
        att.setFileName(safeName);
        att.setFileUrl("/api/attachment/download/" + att.getId());
        att.setFileSize(fileSize);
        att.setFileType(fileType);
        att.setDownloadCount(0);
        att.setDownloadScore(0);
        att.setCreateTime(now);

        orm.insert(att);
        return att;
    }

    /** Strip path separators, quotes, CR/LF and control chars from a client-supplied file name. */
    private static String sanitizeFileName(String name) {
        if (name == null) return "file";
        String cleaned = name
            .replaceAll("[/\\\\]", "_")
            .replaceAll("[\"'\r\n\t]", "")
            .replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.isBlank()) return "file";
        return cleaned.length() > 128 ? cleaned.substring(0, 128) : cleaned;
    }

    /**
     * Get attachment by ID.
     */
    public Attachment getAttachment(String attachmentId) {
        return orm.findById(Attachment.class, attachmentId).orElse(null);
    }

    /**
     * Get the file path for an attachment.
     *
     * <p>Files are stored as {@code <id><ext>} where the extension comes from
     * the (sanitized) original name, so the path resolves directly without
     * scanning the upload directory. A directory scan remains as a fallback
     * for attachments stored before name sanitization.
     */
    public Path getFilePath(Attachment att) {
        String id = att.getId();
        String name = att.getFileName();
        if (name != null && name.contains(".")) {
            var direct = uploadDir.resolve(id + name.substring(name.lastIndexOf('.')));
            if (Files.exists(direct)) return direct;
        }
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
