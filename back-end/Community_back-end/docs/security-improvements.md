# 安全性改进文档

## 概述

本文档记录了社区网站后端的安全性改进措施。

## 1. JWT 密钥管理

### 改进内容
- JWT 密钥通过环境变量 `JWT_SECRET` 配置
- 生产环境必须设置强密钥（至少32字节）
- 支持 Base64 编码的密钥

### 配置方式
```properties
# 环境变量配置
JWT_SECRET=your-secure-secret-key-at-least-32-bytes
JWT_EXPIRE_SECONDS=604800
JWT_REFRESH_EXPIRE_SECONDS=2592000
```

## 2. 刷新令牌机制

### 改进内容
- 实现了完整的刷新令牌机制
- 支持访问令牌和刷新令牌分离
- 刷新令牌存储在数据库中
- 支持单设备登录策略（新登录会撤销旧令牌）
- 自动清理过期的刷新令牌

### API 端点
- `POST /api/auth/refresh-token` - 使用刷新令牌获取新的访问令牌
- `POST /api/auth/logout` - 用户登出，撤销所有刷新令牌

### 数据库表
```sql
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## 3. 速率限制

### 改进内容
- 实现了基于 IP 的速率限制
- 登录接口：每个 IP 每5分钟最多5次尝试
- 注册接口：每个 IP 每小时最多3次尝试
- 可通过环境变量配置限制参数

### 配置方式
```properties
# 速率限制配置
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN_MAX=5
RATE_LIMIT_LOGIN_WINDOW=300
RATE_LIMIT_REGISTER_MAX=3
RATE_LIMIT_REGISTER_WINDOW=3600
```

### 响应示例
当速率限制被触发时，返回 HTTP 429 状态码：
```json
{
  "code": 1045,
  "message": "too many requests, please try again later",
  "data": null
}
```

## 4. CSRF 保护

### 说明
由于本项目使用无状态的 JWT 认证，不需要传统的 CSRF 保护。JWT 令牌通过 Authorization 头传递，不会被浏览器自动包含在跨域请求中。

## 5. 其他安全改进

### 密码安全
- 使用 BCrypt 加密算法
- 密码最少6位

### 输入验证
- 所有用户输入都进行验证
- 防止 SQL 注入（使用参数化查询）
- 防止路径穿越攻击

### 错误处理
- 统一的错误响应格式
- 不泄露敏感信息

## 部署建议

1. **生产环境必须配置强 JWT 密钥**
2. **启用 HTTPS**
3. **配置适当的 CORS 策略**
4. **定期清理过期的刷新令牌**
5. **监控速率限制日志**

## 环境变量列表

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_SECRET` | JWT 签名密钥 | `replace-this-with-at-least-32-byte-secret-key` |
| `JWT_EXPIRE_SECONDS` | 访问令牌过期时间（秒） | `604800` (7天) |
| `JWT_REFRESH_EXPIRE_SECONDS` | 刷新令牌过期时间（秒） | `2592000` (30天) |
| `RATE_LIMIT_ENABLED` | 是否启用速率限制 | `true` |
| `RATE_LIMIT_LOGIN_MAX` | 登录最大尝试次数 | `5` |
| `RATE_LIMIT_LOGIN_WINDOW` | 登录限制时间窗口（秒） | `300` |
| `RATE_LIMIT_REGISTER_MAX` | 注册最大尝试次数 | `3` |
| `RATE_LIMIT_REGISTER_WINDOW` | 注册限制时间窗口（秒） | `3600` |
