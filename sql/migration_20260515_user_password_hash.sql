-- Web 认证迭代（API §15）：正式账号使用 password_hash，与 PRD §7.3 对齐
ALTER TABLE `user`
    ADD COLUMN `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'bcrypt 密码哈希' AFTER `phone`;
