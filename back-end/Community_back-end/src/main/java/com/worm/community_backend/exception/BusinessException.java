package com.worm.community_backend.exception;
import com.worm.community_backend.common.ResultCode;
import lombok.Getter;
/**
 * 涓氬姟寮傚父锛氱敤浜庤繑鍥炲彲棰勬湡鐨勪笟鍔￠敊璇爜鍜岄敊璇俊鎭€?
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
