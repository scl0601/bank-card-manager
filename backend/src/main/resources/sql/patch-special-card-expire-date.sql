-- 特殊通道银行卡有效期字段补丁（兼容旧库）
-- 执行前请先选中目标数据库，例如：USE bank_admin;

SET @ddl = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'special_bank_card'
     AND COLUMN_NAME = 'expire_date') = 0,
  'ALTER TABLE `special_bank_card` ADD COLUMN `expire_date` VARCHAR(32) DEFAULT NULL COMMENT ''有效期（原样保存用户输入）'' AFTER `total_amount`',
  'SELECT ''expire_date already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
