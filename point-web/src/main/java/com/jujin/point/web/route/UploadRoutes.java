package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.UploadService;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.*;

public class UploadRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api",
            Route.post("/upload", ctx -> {
                var user = AuthFilter.requireUser();
                long topicId = ctx.queryParam("topicId").map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        throw new com.jujin.point.service.ServiceException("topicId 参数无效");
                    }
                }).orElse(0L);

                // Base64 image upload (mobile-friendly)
                String body = ctx.bodyText();
                if (body != null && body.contains(",")) {
                    String[] parts = body.split(",", 2);
                    String mime = parts[0].replace("data:", "").replace(";base64", "");
                    byte[] bytes;
                    try {
                        bytes = Base64.getDecoder().decode(parts[1]);
                    } catch (IllegalArgumentException e) {
                        ctx.sendJson(400, ApiResponse.error("无效的 Base64 数据"));
                        return;
                    }
                    if (bytes.length == 0) {
                        ctx.sendJson(400, ApiResponse.error("文件内容为空"));
                        return;
                    }
                    String fn = "upload_" + System.currentTimeMillis() + "." + ext(mime);
                    var att = svc().upload(user.userId(), topicId, fn, new ByteArrayInputStream(bytes), bytes.length, mime);
                    ctx.sendJson(201, ApiResponse.ok(Map.of("id", att.getId(), "url", att.getFileUrl(), "fileName", att.getFileName(), "size", bytes.length)));
                    return;
                }

                if (ctx.isMultipart()) {
                    var multipart = ctx.multipart().orElse(null);
                    if (multipart != null) {
                        var file = multipart.file("file");
                        if (file.isPresent()) {
                            var f = file.get();
                            svc().checkSize(f.size());
                            var att = svc().upload(user.userId(), topicId, f.filename(), f.openStream(), f.size(), f.contentType());
                            ctx.sendJson(201, ApiResponse.ok(Map.of("id", att.getId(), "url", att.getFileUrl(), "fileName", att.getFileName(), "size", f.size())));
                            return;
                        }
                    }
                }
                ctx.sendJson(400, ApiResponse.error("请上传文件"));
            }),
            Route.get("/attachment/download/{id}", ctx -> {
                String id = ctx.pathVar("id").orElse(null);
                var att = svc().getAttachment(id);
                if (att == null || !Files.exists(svc().getFilePath(att))) {
                    ctx.sendJson(404, ApiResponse.error("文件不存在")); return;
                }
                svc().recordDownload(id);
                // Download audit trail (anonymous downloads recorded as user 0)
                var viewer = AuthFilter.currentUser();
                try {
                    com.jujin.freeway.db.Orm.of(
                        com.jujin.point.domain.AppContext.get(com.jujin.freeway.db.Database.class)
                    ).insert(
                        new com.jujin.point.domain.entity.AttachmentDownloadLog(
                            viewer != null ? viewer.userId() : 0L,
                            id,
                            System.currentTimeMillis()
                        )
                    );
                } catch (Exception e) {
                    // never break the download for a logging failure
                }
                String type = att.getFileType() != null ? att.getFileType() : "application/octet-stream";
                // Serve images inline; force download for everything else so an
                // attacker-uploaded HTML/SVG cannot execute from this origin.
                boolean svg = "image/svg+xml".equalsIgnoreCase(type);
                String disposition = (com.jujin.point.service.UploadService.isImage(type) && !svg)
                    ? "inline"
                    : "attachment";
                ctx.setHeader("Content-Type", type);
                ctx.setHeader("Content-Disposition", disposition + "; filename=\"" + att.getFileName() + "\"");
                ctx.setHeader("X-Content-Type-Options", "nosniff");
                // Stream instead of buffering the whole file in memory
                var path = svc().getFilePath(att);
                try (var in = Files.newInputStream(path)) {
                    ctx.output(in, Files.size(path));
                }
            })
        );
    }

    private static UploadService svc() { return AppContext.get(UploadService.class); }

    private static String ext(String mime) {
        return switch (mime) {
            case "image/png" -> "png"; case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif"; case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
