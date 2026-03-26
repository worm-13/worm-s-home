package com.worm.community_backend.common;

public enum ResultCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    NOT_FOUND(404, "resource not found"),
    USERNAME_REQUIRED(1001, "username is required"),
    PASSWORD_INVALID(1002, "password must be at least 6 characters"),
    EMAIL_INVALID(1003, "email format is invalid"),
    USERNAME_EXISTS(1004, "username already exists"),
    EMAIL_EXISTS(1005, "email already exists"),
    INVALID_CREDENTIALS(1006, "username/id/email or password is incorrect"),
    USER_ID_GENERATE_FAILED(1007, "failed to generate user id"),
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

