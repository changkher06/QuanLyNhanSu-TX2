package api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> api(ApiException ex, HttpServletRequest request) {
        return response(ex.status.value(), ex.code, ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> business(IllegalArgumentException ex, HttpServletRequest request) {
        return response(422, "BUSINESS_RULE", ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, HttpServletRequest request) {
        LOG.error("Unexpected API error for {}", request.getRequestURI(), ex);
        return response(500, "INTERNAL_ERROR", "Lỗi máy chủ; xem log để chẩn đoán.", request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "Request không hợp lệ: kiểm tra JSON, kiểu dữ liệu, UUID, ngày yyyy-MM-dd và tham số bắt buộc.";
            case 404 -> "Không tìm thấy endpoint hoặc tài nguyên.";
            case 405 -> "HTTP method không được hỗ trợ cho endpoint này.";
            case 406 -> "Response chỉ hỗ trợ application/json.";
            case 415 -> "Content-Type phải là application/json.";
            default -> "Không thể xử lý HTTP request.";
        };
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return new ResponseEntity<>(new ApiDtos.ErrorResponse(status.value(), "HTTP_" + status.value(),
                message, path), headers, status);
    }
    private ResponseEntity<Object> response(int status, String code, String message, String path) {
        return ResponseEntity.status(status).body(new ApiDtos.ErrorResponse(status, code, message, path));
    }
}
