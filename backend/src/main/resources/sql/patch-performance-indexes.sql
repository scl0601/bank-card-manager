-- Performance indexes for frequently queried tables.
-- Safe to run repeatedly.

SET @idx_user_feedback_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'user_feedback'
     AND INDEX_NAME = 'idx_feedback_lookup') = 0,
  'ALTER TABLE `user_feedback` ADD INDEX `idx_feedback_lookup` (`status`, `priority`, `create_time`, `submitter`)',
  'SELECT ''idx_feedback_lookup already exists'''
);
PREPARE stmt FROM @idx_user_feedback_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_user_feedback_attachment_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'user_feedback_attachment'
     AND INDEX_NAME = 'idx_feedback_attachment_lookup') = 0,
  'ALTER TABLE `user_feedback_attachment` ADD INDEX `idx_feedback_attachment_lookup` (`feedback_id`, `sort_no`, `id`)',
  'SELECT ''idx_feedback_attachment_lookup already exists'''
);
PREPARE stmt FROM @idx_user_feedback_attachment_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_user_feedback_process_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'user_feedback_process_log'
     AND INDEX_NAME = 'idx_feedback_process_lookup') = 0,
  'ALTER TABLE `user_feedback_process_log` ADD INDEX `idx_feedback_process_lookup` (`feedback_id`, `operate_time`, `id`)',
  'SELECT ''idx_feedback_process_lookup already exists'''
);
PREPARE stmt FROM @idx_user_feedback_process_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_calendar_event_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'calendar_event'
     AND INDEX_NAME = 'idx_calendar_event_lookup') = 0,
  'ALTER TABLE `calendar_event` ADD INDEX `idx_calendar_event_lookup` (`event_date`, `status`, `create_by`, `id`)',
  'SELECT ''idx_calendar_event_lookup already exists'''
);
PREPARE stmt FROM @idx_calendar_event_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_special_bank_card_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'special_bank_card'
     AND INDEX_NAME = 'idx_special_bank_card_lookup') = 0,
  'ALTER TABLE `special_bank_card` ADD INDEX `idx_special_bank_card_lookup` (`user_id`, `status`, `id`)',
  'SELECT ''idx_special_bank_card_lookup already exists'''
);
PREPARE stmt FROM @idx_special_bank_card_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_special_card_bill_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'special_card_bill'
     AND INDEX_NAME = 'idx_special_card_bill_lookup') = 0,
  'ALTER TABLE `special_card_bill` ADD INDEX `idx_special_card_bill_lookup` (`card_id`, `user_id`, `bill_year`, `bill_month_no`, `id`)',
  'SELECT ''idx_special_card_bill_lookup already exists'''
);
PREPARE stmt FROM @idx_special_card_bill_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_card_transaction_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'card_transaction'
     AND INDEX_NAME = 'idx_card_transaction_lookup') = 0,
  'ALTER TABLE `card_transaction` ADD INDEX `idx_card_transaction_lookup` (`card_id`, `owner_id`, `tx_type`, `tx_date`, `id`)',
  'SELECT ''idx_card_transaction_lookup already exists'''
);
PREPARE stmt FROM @idx_card_transaction_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_book_lookup = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'personal_book'
     AND INDEX_NAME = 'idx_book_lookup') = 0,
  'ALTER TABLE `personal_book` ADD INDEX `idx_book_lookup` (`create_by`, `book_type`, `book_date`, `card_id`, `account_id`)',
  'SELECT ''idx_book_lookup already exists'''
);
PREPARE stmt FROM @idx_book_lookup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
