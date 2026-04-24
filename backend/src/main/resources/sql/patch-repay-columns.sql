-- 代还业务管理模块 - 补字段SQL（兼容旧库）
-- 手工执行：在已有库上执行，补齐所需字段（若字段已存在则跳过）

USE bank_admin;

-- 用户表补手续费率字段（与 init.sql 保持一致：fee_rate 表示百分比数值，如 1 表示 1%）
ALTER TABLE `card_user`
  ADD COLUMN IF NOT EXISTS `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '费率%（如1表示1%）' AFTER `parent_id`;

-- 银行卡表补归属人信息字段
ALTER TABLE `bank_card`
  ADD COLUMN IF NOT EXISTS `owner_relation` VARCHAR(32) DEFAULT '本人' COMMENT '卡片归属人关系：本人/配偶/子女等' AFTER `card_no_last4`;

ALTER TABLE `bank_card`
  ADD COLUMN IF NOT EXISTS `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '卡片归属人姓名（可选）' AFTER `owner_relation`;

-- 新增代还月度账单条目表（若未执行 repay-tables.sql，可直接执行此段）
CREATE TABLE IF NOT EXISTS `repay_month_bill` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`     BIGINT        NOT NULL COMMENT '用户ID（关联card_user）',
  `card_id`      BIGINT        NOT NULL COMMENT '银行卡ID（关联bank_card）',
  `bill_year`    INT           NOT NULL COMMENT '账单年份',
  `bill_month`   VARCHAR(7)    NOT NULL COMMENT '账单月份（yyyy-MM）',
  `bill_day`     TINYINT       DEFAULT NULL COMMENT '账单日（每月几号）',
  `repay_day`    TINYINT       DEFAULT NULL COMMENT '还款日（每月几号）',
  `repay_date`   DATE          DEFAULT NULL COMMENT '还款日（当月具体日期）',
  `repay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当月实际代还金额',
  `pos_cost`     DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当月POS机使用成本',
  `fee_income`   DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当月手续费收入（repay_amount * fee_rate）',
  `net_profit`   DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当月净利润（fee_income - pos_cost）',
  `remark`       VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `is_deleted`   TINYINT(1)    NOT NULL DEFAULT 0,
  `create_by`    VARCHAR(64)   DEFAULT NULL,
  `create_time`  DATETIME      DEFAULT NULL,
  `update_by`    VARCHAR(64)   DEFAULT NULL,
  `update_time`  DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_card_month` (`owner_id`, `card_id`, `bill_month`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_card_id` (`card_id`),
  KEY `idx_bill_month` (`bill_month`),
  KEY `idx_repay_date` (`repay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代还月度账单条目表';
