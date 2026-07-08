package com.worm.community_backend.post.schedule;

import com.worm.community_backend.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每分钟检查一次，自动发布到期的定时文章。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final PostService postService;

    @Scheduled(fixedRate = 60000)
    public void publishScheduledPosts() {
        try {
            postService.publishScheduledPosts();
        } catch (Exception e) {
            log.error("定时发布任务执行异常", e);
        }
    }
}
