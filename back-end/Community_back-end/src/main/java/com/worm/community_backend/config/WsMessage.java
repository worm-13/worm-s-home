package com.worm.community_backend.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WsMessage {
    private String wsType;  // "NOTIFICATION" | "MESSAGE"
    private Object data;
}
