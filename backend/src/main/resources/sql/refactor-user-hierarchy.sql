-- =============================================
-- 用户表重构：合并 card_owner + card_supplier → card_user（两级层级模型）
-- 银行卡表简化：owner_id + supplier_id → user_id
-- 执行方式：在已有数据库上运行此迁移脚本
-- =============================================

USE bank_admin;

-- ===================== 1. 新建 card_user 表（合并后的用户表） =====================
CREATE TABLE IF NOT EXISTS `card_user` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`     BIGINT         DEFAULT NULL            COMMENT '父用户ID（NULL=一级用户/主账户）',
  `name`          VARCHAR(64)    NOT NULL                COMMENT '姓名/名称',
  `phone`         VARCHAR(20)    DEFAULT NULL            COMMENT '联系电话',
  `fee_rate`      DECIMAL(5,2)   NOT NULL DEFAULT 0.00   COMMENT '手续费率%（仅一级有效，二级继承父级）',
  `remark`        VARCHAR(500)   DEFAULT NULL            COMMENT '备注',
  `sort_order`    INT            NOT NULL DEFAULT 0      COMMENT '排序号',
  `status`        TINYINT        NOT NULL DEFAULT 0      COMMENT '状态：0正常 1停用',
  `is_deleted`    TINYINT(1)     NOT NULL DEFAULT 0,
  `create_by`     VARCHAR(64)    DEFAULT NULL,
  `create_time`   DATETIME       DEFAULT NULL,
  `update_by`     VARCHAR(64)    DEFAULT NULL,
  `update_time`   DATETIME       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status`    (`status`),
  KEY `idx_name`      (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（含两级层级关系）';

-- ===================== 2. 迁移旧数据：card_owner → card_user（作为一级用户） =====================
INSERT INTO `card_user` (`name`, `phone`, `fee_rate`, `remark`, `status`, `is_deleted`, `create_time`)
SELECT `name`, NULL, 0.00, COALESCE(`remark`, ''), `status`, `is_deleted`, `create_time`
FROM `card_owner`
WHERE `is_deleted` = 0;

-- ===================== 3. 银行卡表：新增 user_id 字段（幂等） =====================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'bank_admin' AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'user_id');
SET @sql = IF(@col_exists > 0, 'SELECT ''user_id column already exists''',
    'ALTER TABLE `bank_card` ADD COLUMN `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT ''用户ID'' AFTER `id`');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===================== 4. 银行卡数据迁移：用 owner_id 填充 user_id =====================
UPDATE `bank_card` SET `user_id` = `owner_id`
WHERE `user_id` = 0 AND `is_deleted` = 0;

-- ===================== 5. 测试数据：建立层级关系示例 =====================
-- 将"李娜"、"王芳"、"赵强"设为"张伟"(id=1)的子用户（模拟家庭场景）
-- 注意：实际ID取决于上面INSERT的顺序，这里假设按card_owner原顺序迁移
UPDATE `card_user` SET `parent_id` = 1 WHERE `name` IN ('李娜', '王芳', '赵强') AND `parent_id` IS NULL;
-- 给张伟设置一个费率示例
UPDATE `card_user` SET `fee_rate` = 0.50 WHERE `name` = '张伟' AND `parent_id` IS NULL;
