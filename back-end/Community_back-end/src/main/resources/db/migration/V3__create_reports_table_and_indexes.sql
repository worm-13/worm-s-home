-- 创建举报表
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id INT NOT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'POST, COMMENT, USER',
    target_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL COMMENT 'SPAM, HARASSMENT, INAPPROPRIATE, OTHER',
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, REVIEWED, DISMISSED, RESOLVED',
    reviewed_by INT,
    review_note VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加数据库索引优化查询性能（使用存储过程避免重复创建）
DELIMITER //
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    -- posts 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'posts' AND index_name = 'idx_posts_status_created') THEN
        CREATE INDEX idx_posts_status_created ON posts(status, created_at DESC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'posts' AND index_name = 'idx_posts_user_status') THEN
        CREATE INDEX idx_posts_user_status ON posts(user_id, status, updated_at DESC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'posts' AND index_name = 'idx_posts_scheduled') THEN
        CREATE INDEX idx_posts_scheduled ON posts(status, scheduled_at);
    END IF;
    -- comments 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'comments' AND index_name = 'idx_comments_post_status') THEN
        CREATE INDEX idx_comments_post_status ON comments(post_id, status, created_at);
    END IF;
    -- notifications 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'notifications' AND index_name = 'idx_notifications_receiver') THEN
        CREATE INDEX idx_notifications_receiver ON notifications(receiver_id, created_at DESC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'notifications' AND index_name = 'idx_notifications_unread') THEN
        CREATE INDEX idx_notifications_unread ON notifications(receiver_id, is_read);
    END IF;
    -- messages 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'messages' AND index_name = 'idx_messages_sender_receiver') THEN
        CREATE INDEX idx_messages_sender_receiver ON messages(sender_id, receiver_id, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'messages' AND index_name = 'idx_messages_unread') THEN
        CREATE INDEX idx_messages_unread ON messages(receiver_id, is_read);
    END IF;
    -- post_likes 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'post_likes' AND index_name = 'idx_post_likes_unique') THEN
        CREATE INDEX idx_post_likes_unique ON post_likes(post_id, user_id);
    END IF;
    -- post_favorites 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'post_favorites' AND index_name = 'idx_post_favorites_unique') THEN
        CREATE INDEX idx_post_favorites_unique ON post_favorites(post_id, user_id);
    END IF;
    -- user_follows 表索引
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_name = 'user_follows' AND index_name = 'idx_user_follows_unique') THEN
        CREATE INDEX idx_user_follows_unique ON user_follows(follower_id, following_id);
    END IF;
END //
DELIMITER ;

CALL create_index_if_not_exists();
DROP PROCEDURE create_index_if_not_exists;
