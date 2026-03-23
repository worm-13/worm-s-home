package com.worm.community_backend.common;

public enum ResultCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    NOT_FOUND(404, "resource not found"),
    INTERNAL_ERROR(500, "internal server error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

