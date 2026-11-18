package com.jujin.point.web.filter;

import com.jujin.freeway.http.ErrorResponse;
import com.jujin.freeway.http.ValidationException;
import com.jujin.freeway.http.body.BodyTooLargeException;
import com.jujin.freeway.http.body.MultipartException;
import com.jujin.freeway.http.body.UnsupportedMediaTypeException;
import com.jujin.freeway.http.filter.ErrorMapper;
import com.jujin.point.service.ServiceException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps point's application exceptions to freeway 2.0's {@link ErrorResponse}.
 *
 * <p>2.0 moved error output to the server: a mapper returns the response, it
 * no longer writes one. The wire body is therefore the framework's
 * {@code {error, code}} shape; the SPA normalizes {@code error} back to
 * {@code message} in its axios interceptor so callers keep one field to read.
 * Framework request rejections fall through to the built-in mapper, which
 * owns their canonical status codes.
 */
public class PointErrorMapper implements ErrorMapper {

    private static final Logger log = LoggerFactory.getLogger(PointErrorMapper.class);

    @Override
    public Optional<ErrorResponse> map(Exception failure) {
        if (failure instanceof AuthException ae) {
            return Optional.of(ErrorResponse.of(
                ae.statusCode(), tokenFor(ae.statusCode()), ae.getMessage()));
        }
        if (failure instanceof ServiceException se) {
            return Optional.of(ErrorResponse.of(400, "bad_request", se.getMessage()));
        }
        if (failure instanceof ValidationException ve) {
            List<Map<String, String>> errors = ve.result().errors().stream()
                .map(e -> Map.of("field", e.field(), "message", e.message()))
                .toList();
            return Optional.of(ErrorResponse.of(400, "validation_failed", "参数校验失败")
                .withDetails(Map.of("errors", errors)));
        }
        if (failure instanceof BodyTooLargeException
                || failure instanceof UnsupportedMediaTypeException
                || failure instanceof MultipartException) {
            return Optional.empty();
        }
        log.error(
            "Unhandled exception {}: {}",
            failure.getClass().getSimpleName(),
            failure.getMessage(),
            failure
        );
        return Optional.of(ErrorResponse.of(500, "internal_error", "服务器内部错误"));
    }

    private static String tokenFor(int status) {
        return switch (status) {
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 429 -> "too_many_requests";
            default -> status >= 500 ? "internal_error" : "bad_request";
        };
    }
}
