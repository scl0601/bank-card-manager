package com.bank.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动补齐关键业务字段，并执行一次性的数据库兼容补丁。
 */
@Component
public class DatabaseSchemaPatchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatchRunner.class);
    private static final String PATCH_HISTORY_TABLE = "schema_patch_history";
    private static final String FEE_RATE_PERCENT_PATCH_KEY = "20260419_fee_rate_percent";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaPatchRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensurePatchHistoryTable();
        ensureBankCardColumns();
        ensureCardBillColumns();
        migrateLegacyFeeRateData();
        alignFeeRateColumns();
    }

    private void ensurePatchHistoryTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `schema_patch_history` (
                    `patch_key` VARCHAR(100) NOT NULL COMMENT '补丁标识',
                    `patch_desc` VARCHAR(255) DEFAULT NULL COMMENT '补丁说明',
                    `applied_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
                    PRIMARY KEY (`patch_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库补丁执行记录'
                """);
    }

    private void ensureBankCardColumns() {
        ensureColumnExists(
                "bank_card",
                "owner_relation",
                "ALTER TABLE `bank_card` ADD COLUMN `owner_relation` VARCHAR(32) DEFAULT '本人' COMMENT '卡片归属人关系：本人/配偶/子女等' AFTER `card_no_last4`"
        );
        ensureColumnExists(
                "bank_card",
                "owner_name",
                "ALTER TABLE `bank_card` ADD COLUMN `owner_name` VARCHAR(64) DEFAULT NULL COMMENT '卡片归属人姓名（可选）' AFTER `owner_relation`"
        );
        ensureColumnExists(
                "bank_card",
                "total_limit",
                "ALTER TABLE `bank_card` ADD COLUMN `total_limit` DECIMAL(18,2) DEFAULT NULL COMMENT '总额度（借记卡用）' AFTER `balance`"
        );
        ensureColumnExists(
                "bank_card",
                "repay_method",
                "ALTER TABLE `bank_card` ADD COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT '还款方式：cloudpay云闪付 invoice开票' AFTER `remark`"
        );
        ensureColumnExists(
                "bank_card",
                "verified",
                "ALTER TABLE `bank_card` ADD COLUMN `verified` TINYINT(1) DEFAULT NULL COMMENT '是否核实（云闪付时有效）' AFTER `repay_method`"
        );
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
                "pos_cost_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `pos_cost_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'POS机使用成本' AFTER `fee_amount`"
        );
        ensureColumnExists(
                "card_bill",
                "net_profit",
                "ALTER TABLE `card_bill` ADD COLUMN `net_profit` DECIMAL(18,2) DEFAULT 0.00 COMMENT '本期净利润' AFTER `pos_cost_amount`"
        );
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
        Integer numericScale = jdbcTemplate.queryForObject(
                "SELECT NUMERIC_SCALE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'fee_rate'",
                Integer.class,
                tableName
        );
        return numericScale != null && numericScale > 2;
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
                "SELECT COUNT(*) FROM `schema_patch_history` WHERE `patch_key` = ?",
                Integer.class,
                patchKey
        );
        return count != null && count > 0;
    }

    private void recordPatch(String patchKey, String patchDesc) {
        jdbcTemplate.update(
                "INSERT INTO `schema_patch_history` (`patch_key`, `patch_desc`) VALUES (?, ?)",
                patchKey,
                patchDesc
        );
        log.info("已记录数据库补丁: {}", patchKey);
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

    private void ensureColumnExists(String tableName, String columnName, String ddlSql) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(ddlSql);
        log.info("已自动补齐数据库字段: {}.{}", tableName, columnName);
    }
}
