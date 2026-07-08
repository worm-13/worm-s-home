package com.worm.community_backend.exception;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * 全局异常处理：统一转为 ApiResponse 返回。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 处理业务异常。 */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return ApiResponse.failure(ex.getCode(), ex.getMessage());
    }

    /** 处理请求体格式错误。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleBadRequest(HttpMessageNotReadableException ex) {
        log.warn("Request body parse failed", ex);
        return ApiResponse.failure(ResultCode.BAD_REQUEST);
    }

    /** 处理参数校验异常 (Validation) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        log.warn("Validation failed: {}", message);
        return ApiResponse.failure(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /** 根据请求路径区分头像/背景图超限错误。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                         HttpServletRequest request) {
        log.warn("Upload file too large", ex);
        String requestUri = request != null ? request.getRequestURI() : "";
        if (requestUri != null && requestUri.contains("/background-image")) {
            return ApiResponse.failure(ResultCode.BACKGROUND_SIZE_EXCEEDED);
        }
        return ApiResponse.failure(ResultCode.AVATAR_SIZE_EXCEEDED);
    }

    /** 处理 multipart 请求解析失败。 */
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipartException(MultipartException ex) {
        log.warn("Multipart request failed", ex);
        return ApiResponse.failure(ResultCode.BAD_REQUEST);
    }

    /** 兜底处理未知异常。 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unhandled server exception", ex);
        return ApiResponse.failure(ResultCode.INTERNAL_ERROR);
    }
}
