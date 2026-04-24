-- ===================== 账单明细表（POS刷卡流水/客户还款记录） =====================
CREATE TABLE IF NOT EXISTS `bill_detail` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `bill_id`         BIGINT          NOT NULL                COMMENT '关联账单ID',
    `detail_date`     DATE            NOT NULL                COMMENT '明细日期',
    `description`     VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '描述',
    `amount`          DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '金额（正=收入 负=支出）',
    `detail_type`     TINYINT         NOT NULL DEFAULT 0      COMMENT '0=POS刷出(支出) 1=还信用卡(支出) 2=客户还钱给我(收入)',
    `remark`           VARCHAR(500)    DEFAULT NULL,
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0,
    `create_by`       VARCHAR(64)     DEFAULT NULL,
    `create_time`     DATETIME        DEFAULT NULL,
    `update_by`       VARCHAR(64)     DEFAULT NULL,
    `update_time`     DATETIME        DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_bill_id`     (`bill_id`),
    KEY `idx_detail_date` (`detail_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细表';

-- ===================== card_bill 表增加费率相关字段 =====================
ALTER TABLE `card_bill`
    ADD COLUMN `fee_rate`   DECIMAL(8,2) DEFAULT 0.00 COMMENT '约定费率%（如1表示1%）' AFTER `remark`,
    ADD COLUMN `fee_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '本期应收手续费（=billAmount×feeRate/100）' AFTER `fee_rate`;
