-- Optimize monthly bill detail lookup by bill_id with stable display ordering.
SET @add_bill_detail_query_idx = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'bill_detail'
     AND INDEX_NAME = 'idx_bill_detail_query') = 0,
  'ALTER TABLE `bill_detail` ADD INDEX `idx_bill_detail_query` (`bill_id`, `detail_date`, `create_time`, `id`)',
  'SELECT ''idx_bill_detail_query already exists'''
);
PREPARE s_add_bill_detail_query_idx FROM @add_bill_detail_query_idx;
EXECUTE s_add_bill_detail_query_idx;
DEALLOCATE PREPARE s_add_bill_detail_query_idx;
