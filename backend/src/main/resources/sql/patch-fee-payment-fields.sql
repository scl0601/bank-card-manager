-- 手续费支付信息字段补丁（兼容旧库）
-- 执行前请先选中目标数据库，例如：USE bank_admin;

SET @ddl = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'card_bill'
     AND COLUMN_NAME = 'fee_paid_amount') = 0,
  'ALTER TABLE `card_bill` ADD COLUMN `fee_paid_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT ''手续费已支付金额'' AFTER `fee_paid`',
  'SELECT ''fee_paid_amount already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'card_bill'
     AND COLUMN_NAME = 'fee_pay_time') = 0,
  'ALTER TABLE `card_bill` ADD COLUMN `fee_pay_time` DATETIME DEFAULT NULL COMMENT ''最近手续费支付时间'' AFTER `fee_paid_amount`',
  'SELECT ''fee_pay_time already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'card_bill'
     AND COLUMN_NAME = 'fee_pay_method') = 0,
  'ALTER TABLE `card_bill` ADD COLUMN `fee_pay_method` VARCHAR(20) DEFAULT NULL COMMENT ''手续费支付方式：wechat/alipay/cash/other'' AFTER `fee_pay_time`',
  'SELECT ''fee_pay_method already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `card_bill`
SET `fee_paid_amount` = IFNULL(`fee_amount`, 0)
WHERE `fee_paid` = 1
  AND IFNULL(`fee_paid_amount`, 0) = 0;
