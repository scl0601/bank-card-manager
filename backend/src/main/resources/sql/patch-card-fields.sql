-- ============================================
-- 银行卡表字段收敛
-- 1. 保留新增/编辑所需字段：用户、银行、尾号、卡类型、信用额度、账单日、还款日、有效期、状态、APP、备注
-- 2. 删除 bank_card 未使用字段：card_no、owner_id、used_amount、owner_relation、owner_name、total_limit
-- 3. APP 字段沿用 repay_method 存储，值限定为 cloudpay/wechat/alipay/bankapp/none/other
-- ============================================

USE bank_admin;

-- 删除前尽量保留旧借记卡额度数据
SET @copy_total_limit = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'total_limit') > 0,
  'UPDATE `bank_card` SET `credit_limit` = `total_limit` WHERE `credit_limit` IS NULL AND `total_limit` IS NOT NULL',
  'SELECT ''skip: total_limit not found'''
);
PREPARE s_copy_total_limit FROM @copy_total_limit; EXECUTE s_copy_total_limit; DEALLOCATE PREPARE s_copy_total_limit;

SET @add_balance = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'balance') = 0,
  'ALTER TABLE `bank_card` ADD COLUMN `balance` DECIMAL(18,2) DEFAULT 0.00 COMMENT ''当前余额（流水模块维护）'' AFTER `credit_limit`',
  'SELECT ''skip: balance exists'''
);
PREPARE s_add_balance FROM @add_balance; EXECUTE s_add_balance; DEALLOCATE PREPARE s_add_balance;

-- APP 字段补齐和旧值迁移
SET @add_app = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'repay_method') = 0,
  'ALTER TABLE `bank_card` ADD COLUMN `repay_method` VARCHAR(20) DEFAULT ''cloudpay'' COMMENT ''APP：cloudpay云闪付 wechat微信 alipay支付宝 bankapp银行APP none无 other其他'' AFTER `status`',
  'SELECT ''skip: repay_method exists'''
);
PREPARE s_add_app FROM @add_app; EXECUTE s_add_app; DEALLOCATE PREPARE s_add_app;

UPDATE `bank_card` SET `repay_method` = 'other' WHERE `repay_method` = 'invoice';
UPDATE `bank_card` SET `repay_method` = 'cloudpay' WHERE `repay_method` IS NULL OR `repay_method` = '';
UPDATE `bank_card` SET `repay_method` = 'other' WHERE `repay_method` NOT IN ('cloudpay', 'wechat', 'alipay', 'bankapp', 'none', 'other');

ALTER TABLE `bank_card`
  MODIFY COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT 'APP：cloudpay云闪付 wechat微信 alipay支付宝 bankapp银行APP none无 other其他' AFTER `status`,
  MODIFY COLUMN `remark` VARCHAR(500) DEFAULT NULL AFTER `repay_method`;

SET @drop_verified = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'verified') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `verified`',
  'SELECT ''skip: verified not found'''
);
PREPARE s_drop_verified FROM @drop_verified; EXECUTE s_drop_verified; DEALLOCATE PREPARE s_drop_verified;

-- 安全删除废弃字段
SET @drop_card_no = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'card_no') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `card_no`',
  'SELECT ''skip: card_no not found'''
);
PREPARE s_drop_card_no FROM @drop_card_no; EXECUTE s_drop_card_no; DEALLOCATE PREPARE s_drop_card_no;

SET @drop_owner_id = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'owner_id') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `owner_id`',
  'SELECT ''skip: owner_id not found'''
);
PREPARE s_drop_owner_id FROM @drop_owner_id; EXECUTE s_drop_owner_id; DEALLOCATE PREPARE s_drop_owner_id;

SET @drop_used_amount = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'used_amount') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `used_amount`',
  'SELECT ''skip: used_amount not found'''
);
PREPARE s_drop_used_amount FROM @drop_used_amount; EXECUTE s_drop_used_amount; DEALLOCATE PREPARE s_drop_used_amount;

SET @drop_owner_relation = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'owner_relation') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `owner_relation`',
  'SELECT ''skip: owner_relation not found'''
);
PREPARE s_drop_owner_relation FROM @drop_owner_relation; EXECUTE s_drop_owner_relation; DEALLOCATE PREPARE s_drop_owner_relation;

SET @drop_owner_name = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'owner_name') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `owner_name`',
  'SELECT ''skip: owner_name not found'''
);
PREPARE s_drop_owner_name FROM @drop_owner_name; EXECUTE s_drop_owner_name; DEALLOCATE PREPARE s_drop_owner_name;

SET @drop_total_limit = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'total_limit') > 0,
  'ALTER TABLE `bank_card` DROP COLUMN `total_limit`',
  'SELECT ''skip: total_limit not found'''
);
PREPARE s_drop_total_limit FROM @drop_total_limit; EXECUTE s_drop_total_limit; DEALLOCATE PREPARE s_drop_total_limit;
