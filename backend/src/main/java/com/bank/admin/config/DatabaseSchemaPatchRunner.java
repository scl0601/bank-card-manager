package com.bank.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 启动时自动补齐关键业务字段，并执行一次性的数据库兼容补丁。
 */
@Component
public class DatabaseSchemaPatchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatchRunner.class);
    private static final String PATCH_HISTORY_TABLE = "schema_patch_history";
    private static final String CARD_USER_MODEL_PATCH_KEY = "20260427_card_user_model";
    private static final String FEE_RATE_PERCENT_PATCH_KEY = "20260419_fee_rate_percent";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensurePatchHistoryTable();
        ensureCardUserModelCompatibility();
        ensureBankCardUserIdColumn();
        ensureBankCardColumns();
        alignBankCardAppFields();
        alignBankCardExpireDateColumn();
        ensureCardBillColumns();
        ensureCardTransactionColumns();
        ensureBillDetailTable();
        migrateLegacyFeeRateData();
        alignFeeRateColumns();
    }

    private void ensurePatchHistoryTable() {
        String ddlSql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                    `patch_key` VARCHAR(100) NOT NULL COMMENT '补丁标识',
                    `patch_desc` VARCHAR(255) DEFAULT NULL COMMENT '补丁说明',
                    `applied_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
                    PRIMARY KEY (`patch_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库补丁执行记录'
                """, PATCH_HISTORY_TABLE);
        jdbcTemplate.execute(Objects.requireNonNull(ddlSql));
    }

    private void ensureCardUserModelCompatibility() {
        if (isPatchApplied(CARD_USER_MODEL_PATCH_KEY)) {
            return;
        }

        ensureCardUserTable();
        ensureCardUserColumns();
        int migratedUsers = migrateLegacyCardOwnerData();
        int syncedBankCards = ensureBankCardUserIdColumn();

        recordPatch(
                CARD_USER_MODEL_PATCH_KEY,
                String.format("旧持卡人模型兼容完成：迁移 card_owner 到 card_user %d 条，回填 bank_card.user_id %d 条", migratedUsers, syncedBankCards)
        );
    }

    private void ensureCardUserTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `card_user` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                    `parent_id` BIGINT DEFAULT NULL COMMENT '父用户ID（NULL=一级用户）',
                    `name` VARCHAR(64) NOT NULL COMMENT '姓名/名称',
                    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
                    `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '手续费率%（如1表示1%）',
                    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
                    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用',
                    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
                    `create_by` VARCHAR(64) DEFAULT NULL,
                    `create_time` DATETIME DEFAULT NULL,
                    `update_by` VARCHAR(64) DEFAULT NULL,
                    `update_time` DATETIME DEFAULT NULL,
                    `_openid` VARCHAR(64) DEFAULT '' NOT NULL,
                    PRIMARY KEY (`id`),
                    KEY `idx_parent_id` (`parent_id`),
                    KEY `idx_status` (`status`),
                    KEY `idx_name` (`name`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（含两级层级关系）'
                """);
    }

    private void ensureCardUserColumns() {
        ensureColumnExists(
                "card_user",
                "parent_id",
                "ALTER TABLE `card_user` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '父用户ID（NULL=一级用户）' AFTER `id`"
        );
        ensureColumnExists(
                "card_user",
                "phone",
                "ALTER TABLE `card_user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话' AFTER `name`"
        );
        ensureColumnExists(
                "card_user",
                "fee_rate",
                "ALTER TABLE `card_user` ADD COLUMN `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '手续费率%（如1表示1%）' AFTER `phone`"
        );
        ensureColumnExists(
                "card_user",
                "remark",
                "ALTER TABLE `card_user` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注' AFTER `fee_rate`"
        );
        ensureColumnExists(
                "card_user",
                "sort_order",
                "ALTER TABLE `card_user` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号' AFTER `remark`"
        );
        ensureColumnExists(
                "card_user",
                "status",
                "ALTER TABLE `card_user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1停用' AFTER `sort_order`"
        );
        ensureColumnExists(
                "card_user",
                "_openid",
                "ALTER TABLE `card_user` ADD COLUMN `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'CloudBase 用户标识'"
        );
    }

    private int migrateLegacyCardOwnerData() {
        if (!tableExists("card_owner") || !tableExists("card_user")) {
            return 0;
        }
        String migratedSql = """
                INSERT INTO `card_user` (
                    `id`, `parent_id`, `name`, `phone`, `fee_rate`, `remark`, `sort_order`, `status`,
                    `is_deleted`, `create_by`, `create_time`, `update_by`, `update_time`, `_openid`
                )
                SELECT
                    co.`id`,
                    NULL,
                    %s,
                    %s,
                    0.00,
                    %s,
                    0,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s
                FROM `card_owner` co
                LEFT JOIN `card_user` cu ON cu.`id` = co.`id`
                WHERE cu.`id` IS NULL
                """.formatted(
                sourceColumnOrDefault("card_owner", "name", "''"),
                sourceColumnOrDefault("card_owner", "phone", "NULL"),
                sourceColumnOrDefault("card_owner", "remark", "NULL"),
                sourceColumnOrDefault("card_owner", "status", "0", true),
                sourceColumnOrDefault("card_owner", "is_deleted", "0", true),
                sourceColumnOrDefault("card_owner", "create_by", "NULL"),
                sourceColumnOrDefault("card_owner", "create_time", "NULL"),
                sourceColumnOrDefault("card_owner", "update_by", "NULL"),
                sourceColumnOrDefault("card_owner", "update_time", "NULL"),
                sourceColumnOrDefault("card_owner", "_openid", "''", false, true)
        );
        int migrated = jdbcTemplate.update(migratedSql);
        if (migrated > 0) {
            log.info("已将旧持卡人表 card_owner 迁移到 card_user: {} 条", migrated);
        }
        return migrated;
    }

    private int ensureBankCardUserIdColumn() {
        if (!tableExists("bank_card")) {
            return 0;
        }

        if (!columnExists("bank_card", "user_id")) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（关联card_user）' AFTER `id`");
            log.info("已自动补齐数据库字段: bank_card.user_id");
        }

        int updated = 0;
        if (columnExists("bank_card", "owner_id") && columnExists("bank_card", "user_id")) {
            updated = jdbcTemplate.update(
                    "UPDATE `bank_card` SET `user_id` = `owner_id` WHERE (`user_id` IS NULL OR `user_id` = 0) AND `owner_id` IS NOT NULL"
            );
            if (updated > 0) {
                log.info("已回填 bank_card.user_id 历史数据 {} 条", updated);
            }
        }

        if (!indexExists("bank_card", "idx_user_id")) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` ADD INDEX `idx_user_id` (`user_id`)");
            log.info("已自动补齐数据库索引: bank_card.idx_user_id");
        }
        return updated;
    }

    private void ensureBankCardColumns() {
        ensureColumnExists(
                "bank_card",
                "repay_method",
                "ALTER TABLE `bank_card` ADD COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT 'APP：cloudpay云闪付 wechat微信 alipay支付宝 bankapp银行APP none无 other其他' AFTER `status`"
        );
        ensureColumnExists(
                "bank_card",
                "balance",
                "ALTER TABLE `bank_card` ADD COLUMN `balance` DECIMAL(18,2) DEFAULT 0.00 COMMENT '当前余额（流水模块维护）' AFTER `credit_limit`"
        );
        // 修复旧版 owner_id（NOT NULL 无默认值）导致 INSERT 失败的问题
        fixBankCardOwnerIdDefault();
    }

    private void alignBankCardAppFields() {
        if (!tableExists("bank_card")) {
            return;
        }
        if (columnExists("bank_card", "total_limit") && columnExists("bank_card", "credit_limit")) {
            jdbcTemplate.update("""
                    UPDATE `bank_card`
                    SET `credit_limit` = `total_limit`
                    WHERE `credit_limit` IS NULL AND `total_limit` IS NOT NULL
                    """);
        }
        if (columnExists("bank_card", "repay_method")) {
            jdbcTemplate.update("UPDATE `bank_card` SET `repay_method` = 'other' WHERE `repay_method` = 'invoice'");
            jdbcTemplate.update("UPDATE `bank_card` SET `repay_method` = 'cloudpay' WHERE `repay_method` IS NULL OR `repay_method` = ''");
            jdbcTemplate.update("""
                    UPDATE `bank_card`
                    SET `repay_method` = 'other'
                    WHERE `repay_method` NOT IN ('cloudpay', 'wechat', 'alipay', 'bankapp', 'none', 'other')
                    """);
            jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT 'APP：cloudpay云闪付 wechat微信 alipay支付宝 bankapp银行APP none无 other其他' AFTER `status`");
        }
        if (columnExists("bank_card", "remark")) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `remark` VARCHAR(500) DEFAULT NULL AFTER `repay_method`");
        }
        dropColumnIfExists("bank_card", "verified");
        dropColumnIfExists("bank_card", "owner_name");
        dropColumnIfExists("bank_card", "owner_relation");
        dropColumnIfExists("bank_card", "total_limit");
        dropColumnIfExists("bank_card", "card_no");
        dropColumnIfExists("bank_card", "used_amount");
        dropColumnIfExists("bank_card", "owner_id");
    }

    private void fixBankCardOwnerIdDefault() {
        if (!columnExists("bank_card", "owner_id")) {
            return;
        }
        try {
            String columnDefault = jdbcTemplate.queryForObject(
                    "SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'owner_id'",
                    String.class
            );
            if (columnDefault == null) {
                jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `owner_id` BIGINT NOT NULL DEFAULT 0 COMMENT '旧版归属人ID（已弃用，兼容保留）'");
                log.info("已修复 bank_card.owner_id 默认值（原为 NOT NULL 无默认值，已改为 DEFAULT 0）");
            }
        } catch (Exception e) {
            log.warn("检查 bank_card.owner_id 默认值时出错，忽略: {}", e.getMessage());
        }
    }

    private void ensureCardBillColumns() {
        ensureColumnExists(
                "card_bill",
                "supplier_id",
                "ALTER TABLE `card_bill` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '来源方ID（冗余，用于聚合查询）' AFTER `owner_id`"
        );
        ensureColumnExists(
                "card_bill",
                "bill_day",
                "ALTER TABLE `card_bill` ADD COLUMN `bill_day` TINYINT DEFAULT NULL COMMENT '账单日' AFTER `bill_month`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_rate",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT '约定费率%（如1表示1%）' AFTER `remark`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '本期应收手续费' AFTER `fee_rate`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_paid",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_paid` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '手续费是否已支付' AFTER `fee_amount`"
        );
        ensureColumnExists(
                "card_bill",
                "verified",
                "ALTER TABLE `card_bill` ADD COLUMN `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当月账单是否已核实' AFTER `fee_paid`"
        );
        ensureColumnExists(
                "card_bill",
                "expense_verified",
                "ALTER TABLE `card_bill` ADD COLUMN `expense_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本月支出明细是否已核实' AFTER `verified`"
        );
        ensureColumnExists(
                "card_bill",
                "pos_cost_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `pos_cost_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'POS机使用成本' AFTER `expense_verified`"
        );
        ensureColumnExists(
                "card_bill",
                "net_profit",
                "ALTER TABLE `card_bill` ADD COLUMN `net_profit` DECIMAL(18,2) DEFAULT 0.00 COMMENT '本期净利润' AFTER `pos_cost_amount`"
        );
    }

    private void alignBankCardExpireDateColumn() {
        if (!columnExists("bank_card", "expire_date")) {
            return;
        }
        Integer maxLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND COLUMN_NAME = 'expire_date'",
                Integer.class
        );
        if (maxLength != null && maxLength < 32) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `expire_date` VARCHAR(32) DEFAULT NULL COMMENT '有效期（原样保存用户输入）'");
            log.info("已放宽银行卡有效期字段长度: bank_card.expire_date");
        }
    }

    private void migrateLegacyFeeRateData() {
        if (isPatchApplied(FEE_RATE_PERCENT_PATCH_KEY)) {
            return;
        }

        boolean cardUserLegacySchema = isLegacyFeeRateSchema("card_user");
        boolean cardBillLegacySchema = isLegacyFeeRateSchema("card_bill");
        boolean cardUserLegacyData = hasLegacyLikeFeeRateData("card_user");
        boolean cardBillLegacyData = hasLegacyLikeFeeRateData("card_bill");

        int migratedUserRows = migrateFeeRateTable("card_user", cardUserLegacySchema, cardUserLegacyData);
        int migratedBillRows = migrateFeeRateTable("card_bill", cardBillLegacySchema, cardBillLegacyData);

        if (migratedBillRows > 0) {
            int refreshedRows = jdbcTemplate.update(
                    "UPDATE `card_bill` " +
                            "SET `fee_amount` = ROUND(IFNULL(`bill_amount`, 0) * IFNULL(`fee_rate`, 0) / 100, 2), " +
                            "    `net_profit` = ROUND(ROUND(IFNULL(`bill_amount`, 0) * IFNULL(`fee_rate`, 0) / 100, 2) - IFNULL(`pos_cost_amount`, 0), 2) " +
                            "WHERE `is_deleted` = 0"
            );
            log.info("手续费率口径迁移后，已重算 card_bill 收益字段 {} 条", refreshedRows);
        }

        recordPatch(
                FEE_RATE_PERCENT_PATCH_KEY,
                (migratedUserRows > 0 || migratedBillRows > 0)
                        ? "手续费率已从小数口径迁移为百分数口径"
                        : "手续费率百分数口径已确认，无需迁移历史数据"
        );
    }

    private int migrateFeeRateTable(String tableName, boolean legacySchema, boolean legacyData) {
        if (!columnExists(tableName, "fee_rate") || (!legacySchema && !legacyData)) {
            return 0;
        }
        String condition = legacySchema
                ? "`fee_rate` > 0 AND `fee_rate` <= 1"
                : "`fee_rate` > 0 AND `fee_rate` < 0.1";
        int updated = jdbcTemplate.update(
                "UPDATE `" + tableName + "` SET `fee_rate` = ROUND(`fee_rate` * 100, 6) WHERE " + condition
        );
        log.info("已迁移 {} 表手续费率历史数据 {} 条", tableName, updated);
        return updated;
    }

    private boolean isLegacyFeeRateSchema(String tableName) {
        try {
            Integer numericScale = jdbcTemplate.queryForObject(
                    "SELECT NUMERIC_SCALE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'fee_rate'",
                    Integer.class,
                    tableName
            );
            return numericScale != null && numericScale > 2;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private boolean hasLegacyLikeFeeRateData(String tableName) {
        if (!columnExists(tableName, "fee_rate")) {
            return false;
        }
        String sql = columnExists(tableName, "is_deleted")
                ? "SELECT COUNT(*) FROM `" + tableName + "` WHERE `is_deleted` = 0 AND `fee_rate` > 0 AND `fee_rate` < 0.1"
                : "SELECT COUNT(*) FROM `" + tableName + "` WHERE `fee_rate` > 0 AND `fee_rate` < 0.1";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

    private void ensureCardTransactionColumns() {
        ensureColumnExists(
                "card_transaction",
                "supplier_id",
                "ALTER TABLE `card_transaction` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '来源方ID（冗余，用于聚合查询）' AFTER `owner_id`"
        );
    }

    private void ensureBillDetailTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `bill_detail` (
                    `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
                    `bill_id`     BIGINT          NOT NULL                COMMENT '关联账单ID',
                    `detail_date` DATE            NOT NULL                COMMENT '明细日期',
                    `description` VARCHAR(200)    NOT NULL DEFAULT ''     COMMENT '描述',
                    `amount`      DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '金额（正=收入 负=支出）',
                    `detail_type` TINYINT         NOT NULL DEFAULT 0      COMMENT '0=POS刷出(支出) 1=还信用卡(支出) 2=客户还钱给我(收入)',
                    `remark`      VARCHAR(500)    DEFAULT NULL,
                    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0,
                    `create_by`   VARCHAR(64)     DEFAULT NULL,
                    `create_time` DATETIME        DEFAULT NULL,
                    `update_by`   VARCHAR(64)     DEFAULT NULL,
                    `update_time` DATETIME        DEFAULT NULL,
                    PRIMARY KEY (`id`),
                    KEY `idx_bill_id`     (`bill_id`),
                    KEY `idx_detail_date` (`detail_date`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细表'
                """);
    }

    private void alignFeeRateColumns() {
        if (columnExists("card_user", "fee_rate")) {
            jdbcTemplate.execute("ALTER TABLE `card_user` MODIFY COLUMN `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '手续费率%（如1表示1%）'");
        }
        if (columnExists("card_bill", "fee_rate")) {
            jdbcTemplate.execute("ALTER TABLE `card_bill` MODIFY COLUMN `fee_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT '约定费率%（如1表示1%）'");
        }
    }

    private boolean isPatchApplied(String patchKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + PATCH_HISTORY_TABLE + "` WHERE `patch_key` = ?",
                Integer.class,
                patchKey
        );
        return count != null && count > 0;
    }

    private void recordPatch(String patchKey, String patchDesc) {
        jdbcTemplate.update(
                "INSERT INTO `" + PATCH_HISTORY_TABLE + "` (`patch_key`, `patch_desc`) VALUES (?, ?)",
                patchKey,
                patchDesc
        );
        log.info("已记录数据库补丁: {}", patchKey);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private String sourceColumnOrDefault(String tableName, String columnName, String defaultExpression) {
        return sourceColumnOrDefault(tableName, columnName, defaultExpression, false, false);
    }

    private String sourceColumnOrDefault(String tableName, String columnName, String defaultExpression, boolean wrapIfNull) {
        return sourceColumnOrDefault(tableName, columnName, defaultExpression, wrapIfNull, false);
    }

    private String sourceColumnOrDefault(
            String tableName,
            String columnName,
            String defaultExpression,
            boolean wrapIfNull,
            boolean wrapCoalesce
    ) {
        if (!columnExists(tableName, columnName)) {
            return defaultExpression;
        }
        String sourceExpression = "co.`" + columnName + "`";
        if (wrapIfNull) {
            return "IFNULL(" + sourceExpression + ", " + defaultExpression + ")";
        }
        if (wrapCoalesce) {
            return "COALESCE(" + sourceExpression + ", " + defaultExpression + ")";
        }
        return sourceExpression;
    }

    private void ensureColumnExists(String tableName, String columnName, String ddlSql) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(Objects.requireNonNull(ddlSql));
        log.info("已自动补齐数据库字段: {}.{}", tableName, columnName);
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`");
        log.info("已删除废弃数据库字段: {}.{}", tableName, columnName);
    }
}
