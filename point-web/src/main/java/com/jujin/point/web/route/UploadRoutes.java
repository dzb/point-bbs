package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.UploadService;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.*;

public class UploadRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api",
            Route.post("/upload", ctx -> {
                var user = AuthFilter.requireUser();
                long topicId = 0;
                try { topicId = Long.parseLong(ctx.queryParam("topicId")); } catch (Exception ignored) {}

                // Base64 image upload (mobile-friendly)
                String body = ctx.bodyText();
                if (body != null && body.contains(",")) {
                    String[] parts = body.split(",", 2);
                    String mime = parts[0].replace("data:", "").replace(";base64", "");
                    byte[] bytes = Base64.getDecoder().decode(parts[1]);
                    String fn = "upload_" + System.currentTimeMillis() + "." + ext(mime);
                    var att = svc().upload(user.userId(), topicId, fn, new ByteArrayInputStream(bytes), bytes.length, mime);
                    ctx.sendJson(201, ApiResponse.ok(Map.of("id", att.getId(), "url", att.getFileUrl(), "fileName", att.getFileName(), "size", bytes.length)));
                    return;
                }

                if (ctx.isMultipart()) {
                    var file = ctx.multipart().file("file");
                    if (file.isPresent()) {
                        var f = file.get();
                        var att = svc().upload(user.userId(), topicId, f.filename(), f.openStream(), f.size(), f.contentType());
                        ctx.sendJson(201, ApiResponse.ok(Map.of("id", att.getId(), "url", att.getFileUrl(), "fileName", att.getFileName(), "size", f.size())));
                        return;
                    }
                }
                ctx.sendJson(400, ApiResponse.error("请上传文件"));
            }),
            Route.get("/attachment/download/{id}", ctx -> {
                String id = ctx.pathVar("id");
                var att = svc().getAttachment(id);
                if (att == null || !Files.exists(svc().getFilePath(att))) {
                    ctx.sendJson(404, ApiResponse.error("文件不存在")); return;
                }
                svc().recordDownload(id);
                ctx.headerSet("Content-Type", att.getFileType() != null ? att.getFileType() : "application/octet-stream");
                ctx.headerSet("Content-Disposition", "inline; filename=\"" + att.getFileName() + "\"");
                ctx.output(Files.readAllBytes(svc().getFilePath(att)));
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
