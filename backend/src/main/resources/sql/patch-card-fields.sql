-- ============================================
-- 银行卡表字段变更
-- 1. 删除遗留 owner_id 字段（已由 user_id 替代）
-- 2. 删除 used_amount（已用额度）字段
-- 3. repay_day 改为 VARCHAR(7)（与有效期格式一致）
-- 4. 新增 total_limit 总额度（借记卡用）
-- 5. 新增 repay_method 还款方式
-- 6. 新增 verified 是否核实
-- 7. 修复 card_bill.fee_rate 类型过小问题
-- ============================================

USE bank_admin;

-- 安全删除可能存在的旧字段
SET @drop1 = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'owner_id') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `owner_id`',
  'SELECT ''skip: owner_id not found'''
);
PREPARE s1 FROM @drop1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @drop2 = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'used_amount') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `used_amount`',
  'SELECT ''skip: used_amount not found'''
);
PREPARE s2 FROM @drop2; EXECUTE s2; DEALLOCATE PREPARE s2;

ALTER TABLE `bank_card`
  MODIFY COLUMN `repay_day`   VARCHAR(7) DEFAULT NULL COMMENT '还款日（格式同有效期 yyyy-MM）',
  ADD COLUMN    `total_limit` DECIMAL(18,2) DEFAULT NULL COMMENT '总额度（借记卡用）' AFTER `balance`,
  ADD COLUMN    `repay_method` VARCHAR(20)  DEFAULT 'cloudpay' COMMENT '还款方式：cloudpay云闪付 invoice开票' AFTER `remark`,
  ADD COLUMN    `verified`     TINYINT(1)    DEFAULT NULL COMMENT '是否核实（云闪付时有效）' AFTER `repay_method`;

-- 统一 card_bill.fee_rate 为百分数口径，数据库直接保存百分比值（如1表示1%）
SET @fix_fee = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'card_bill' AND COLUMN_NAME = 'fee_rate') > 0,
  'ALTER TABLE `card_bill` MODIFY COLUMN `fee_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT ''费率%（如1表示1%）''',
  'SELECT ''skip: card_bill.fee_rate not found'''
);
PREPARE s3 FROM @fix_fee; EXECUTE s3; DEALLOCATE PREPARE s3;
