# 中优先级功能实现报告

## 概述

本文档记录了社区网站中优先级功能的实现情况，包括内容管理、测试覆盖和性能优化三个方面。

---

## 一、内容管理

### 1.1 举报系统

**新增功能：**
- 完整的举报系统，支持举报文章、评论和用户
- 举报状态管理（PENDING → REVIEWED/DISMISSED/RESOLVED）
- 防止重复举报

**新增文件：**
- `moderation/entity/Report.java` - 举报实体
- `moderation/mapper/ReportMapper.java` - 举报数据访问层
- `moderation/dto/ReportCreateDTO.java` - 举报请求DTO
- `moderation/vo/ReportVO.java` - 举报响应VO
- `moderation/service/ReportService.java` - 举报服务接口
- `moderation/service/impl/ReportServiceImpl.java` - 举报服务实现
- `moderation/controller/ReportController.java` - 举报控制器

**API端点：**
- `POST /api/reports` - 创建举报
- `GET /api/reports` - 获取举报列表（支持状态过滤和分页）
- `PUT /api/reports/{id}/review` - 审核举报

### 1.2 敏感词过滤

**新增功能：**
- 敏感词检测和过滤服务
- 支持大小写不敏感匹配
- 可动态添加/移除敏感词

**新增文件：**
- `moderation/service/SensitiveWordFilter.java` - 敏感词过滤服务

**集成位置：**
- `PostServiceImpl.createPost()` - 文章发布时检测
- `CommentServiceImpl.createComment()` - 评论发布时检测

### 1.3 内容长度限制

**服务端验证：**
- 文章标题：最大120字符
- 文章内容：最大50000字符
- 评论内容：最大500字符

**错误码：**
- `1047` - 标题超出最大长度
- `1048` - 内容超出最大长度
- `1049` - 文章包含敏感词
- `1050` - 评论超出最大长度
- `1051` - 评论包含敏感词

### 1.4 数据库表

**新增表：**
```sql
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_note VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    -- 索引和外键...
);
```

---

## 二、测试覆盖

### 2.1 新增单元测试

| 测试文件 | 测试类 | 测试方法数 |
|---------|--------|-----------|
| `UserServiceImplTest.java` | UserServiceImpl | 10 |
| `CommentServiceImplTest.java` | CommentServiceImpl | 8 |
| `ReportServiceImplTest.java` | ReportServiceImpl | 5 |
| `SensitiveWordFilterTest.java` | SensitiveWordFilter | 10 |
| `RateLimiterTest.java` | RateLimiter | 9 |

**总计：42个新增测试方法**

### 2.2 测试覆盖范围

**UserService测试：**
- 注册成功/用户名已存在
- 登录成功/密码错误
- 获取用户信息/用户不存在
- 修改密码成功/旧密码错误
- 刷新令牌成功
- 用户登出

**CommentService测试：**
- 评论成功/内容为空/内容过长/包含敏感词
- 文章不存在
- 删除评论成功/非评论作者
- 分页查询评论

**ReportService测试：**
- 创建举报成功/重复举报
- 获取举报列表（带状态过滤）
- 审核举报成功/举报不存在

**SensitiveWordFilter测试：**
- 敏感词检测（包含/不包含/大小写/空值）
- 敏感词过滤（替换/无敏感词/空值）
- 添加/移除敏感词

**RateLimiter测试：**
- 登录/注册速率限制
- 限制重置
- 禁用速率限制
- 不同IP独立限制

---

## 三、性能优化

### 3.1 Redis缓存配置

**新增文件：**
- `config/CacheConfig.java` - Redis缓存配置

**缓存策略：**
- `userProfiles` - 用户资料缓存，TTL 10分钟
- `postDetails` - 文章详情缓存，TTL 2分钟
- `notificationCounts` - 通知计数缓存，TTL 1分钟

**缓存注解：**
- `@Cacheable` - getUserById(), getPostDetail()
- `@CacheEvict` - updateProfile(), updatePost()

### 3.2 分页支持

**新增分页的API：**

| API | 原实现 | 新实现 |
|-----|--------|--------|
| `GET /api/users/{id}/following` | 返回全部 | 支持page/size参数 |
| `GET /api/users/{id}/followers` | 返回全部 | 支持page/size参数 |
| `GET /api/notifications` | 硬编码LIMIT 50 | 支持page/size参数 |

**新增Mapper方法：**
- `UserFollowMapper.countFollowing()` / `countFollowers()`
- `UserFollowMapper.selectFollowingPage()` / `selectFollowersPage()`
- `NotificationMapper.selectByReceiverIdPage()` / `countByReceiverId()`
- `NotificationMapper.selectById()` - 修复N+1查询问题

### 3.3 数据库索引

**新增索引（V3迁移脚本）：**
```sql
-- 文章表
idx_posts_status_created ON posts(status, created_at DESC)
idx_posts_user_status ON posts(user_id, status, updated_at DESC)
idx_posts_scheduled ON posts(status, scheduled_at)

-- 评论表
idx_comments_post_status ON comments(post_id, status, created_at)

-- 通知表
idx_notifications_receiver ON notifications(receiver_id, created_at DESC)
idx_notifications_unread ON notifications(receiver_id, is_read)

-- 消息表
idx_messages_sender_receiver ON messages(sender_id, receiver_id, created_at)
idx_messages_unread ON messages(receiver_id, is_read)

-- 点赞/收藏表
idx_post_likes_unique ON post_likes(post_id, user_id)
idx_post_favorites_unique ON post_favorites(post_id, user_id)

-- 关注表
idx_user_follows_unique ON user_follows(follower_id, following_id)
```

### 3.4 静态资源缓存

**更新文件：**
- `config/StaticResourceConfig.java`

**缓存策略：**
- 头像、背景图、文章封面：7天缓存
- 使用`Cache-Control: public, max-age=604800, must-revalidate`

### 3.5 Redis配置

**application.properties新增：**
```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

---

## 四、待办事项

### 4.1 内容管理
- [ ] 管理员角色和权限控制（RBAC）
- [ ] 管理员后台界面
- [ ] 用户封禁/解封功能
- [ ] 举报处理队列UI

### 4.2 测试覆盖
- [ ] InteractionService单元测试
- [ ] MessageService单元测试
- [ ] NotificationService单元测试
- [ ] 集成测试
- [ ] 前端测试框架配置（Vitest）

### 4.3 性能优化
- [ ] 图片上传时压缩和缩放
- [ ] WebP格式转换
- [ ] 前端图片懒加载
- [ ] 前端组件懒加载
- [ ] Vite构建分块配置

---

## 五、部署说明

### 5.1 数据库迁移

执行以下SQL脚本：
1. `V2__create_refresh_tokens_table.sql` - 刷新令牌表
2. `V3__create_reports_table_and_indexes.sql` - 举报表和索引

### 5.2 Redis配置

确保Redis服务已启动，并配置以下环境变量：
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### 5.3 验证步骤

1. 编译项目：`mvn clean compile`
2. 运行测试：`mvn test`
3. 启动应用：`mvn spring-boot:run`
4. 测试举报API：`POST /api/reports`
5. 验证缓存：查看Redis中的缓存数据
6. 验证分页：测试关注列表和通知列表的分页功能
