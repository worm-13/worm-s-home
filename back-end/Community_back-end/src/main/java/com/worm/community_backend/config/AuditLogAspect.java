package com.worm.community_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计日志切面：拦截关键业务操作，记录 who/what/when/result。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("audit-log");

    // ---- 用户操作 ----

    @Around("execution(* com.worm.community_backend.user.controller.UserController.login(..))")
    public Object auditLogin(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "LOGIN", null);
    }

    @Around("execution(* com.worm.community_backend.user.controller.UserController.register(..))")
    public Object auditRegister(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "REGISTER", null);
    }

    // ---- 文章操作 ----

    @Around("execution(* com.worm.community_backend.post.controller.PostController.createPost(..))")
    public Object auditCreatePost(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "CREATE_POST", extractIdFromArgs(pjp));
    }

    @Around("execution(* com.worm.community_backend.post.controller.PostController.saveDraft(..))")
    public Object auditSaveDraft(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "SAVE_DRAFT", extractIdFromArgs(pjp));
    }

    @Around("execution(* com.worm.community_backend.post.controller.PostController.updatePost(..))")
    public Object auditUpdatePost(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "UPDATE_POST", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.post.controller.PostController.deletePost(..))")
    public Object auditDeletePost(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "DELETE_POST", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.post.controller.PostController.publishPost(..))")
    public Object auditPublishPost(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "PUBLISH_POST", extractIdFromPath(pjp));
    }

    // ---- 评论操作 ----

    @Around("execution(* com.worm.community_backend.comment.controller.CommentController.create(..))")
    public Object auditCreateComment(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "CREATE_COMMENT", extractIdFromArgs(pjp));
    }

    @Around("execution(* com.worm.community_backend.comment.controller.CommentController.delete(..))")
    public Object auditDeleteComment(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "DELETE_COMMENT", extractIdFromPath(pjp));
    }

    // ---- 互动操作 ----

    @Around("execution(* com.worm.community_backend.interaction.controller.InteractionController.like(..))")
    public Object auditLike(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "LIKE_POST", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.interaction.controller.InteractionController.unlike(..))")
    public Object auditUnlike(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "UNLIKE_POST", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.interaction.controller.InteractionController.favorite(..))")
    public Object auditFavorite(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "FAVORITE_POST", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.interaction.controller.InteractionController.unfavorite(..))")
    public Object auditUnfavorite(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "UNFAVORITE_POST", extractIdFromPath(pjp));
    }

    // ---- 关注操作 ----

    @Around("execution(* com.worm.community_backend.user.controller.FollowController.follow(..))")
    public Object auditFollow(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "FOLLOW_USER", extractIdFromPath(pjp));
    }

    @Around("execution(* com.worm.community_backend.user.controller.FollowController.unfollow(..))")
    public Object auditUnfollow(ProceedingJoinPoint pjp) throws Throwable {
        return audit(pjp, "UNFOLLOW_USER", extractIdFromPath(pjp));
    }

    // ---- 核心审计方法 ----

    private Object audit(ProceedingJoinPoint pjp, String action, String targetId) throws Throwable {
        String userId = resolveUserId();
        String ip = resolveClientIp();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            if (targetId != null) {
                auditLog.info("action={} userId={} targetId={} ip={} result=SUCCESS cost={}ms",
                        action, userId, targetId, ip, cost);
            } else {
                auditLog.info("action={} userId={} ip={} result=SUCCESS cost={}ms",
                        action, userId, ip, cost);
            }
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            if (targetId != null) {
                auditLog.warn("action={} userId={} targetId={} ip={} result=FAILURE error={} cost={}ms",
                        action, userId, targetId, ip, e.getMessage(), cost);
            } else {
                auditLog.warn("action={} userId={} ip={} result=FAILURE error={} cost={}ms",
                        action, userId, ip, e.getMessage(), cost);
            }
            throw e;
        }
    }

    // ---- 辅助方法 ----

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "-";
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "-";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 从方法参数中提取第一个 Long 类型参数作为 targetId。
     * 适用于 @PathVariable Long id 的场景。
     */
    private String extractIdFromPath(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long id) {
                return String.valueOf(id);
            }
        }
        return null;
    }

    /**
     * 从方法参数中提取第一个 Long 类型参数作为 targetId（排除 Authentication）。
     */
    private String extractIdFromArgs(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long id) {
                return String.valueOf(id);
            }
        }
        return null;
    }
}
