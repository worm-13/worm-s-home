-- 添加用户角色字段
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 将第一个注册用户设为管理员（使用临时表避免MySQL限制）
UPDATE users SET role = 'ADMIN' WHERE id = (SELECT min_id FROM (SELECT MIN(id) AS min_id FROM users) AS tmp);
