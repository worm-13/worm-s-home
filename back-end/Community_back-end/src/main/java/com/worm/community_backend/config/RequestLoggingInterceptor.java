package com.worm.community_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求性能日志拦截器：记录每个 API 请求的方法、URI、用户、状态码和耗时。
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger accessLog = LoggerFactory.getLogger("access-log");
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long cost = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        int status = response.getStatus();
        String userId = resolveUserId();

        if (query != null) {
            uri = uri + "?" + query;
        }

        accessLog.info("method={} uri={} userId={} status={} cost={}ms",
                method, uri, userId, status, cost);
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "-";
    }
}
