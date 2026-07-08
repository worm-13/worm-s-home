package com.worm.community_backend.common;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 妤嫏杩旀洖纰肩董缇?
 */
@Getter
@AllArgsConstructor
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
    AVATAR_FILE_REQUIRED(1008, "avatar file is required"),
    AVATAR_TYPE_INVALID(1009, "avatar must be jpg, jpeg, png, gif or webp"),
    AVATAR_SIZE_EXCEEDED(1010, "avatar file size exceeds limit"),
    AVATAR_UPLOAD_FAILED(1011, "failed to upload avatar"),
    BACKGROUND_FILE_REQUIRED(1012, "background image file is required"),
    BACKGROUND_TYPE_INVALID(1013, "background image must be jpg, jpeg, png, gif or webp"),
    BACKGROUND_SIZE_EXCEEDED(1014, "background image file size exceeds limit"),
    BACKGROUND_UPLOAD_FAILED(1015, "failed to upload background image"),
    POST_TITLE_REQUIRED(1016, "post title is required"),
    POST_CONTENT_REQUIRED(1017, "post content is required"),
    POST_CREATE_FAILED(1018, "failed to create post"),
    POST_COVER_FILE_REQUIRED(1019, "post cover file is required"),
    POST_COVER_TYPE_INVALID(1020, "post cover must be jpg, jpeg, png, gif or webp"),
    POST_COVER_SIZE_EXCEEDED(1021, "post cover file size exceeds limit"),
    POST_COVER_UPLOAD_FAILED(1022, "failed to upload post cover"),
    ALREADY_FOLLOWED(1023, "already followed"),
    NOT_FOLLOWED(1024, "not followed"),
    CANNOT_FOLLOW_SELF(1025, "cannot follow yourself"),
    COMMENT_CONTENT_REQUIRED(1026, "comment content is required"),
    COMMENT_NOT_FOUND(1027, "comment not found"),
    COMMENT_DELETE_FAILED(1028, "failed to delete comment"),
    ALREADY_LIKED(1029, "already liked"),
    NOT_LIKED(1030, "not liked"),
    ALREADY_FAVORITED(1031, "already favorited"),
    NOT_FAVORITED(1032, "not favorited"),
    OLD_PASSWORD_INCORRECT(1033, "old password is incorrect"),
    POST_SCHEDULE_TIME_REQUIRED(1034, "scheduled publish time is required for scheduled posts"),
    POST_SCHEDULE_TIME_PAST(1035, "scheduled publish time must be in the future"),
    POST_PUBLISH_FAILED(1036, "failed to publish post"),
    NOTIFICATION_NOT_FOUND(1037, "notification not found"),
    NOT_MUTUAL_FOLLOW(1038, "both users must follow each other to send messages"),
    MESSAGE_RECEIVER_NOT_FOUND(1039, "message receiver not found"),
    MESSAGE_CONTENT_REQUIRED(1040, "message content is required"),
    MESSAGE_SEND_FAILED(1041, "failed to send message"),
    REFRESH_TOKEN_REQUIRED(1042, "refresh token is required"),
    REFRESH_TOKEN_INVALID(1043, "refresh token is invalid or expired"),
    REFRESH_TOKEN_EXPIRED(1044, "refresh token has expired"),
    RATE_LIMIT_EXCEEDED(1045, "too many requests, please try again later"),
    ALREADY_REPORTED(1046, "already reported this content"),
    TITLE_TOO_LONG(1047, "title exceeds maximum length"),
    CONTENT_TOO_LONG(1048, "content exceeds maximum length"),
    SENSITIVE_WORD_DETECTED(1049, "content contains sensitive words"),
    COMMENT_TOO_LONG(1050, "comment exceeds maximum length"),
    COMMENT_SENSITIVE_WORD(1051, "comment contains sensitive words"),
    INTERNAL_ERROR(500, "internal server error");

    private final int code;
    private final String message;
}
