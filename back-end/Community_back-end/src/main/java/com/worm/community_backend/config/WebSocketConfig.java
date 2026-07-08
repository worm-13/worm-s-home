package com.worm.community_backend.config;

import com.worm.community_backend.notification.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final String[] allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                           JwtTokenProvider jwtTokenProvider,
                           @Value("${app.websocket.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.jwtTokenProvider = jwtTokenProvider;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(new WebSocketAuthInterceptor(jwtTokenProvider))
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
