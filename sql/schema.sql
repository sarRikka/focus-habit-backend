-- =====================================================================
-- 自控力与目标管理 APP 业务库 schema
-- MySQL 8.0+，字符集 utf8mb4，所有表使用 InnoDB
-- 命名规范：snake_case，主键统一使用业务前缀字符串 ID（与 API 文档一致）
-- 通用字段：created_at / updated_at / deleted（逻辑删除）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `atomic_focus`
    DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `atomic_focus`;

-- ---------------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`             VARCHAR(32)  NOT NULL COMMENT '用户ID（u_xxx）',
    `phone`          VARCHAR(32)  DEFAULT NULL COMMENT '手机号（含国家码）',
    `password_hash`  VARCHAR(255) DEFAULT NULL COMMENT 'bcrypt 等密码哈希（正式账号）',
    `nickname`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`         VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `is_guest`       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否游客 0否 1是',
    `device_id`      VARCHAR(128) DEFAULT NULL COMMENT '游客绑定的设备ID',
    `joined_at`      DATE         NOT NULL COMMENT '注册日期',
    `total_checkin_days` INT      NOT NULL DEFAULT 0 COMMENT '累计打卡天数',
    `fixed_habits_count` INT      NOT NULL DEFAULT 0 COMMENT '已固化习惯数量',
    `continuous_days`    INT      NOT NULL DEFAULT 0 COMMENT '当前连续打卡天数',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_device` (`device_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ---------------------------------------------------------------------
-- 用户设置表（与 user 1:1）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
    `user_id`                    VARCHAR(32) NOT NULL,
    `reminder_time`              VARCHAR(8)  NOT NULL DEFAULT '19:00' COMMENT '打卡提醒时间 HH:mm',
    `reminder_repeat`            TINYINT     NOT NULL DEFAULT 3 COMMENT '提醒重复次数 1-3',
    `review_reminder_enabled`    TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '复盘提醒开关',
    `review_reminder_time`       VARCHAR(8)  NOT NULL DEFAULT '19:00',
    `push_enabled`               TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '消息推送开关',
    `theme`                      VARCHAR(16) NOT NULL DEFAULT 'light' COMMENT 'light | dark',
    `data_retention`             VARCHAR(16) NOT NULL DEFAULT '1y' COMMENT '1y | 3y | 5y | forever',
    `default_progress_deduction` TINYINT     NOT NULL DEFAULT 1 COMMENT '默认进度扣除百分比 1-5',
    `created_at`                 DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                 DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户设置表';

-- ---------------------------------------------------------------------
-- 自定义鼓励语
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_encouragement`;
CREATE TABLE `user_encouragement` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    VARCHAR(32)  NOT NULL,
    `content`    VARCHAR(255) NOT NULL,
    `sort`       INT          NOT NULL DEFAULT 0,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户自定义鼓励语';

-- ---------------------------------------------------------------------
-- 用户成就 / 标签
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_achievement`;
CREATE TABLE `user_achievement` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    VARCHAR(32)  NOT NULL,
    `key_code`   VARCHAR(64)  NOT NULL COMMENT '标签 key',
    `name`       VARCHAR(64)  NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `earned`     TINYINT(1)   NOT NULL DEFAULT 0,
    `earned_at`  DATE         DEFAULT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_key` (`user_id`, `key_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户身份标签/成就';

-- ---------------------------------------------------------------------
-- 目标表（含每日习惯字段）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goal`;
CREATE TABLE `goal` (
    `id`                  VARCHAR(32)  NOT NULL COMMENT '目标ID（g_xxx）',
    `user_id`             VARCHAR(32)  NOT NULL,
    `name`                VARCHAR(128) NOT NULL,
    `category`            VARCHAR(16)  NOT NULL DEFAULT 'custom' COMMENT 'habit|ability|state|custom',
    `custom_category_name` VARCHAR(64) DEFAULT NULL,
    `final_goal`          VARCHAR(512) DEFAULT NULL COMMENT '最终目标',
    `core_need`           VARCHAR(512) DEFAULT NULL COMMENT '核心诉求（习惯固化判定）',
    `total_description`   VARCHAR(512) DEFAULT NULL,
    `deadline`            DATE         NOT NULL COMMENT '截止日期',
    `color`               VARCHAR(16)  NOT NULL DEFAULT 'brand' COMMENT 'brand|mint|lavender|peach',
    `icon`                VARCHAR(16)  DEFAULT NULL,
    -- 每日习惯（与目标 1:1，内嵌方便查询）
    `dh_description`      VARCHAR(255) DEFAULT NULL COMMENT '每日习惯描述',
    `dh_duration`         INT          NOT NULL DEFAULT 10 COMMENT '每日时长（分钟）',
    `dh_auto_level_up`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否开启自动进阶',
    `dh_level_up_step`    TINYINT      NOT NULL DEFAULT 1 COMMENT '进阶幅度（分钟）',
    -- 进度
    `progress`            INT          NOT NULL DEFAULT 0 COMMENT '当前进度 0-100',
    `manual_deduction`    INT          NOT NULL DEFAULT 0 COMMENT '累计手动扣除百分比',
    `archived`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否归档/结束',
    `fixed`               TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已习惯固化',
    `fixed_at`            DATETIME     DEFAULT NULL,
    `client_op_id`        VARCHAR(64)  DEFAULT NULL COMMENT '客户端幂等 ID',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `archived`, `fixed`),
    KEY `idx_client_op` (`client_op_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '目标表';

-- ---------------------------------------------------------------------
-- 阶段表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `phase`;
CREATE TABLE `phase` (
    `id`            VARCHAR(32)  NOT NULL COMMENT '阶段ID（p_xxx）',
    `goal_id`       VARCHAR(32)  NOT NULL,
    `user_id`       VARCHAR(32)  NOT NULL,
    `name`          VARCHAR(128) NOT NULL,
    `description`   VARCHAR(512) DEFAULT NULL,
    `total_minutes` INT          NOT NULL DEFAULT 0 COMMENT '阶段总时长目标',
    `start_date`    DATE         NOT NULL,
    `end_date`      DATE         NOT NULL,
    `sort`          INT          NOT NULL DEFAULT 0,
    `completed`     TINYINT(1)   NOT NULL DEFAULT 0,
    `completed_at`  DATETIME     DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_goal` (`goal_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '目标阶段表';

-- ---------------------------------------------------------------------
-- 奖励表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `reward`;
CREATE TABLE `reward` (
    `id`            VARCHAR(32)  NOT NULL COMMENT '奖励ID（r_xxx）',
    `goal_id`       VARCHAR(32)  NOT NULL,
    `user_id`       VARCHAR(32)  NOT NULL,
    `name`          VARCHAR(128) NOT NULL,
    `content`       VARCHAR(512) DEFAULT NULL,
    `trigger_type`  VARCHAR(16)  NOT NULL COMMENT 'progress|phase|days',
    `trigger_value` INT          NOT NULL COMMENT '阈值',
    `sort`          INT          NOT NULL DEFAULT 0,
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'locked' COMMENT 'locked|available|claimed',
    `claimed_at`    DATETIME     DEFAULT NULL,
    `client_op_id`  VARCHAR(64)  DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_goal` (`goal_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '阶段奖励表';

-- ---------------------------------------------------------------------
-- 打卡记录表（按 goal+date 唯一）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `checkin`;
CREATE TABLE `checkin` (
    `id`           VARCHAR(32)  NOT NULL COMMENT '打卡ID（c_xxx）',
    `goal_id`      VARCHAR(32)  NOT NULL,
    `user_id`      VARCHAR(32)  NOT NULL,
    `checkin_date` DATE         NOT NULL,
    `status`       VARCHAR(16)  NOT NULL COMMENT 'done|late|missed|paused',
    `duration`     INT          NOT NULL DEFAULT 0 COMMENT '实际完成分钟数',
    `note`         VARCHAR(512) DEFAULT NULL,
    `client_op_id` VARCHAR(64)  DEFAULT NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_goal_date` (`goal_id`, `checkin_date`, `deleted`),
    KEY `idx_user_date` (`user_id`, `checkin_date`),
    KEY `idx_client_op` (`client_op_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '打卡记录表';

-- ---------------------------------------------------------------------
-- 复盘表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`             VARCHAR(32)  NOT NULL COMMENT '复盘ID（rev_xxx）',
    `user_id`        VARCHAR(32)  NOT NULL,
    `goal_id`        VARCHAR(32)  DEFAULT NULL,
    `type`           VARCHAR(16)  NOT NULL COMMENT 'weekly|monthly|manual',
    `title`          VARCHAR(255) DEFAULT NULL,
    `review_date`    DATE         NOT NULL,
    `content`        TEXT,
    `metrics_json`   TEXT COMMENT '复盘度量指标 JSON',
    `suggestions_json` TEXT COMMENT '调整建议 JSON 数组',
    `is_favorite`    TINYINT(1)   NOT NULL DEFAULT 0,
    `client_op_id`   VARCHAR(64)  DEFAULT NULL,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_type_date` (`user_id`, `type`, `review_date`),
    KEY `idx_goal` (`goal_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '复盘表';

-- ---------------------------------------------------------------------
-- 特殊场景表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene`;
CREATE TABLE `scene` (
    `id`           VARCHAR(32)  NOT NULL COMMENT '场景ID（s_xxx）',
    `user_id`      VARCHAR(32)  NOT NULL,
    `type`         VARCHAR(16)  NOT NULL COMMENT 'holiday|travel|sick|other',
    `label`        VARCHAR(128) NOT NULL,
    `start_date`   DATE         NOT NULL,
    `end_date`     DATE         NOT NULL,
    `mode`         VARCHAR(16)  NOT NULL COMMENT 'shorten|extend|pause',
    `shorten_to`   INT          DEFAULT NULL,
    `extend_hours` INT          DEFAULT NULL,
    `client_op_id` VARCHAR(64)  DEFAULT NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `start_date`, `end_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '特殊场景表';

-- ---------------------------------------------------------------------
-- 通知与设备
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`         VARCHAR(32)  NOT NULL COMMENT '通知ID（n_xxx）',
    `user_id`    VARCHAR(32)  NOT NULL,
    `event`      VARCHAR(64)  NOT NULL COMMENT '事件类型',
    `title`      VARCHAR(128) DEFAULT NULL,
    `content`    VARCHAR(512) DEFAULT NULL,
    `payload_json` TEXT,
    `is_read`    TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应用内通知';

DROP TABLE IF EXISTS `notification_device`;
CREATE TABLE `notification_device` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    VARCHAR(32)  NOT NULL,
    `platform`   VARCHAR(16)  NOT NULL COMMENT 'ios|android',
    `push_token` VARCHAR(255) NOT NULL,
    `device_id`  VARCHAR(128) DEFAULT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_token` (`user_id`, `push_token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '推送设备';

-- ---------------------------------------------------------------------
-- 同步幂等记录表（client_op_id 去重）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sync_op_log`;
CREATE TABLE `sync_op_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      VARCHAR(32)  NOT NULL,
    `client_op_id` VARCHAR(64)  NOT NULL,
    `op_type`      VARCHAR(64)  NOT NULL,
    `payload_json` TEXT,
    `result_json`  TEXT,
    `success`      TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_op` (`user_id`, `client_op_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '同步幂等记录';

-- ---------------------------------------------------------------------
-- 审计日志（自动进阶等系统行为）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    VARCHAR(32)  NOT NULL,
    `target_id`  VARCHAR(32)  DEFAULT NULL COMMENT '操作目标ID（goal_id 等）',
    `action`     VARCHAR(64)  NOT NULL,
    `detail`     VARCHAR(1024) DEFAULT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_action` (`user_id`, `action`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审计日志';
