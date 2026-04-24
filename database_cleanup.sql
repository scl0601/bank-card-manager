-- ============================================
-- 数据库冗余清理 SQL 脚本
-- 生成时间: 2026-04-24
-- 数据库: bank_admin
-- 用途: 清理冗余字段和优化数据库结构
-- ============================================

-- ⚠️ 警告: 执行前请务必备份数据库！
-- 备份命令: mysqldump -u root -p bank_admin > backup_20260424.sql

USE bank_admin;

-- ============================================
-- 第一步: 检查冗余字段使用情况
-- ============================================

-- 检查 card_bill.supplier_id 字段使用情况
SELECT
    '检查 card_bill.supplier_id 使用情况' AS check_item,
    COUNT(*) AS total_records,
    COUNT(supplier_id) AS non_null_count,
    COUNT(*) - COUNT(supplier_id) AS null_count
FROM card_bill;

-- 预期结果: non_null_count = 0 (该字段从未使用)

-- ============================================
-- 第二步: 标记废弃字段（推荐方案）
-- ============================================

-- 方案 A: 仅修改注释，标记为废弃（推荐）
-- 优点: 不破坏现有数据，保持向后兼容
-- 缺点: 字段仍占用存储空间

ALTER TABLE card_bill
MODIFY COLUMN supplier_id BIGINT DEFAULT NULL
COMMENT '来源方ID（已废弃 - CardSupplier 模块未实现，保留用于兼容）';

SELECT '✓ 已标记 supplier_id 字段为废弃' AS result;

-- ============================================
-- 第三步: 删除冗余字段（可选，谨慎操作）
-- ============================================

-- 方案 B: 完全删除字段（仅在确认无影响时执行）
-- ⚠️ 警告: 此操作不可逆，请确保已备份数据库

-- 取消下面的注释以执行删除操作
-- ALTER TABLE card_bill DROP COLUMN supplier_id;
-- SELECT '✓ 已删除 supplier_id 字段' AS result;

-- ============================================
-- 第四步: 优化索引（可选）
-- ============================================

-- 检查索引使用情况
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'bank_admin'
  AND TABLE_NAME IN ('card_bill', 'bank_card', 'card_transaction', 'card_user')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 分析索引效率
ANALYZE TABLE card_bill, bank_card, card_transaction, card_user;

SELECT '✓ 索引分析完成' AS result;

-- ============================================
-- 第五步: 清理测试数据（可选）
-- ============================================

-- ⚠️ 警告: 仅在开发环境执行，生产环境禁止执行

-- 检查测试数据数量
SELECT
    'bank_sys_user' AS table_name, COUNT(*) AS record_count FROM bank_sys_user
UNION ALL
SELECT 'card_user', COUNT(*) FROM card_user
UNION ALL
SELECT 'bank_card', COUNT(*) FROM bank_card
UNION ALL
SELECT 'card_transaction', COUNT(*) FROM card_transaction
UNION ALL
SELECT 'card_bill', COUNT(*) FROM card_bill
UNION ALL
SELECT 'reminder_task', COUNT(*) FROM reminder_task
UNION ALL
SELECT 'operation_log', COUNT(*) FROM operation_log;

-- 如需清理测试数据，取消下面的注释（仅开发环境）
-- DELETE FROM operation_log WHERE operator IN ('zhangsan', 'lisi', 'wangwu');
-- DELETE FROM reminder_task WHERE owner_id IN (1, 2, 3, 4);
-- DELETE FROM card_bill WHERE owner_id IN (1, 2, 3, 4);
-- DELETE FROM card_transaction WHERE owner_id IN (1, 2, 3, 4);
-- DELETE FROM bank_card WHERE user_id IN (1, 2, 3, 4);
-- DELETE FROM card_user WHERE id IN (1, 2, 3, 4);
-- DELETE FROM bank_sys_user WHERE username IN ('zhangsan', 'lisi', 'wangwu');
-- SELECT '✓ 测试数据已清理' AS result;

-- ============================================
-- 第六步: 优化表结构
-- ============================================

-- 优化表（回收空间、重建索引）
OPTIMIZE TABLE card_bill;
OPTIMIZE TABLE bank_card;
OPTIMIZE TABLE card_transaction;
OPTIMIZE TABLE card_user;
OPTIMIZE TABLE reminder_task;
OPTIMIZE TABLE operation_log;

SELECT '✓ 表优化完成' AS result;

-- ============================================
-- 第七步: 查看表空间使用情况
-- ============================================

SELECT
    TABLE_NAME AS '表名',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
    TABLE_ROWS AS '行数'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'bank_admin'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ============================================
-- 第八步: 验证数据完整性
-- ============================================

-- 检查外键关系完整性
SELECT
    '检查 bank_card.user_id 外键' AS check_item,
    COUNT(*) AS invalid_count
FROM bank_card bc
LEFT JOIN card_user cu ON bc.user_id = cu.id
WHERE cu.id IS NULL AND bc.is_deleted = 0;

SELECT
    '检查 card_bill.card_id 外键' AS check_item,
    COUNT(*) AS invalid_count
FROM card_bill cb
LEFT JOIN bank_card bc ON cb.card_id = bc.id
WHERE bc.id IS NULL AND cb.is_deleted = 0;

SELECT
    '检查 card_transaction.card_id 外键' AS check_item,
    COUNT(*) AS invalid_count
FROM card_transaction ct
LEFT JOIN bank_card bc ON ct.card_id = bc.id
WHERE bc.id IS NULL AND ct.is_deleted = 0;

-- 预期结果: 所有 invalid_count 应为 0

-- ============================================
-- 清理完成总结
-- ============================================

SELECT
    '========================================' AS summary
UNION ALL
SELECT '数据库清理完成'
UNION ALL
SELECT '========================================'
UNION ALL
SELECT CONCAT('执行时间: ', NOW())
UNION ALL
SELECT '已完成操作:'
UNION ALL
SELECT '  ✓ 标记 supplier_id 字段为废弃'
UNION ALL
SELECT '  ✓ 分析索引使用情况'
UNION ALL
SELECT '  ✓ 优化表结构'
UNION ALL
SELECT '  ✓ 验证数据完整性'
UNION ALL
SELECT ''
UNION ALL
SELECT '建议后续操作:'
UNION ALL
SELECT '  1. 定期执行 OPTIMIZE TABLE'
UNION ALL
SELECT '  2. 监控慢查询日志'
UNION ALL
SELECT '  3. 定期备份数据库';

-- ============================================
-- 附录: 数据库维护命令
-- ============================================

-- 查看数据库大小
-- SELECT
--     TABLE_SCHEMA AS '数据库',
--     ROUND(SUM(DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '大小(MB)'
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = 'bank_admin'
-- GROUP BY TABLE_SCHEMA;

-- 查看慢查询配置
-- SHOW VARIABLES LIKE 'slow_query%';
-- SHOW VARIABLES LIKE 'long_query_time';

-- 查看表状态
-- SHOW TABLE STATUS FROM bank_admin;

-- 分析表
-- ANALYZE TABLE table_name;

-- 检查表
-- CHECK TABLE table_name;

-- 修复表
-- REPAIR TABLE table_name;
