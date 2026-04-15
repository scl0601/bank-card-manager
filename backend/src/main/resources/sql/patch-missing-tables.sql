-- ============================================
-- 银行卡管理后台系统 - 数据库补全脚本
-- 目标数据库: CynosDB (bank_admin)
-- 执行方式: 在 CynosDB 控制台或 Navicat/DBeaver 中执行
-- ============================================

USE bank_admin;

-- ===================== 1. 系统用户表 =====================
CREATE TABLE IF NOT EXISTS `bank_sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名',
  `password`    VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `role`        VARCHAR(32)  NOT NULL DEFAULT 'VIEWER' COMMENT '角色：ADMIN/OPERATOR/VIEWER',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0正常 1禁用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ===================== 2. 持卡人表 =====================
CREATE TABLE IF NOT EXISTS `card_owner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '姓名',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `id_card`     VARCHAR(255) DEFAULT NULL            COMMENT '身份证号',
  `remark`      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0正常 1禁用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持卡人表';

-- ===================== 3. 银行卡表 =====================
CREATE TABLE IF NOT EXISTS `bank_card` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`        BIGINT          NOT NULL                COMMENT '持卡人ID',
  `bank_name`       VARCHAR(64)     NOT NULL                COMMENT '银行名称',
  `card_no`         VARCHAR(255)    NOT NULL                COMMENT '卡号（加密）',
  `card_no_last4`   CHAR(4)         NOT NULL                COMMENT '卡号后四位',
  `card_type`       TINYINT         NOT NULL DEFAULT 1      COMMENT '1借记卡 2信用卡',
  `credit_limit`    DECIMAL(18,2)   DEFAULT NULL            COMMENT '信用额度',
  `balance`         DECIMAL(18,2)   DEFAULT 0.00            COMMENT '当前余额',
  `used_amount`     DECIMAL(18,2)   DEFAULT 0.00            COMMENT '已用额度',
  `bill_day`        TINYINT         DEFAULT NULL            COMMENT '账单日',
  `repay_day`       TINYINT         DEFAULT NULL            COMMENT '还款日',
  `expire_date`     VARCHAR(7)      DEFAULT NULL            COMMENT '有效期（yyyy-MM）',
  `status`          TINYINT         NOT NULL DEFAULT 0      COMMENT '0正常 1冻结 2注销',
  `remark`          VARCHAR(500)    DEFAULT NULL,
  `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`       VARCHAR(64)     DEFAULT NULL,
  `create_time`     DATETIME        DEFAULT NULL,
  `update_by`       VARCHAR(64)     DEFAULT NULL,
  `update_time`     DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id`    (`owner_id`),
  KEY `idx_card_type`   (`card_type`),
  KEY `idx_status`      (`status`),
  KEY `idx_expire_date` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行卡表';

-- ===================== 4. 流水表 =====================
CREATE TABLE IF NOT EXISTS `card_transaction` (
  `id`               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `card_id`          BIGINT          NOT NULL                COMMENT '银行卡ID',
  `owner_id`         BIGINT          NOT NULL                COMMENT '持卡人ID',
  `tx_type`          TINYINT         NOT NULL                COMMENT '1收入 2支出',
  `amount`           DECIMAL(18,2)   NOT NULL                COMMENT '交易金额',
  `tx_date`          DATE            NOT NULL                COMMENT '交易日期',
  `description`      VARCHAR(255)    DEFAULT NULL            COMMENT '交易描述',
  `counterpart`      VARCHAR(255)    DEFAULT NULL            COMMENT '对手方',
  `balance_snapshot` DECIMAL(18,2)   DEFAULT NULL            COMMENT '余额快照',
  `remark`           VARCHAR(500)    DEFAULT NULL,
  `is_deleted`       TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`        VARCHAR(64)     DEFAULT NULL,
  `create_time`      DATETIME        DEFAULT NULL,
  `update_by`        VARCHAR(64)     DEFAULT NULL,
  `update_time`      DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_card_id`  (`card_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_tx_date`  (`tx_date`),
  KEY `idx_tx_type`  (`tx_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水记录表';

-- ===================== 5. 账单表 =====================
CREATE TABLE IF NOT EXISTS `card_bill` (
  `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `card_id`           BIGINT          NOT NULL                COMMENT '银行卡ID',
  `owner_id`          BIGINT          NOT NULL                COMMENT '持卡人ID',
  `bill_month`        VARCHAR(7)      NOT NULL                COMMENT '账单月份（yyyy-MM）',
  `bill_amount`       DECIMAL(18,2)   NOT NULL                COMMENT '账单金额',
  `min_pay_amount`    DECIMAL(18,2)   DEFAULT NULL            COMMENT '最低还款额',
  `repay_date`        DATE            DEFAULT NULL            COMMENT '还款日',
  `actual_pay_amount` DECIMAL(18,2)   DEFAULT NULL            COMMENT '实际还款金额',
  `actual_pay_date`   DATE            DEFAULT NULL            COMMENT '实际还款日期',
  `status`            TINYINT         NOT NULL DEFAULT 0      COMMENT '0待还款 1已还清 2部分还款 3逾期',
  `remark`            VARCHAR(500)    DEFAULT NULL,
  `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`         VARCHAR(64)     DEFAULT NULL,
  `create_time`       DATETIME        DEFAULT NULL,
  `update_by`         VARCHAR(64)     DEFAULT NULL,
  `update_time`       DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_card_id`    (`card_id`),
  KEY `idx_owner_id`   (`owner_id`),
  KEY `idx_bill_month` (`bill_month`),
  KEY `idx_status`     (`status`),
  KEY `idx_repay_date` (`repay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- ===================== 6. 提醒任务表 =====================
CREATE TABLE IF NOT EXISTS `reminder_task` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`      BIGINT          NOT NULL                COMMENT '持卡人ID',
  `card_id`       BIGINT          DEFAULT NULL            COMMENT '银行卡ID',
  `bill_id`       BIGINT          DEFAULT NULL            COMMENT '账单ID',
  `reminder_type` TINYINT         NOT NULL                COMMENT '1即将到期 2今日到期 3已逾期 4卡片即将过期',
  `reminder_date` DATE            NOT NULL                COMMENT '提醒日期',
  `content`       VARCHAR(500)    NOT NULL                COMMENT '提醒内容',
  `status`        TINYINT         NOT NULL DEFAULT 0      COMMENT '0待处理 1已处理 2已忽略',
  `handled_time`  DATETIME        DEFAULT NULL,
  `handled_by`    VARCHAR(64)     DEFAULT NULL,
  `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`     VARCHAR(64)     DEFAULT NULL,
  `create_time`   DATETIME        DEFAULT NULL,
  `update_by`     VARCHAR(64)     DEFAULT NULL,
  `update_time`   DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id`      (`owner_id`),
  KEY `idx_reminder_date` (`reminder_date`),
  KEY `idx_status`        (`status`),
  KEY `idx_type_status`   (`reminder_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒任务表';

-- ===================== 7. 操作日志表 =====================
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator`       VARCHAR(64)   DEFAULT NULL COMMENT '操作人',
  `module`         VARCHAR(64)   DEFAULT NULL COMMENT '操作模块',
  `action`         VARCHAR(64)   DEFAULT NULL COMMENT '操作类型',
  `description`    VARCHAR(255)  DEFAULT NULL COMMENT '操作描述',
  `request_path`   VARCHAR(255)  DEFAULT NULL COMMENT '请求路径',
  `request_method` VARCHAR(16)   DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT          DEFAULT NULL COMMENT '请求参数',
  `result`         TINYINT       DEFAULT 0    COMMENT '0成功 1失败',
  `error_msg`      VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
  `client_ip`      VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP',
  `duration`       BIGINT        DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time`    DATETIME      DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator`    (`operator`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ===================== 8. 记账分类表 =====================
CREATE TABLE IF NOT EXISTS `book_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '分类名称',
  `icon`        VARCHAR(32)  DEFAULT NULL            COMMENT '图标标识',
  `type`        TINYINT      NOT NULL                COMMENT '1收入 2支出',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父级ID（0=一级）',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '0启用 1停用',
  `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账分类表';

-- ===================== 9. 个人记账表 =====================
CREATE TABLE IF NOT EXISTS `personal_book` (
  `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_date`   DATE            NOT NULL                COMMENT '记账日期',
  `book_type`   TINYINT         NOT NULL                COMMENT '1收入 2支出',
  `amount`      DECIMAL(18,2)   NOT NULL                COMMENT '金额',
  `category_id` BIGINT          NOT NULL                COMMENT '分类ID',
  `description` VARCHAR(255)    DEFAULT NULL            COMMENT '描述/备注',
  `card_id`     BIGINT          DEFAULT NULL            COMMENT '关联银行卡ID（可选）',
  `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`   VARCHAR(64)     DEFAULT NULL,
  `create_time` DATETIME        DEFAULT NULL,
  `update_by`   VARCHAR(64)     DEFAULT NULL,
  `update_time` DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_book_date` (`book_date`),
  KEY `idx_book_type` (`book_type`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_card_id` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人记账表';

-- ===================== 10. 日程事项表 =====================
CREATE TABLE IF NOT EXISTS `calendar_event` (
  `id`               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`            VARCHAR(100)    NOT NULL                COMMENT '标题',
  `description`      VARCHAR(500)    DEFAULT NULL            COMMENT '描述内容',
  `event_date`       DATE            NOT NULL                COMMENT '事项日期',
  `start_time`       TIME            DEFAULT NULL            COMMENT '开始时间',
  `end_time`         TIME            DEFAULT NULL            COMMENT '结束时间',
  `category`         TINYINT         NOT NULL DEFAULT 0      COMMENT '分类：0工作 1生活 2学习 3健康 4其他',
  `priority`         TINYINT         NOT NULL DEFAULT 1      COMMENT '优先级：0低 1中 2高',
  `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0待办 1进行中 2已完成 3已取消',
  `remind_before_min`INT             DEFAULT NULL            COMMENT '提前提醒分钟数（null=不提醒）',
  `remark`           VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  `is_deleted`       TINYINT(1)      NOT NULL DEFAULT 0,
  `create_by`        VARCHAR(64)     DEFAULT NULL,
  `create_time`      DATETIME        DEFAULT NULL,
  `update_by`        VARCHAR(64)     DEFAULT NULL,
  `update_time`      DATETIME        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_date`  (`event_date`),
  KEY `idx_category`    (`category`),
  KEY `idx_status`      (`status`),
  KEY `idx_date_status` (`event_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程事项表';

-- ===================== 11. 登录/日志关键字段修复 =====================
-- 用于修复旧库表结构不完整导致的登录 500、操作日志保存失败等问题
ALTER TABLE `bank_sys_user`
  ADD COLUMN IF NOT EXISTS `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  ADD COLUMN IF NOT EXISTS `role` VARCHAR(32) NOT NULL DEFAULT 'VIEWER' COMMENT '角色：ADMIN/OPERATOR/VIEWER',
  ADD COLUMN IF NOT EXISTS `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1禁用',
  ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `create_by` VARCHAR(64) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `create_time` DATETIME DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `update_by` VARCHAR(64) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `update_time` DATETIME DEFAULT NULL;

ALTER TABLE `operation_log`
  ADD COLUMN IF NOT EXISTS `request_path` VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
  ADD COLUMN IF NOT EXISTS `request_method` VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  ADD COLUMN IF NOT EXISTS `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  ADD COLUMN IF NOT EXISTS `result` TINYINT DEFAULT 0 COMMENT '0成功 1失败',
  ADD COLUMN IF NOT EXISTS `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  ADD COLUMN IF NOT EXISTS `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  ADD COLUMN IF NOT EXISTS `duration` BIGINT DEFAULT NULL COMMENT '执行耗时（毫秒）';

-- ===================== 初始管理员账号 =====================
INSERT IGNORE INTO `bank_sys_user` (`username`, `password`, `nickname`, `role`, `status`, `is_deleted`, `create_time`)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'ADMIN', 0, 0, NOW());

-- ===================== 默认分类数据 =====================
-- 收入分类
INSERT IGNORE INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
('工资',     'Money', 1, 1, 0, 0, NOW()),
('奖金',     'Trophy', 1, 2, 0, 0, NOW()),
('兼职',     'Briefcase', 1, 3, 0, 0, NOW()),
('投资理财', 'TrendCharts', 1, 4, 0, 0, NOW()),
('其他收入', 'MoreFilled', 1, 99, 0, 0, NOW());

-- 支出一级分类
INSERT IGNORE INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
('餐饮',     'Food',     2, 1, 0, 0, NOW()),
('交通',     'Van',      2, 2, 0, 0, NOW()),
('购物',     'ShoppingBag', 2, 3, 0, 0, NOW()),
('居住',     'House',    2, 4, 0, 0, NOW()),
('医疗',     'FirstAidKit', 2, 5, 0, 0, NOW()),
('教育',     'Reading',  2, 6, 0, 0, NOW()),
('娱乐',     'Film',     2, 7, 0, 0, NOW()),
('人情',     'Present',  2, 8, 0, 0, NOW()),
('其他支出', 'MoreFilled', 2, 99, 0, 0, NOW());

-- 支出二级分类
INSERT IGNORE INTO `book_category` (`name`, `icon`, `type`, `sort_order`, `parent_id`, `status`, `create_time`) VALUES
('早餐',     null, 2, 1, 6, 0, NOW()),
('午餐',     null, 2, 2, 6, 0, NOW()),
('晚餐',     null, 2, 3, 6, 0, NOW()),
('零食饮料', null, 2, 4, 6, 0, NOW()),
('公交地铁', null, 2, 1, 7, 0, NOW()),
('打车',     null, 2, 2, 7, 0, NOW()),
('停车费',   null, 2, 3, 7, 0, NOW()),
('日用品',   null, 2, 1, 8, 0, NOW()),
('服装鞋包', null, 2, 2, 8, 0, NOW()),
('电子产品', null, 2, 3, 8, 0, NOW()),
('房租',     null, 2, 1, 9, 0, NOW()),
('水电燃气', null, 2, 2, 9, 0, NOW()),
('网费',     null, 2, 3, 9, 0, NOW()),
('物业',     null, 2, 4, 9, 0, NOW());

-- 验证所有表是否创建成功
SELECT 
    TABLE_NAME as '表名',
    TABLE_COMMENT as '说明',
    TABLE_ROWS as '数据量'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'bank_admin' 
ORDER BY TABLE_NAME;
