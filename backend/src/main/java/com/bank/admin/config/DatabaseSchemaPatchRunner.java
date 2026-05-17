package com.bank.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DatabaseSchemaPatchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatchRunner.class);

    private static final String PATCH_HISTORY_TABLE = "schema_patch_history";
    private static final String CARD_USER_MODEL_PATCH_KEY = "20260427_card_user_model";
    private static final String FEE_RATE_PERCENT_PATCH_KEY = "20260419_fee_rate_percent";
    private static final String TEST_ACCOUNT_PASSWORD_HASH = "$2a$10$gX5wW4SAPR.eeXW1.c5x8eRaQIzrNHyHYat2Axq6IfH20oIePAzHS";
    private static final String MONITOR_ACCOUNT_PASSWORD_HASH = "$2a$10$YqSoxXaVEaZLntYw0OB7Rebe9XV/qvs7QeXhur/dVzJovBzyuter.";

    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;

    public DatabaseSchemaPatchRunner(JdbcTemplate jdbcTemplate, AppProperties appProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!appProperties.getSchemaPatch().isEnabled()) {
            log.info("Database schema patch runner is disabled by app.schema-patch.enabled=false");
            return;
        }

        ensurePatchHistoryTable();
        ensureSysUserOpenidColumn();
        ensureSysUserDataScope();
        if (appProperties.getBootstrapUsers().isEnabled()) {
            ensureFunctionalTestAccount();
            ensureMonitorAccount();
        } else {
            log.info("Bootstrap users are disabled by app.bootstrap-users.enabled=false");
        }
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
        ensureSpecialChannelTables();
        ensureCommonOpenidColumns();
    }

    private void ensurePatchHistoryTable() {
        String ddlSql = String.format(sql(
                "CREATE TABLE IF NOT EXISTS `%s` (",
                "    `patch_key` VARCHAR(100) NOT NULL COMMENT 'patch key',",
                "    `patch_desc` VARCHAR(255) DEFAULT NULL COMMENT 'patch description',",
                "    `applied_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'applied time',",
                "    PRIMARY KEY (`patch_key`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='schema patch history'"
        ), PATCH_HISTORY_TABLE);
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
                String.format(
                        "Migrate legacy card_owner to card_user: %d rows, sync bank_card.user_id: %d rows",
                        migratedUsers,
                        syncedBankCards
                )
        );
    }

    private void ensureSysUserOpenidColumn() {
        ensureOpenidColumnIfTableExists("bank_sys_user");
    }

    private void ensureSysUserDataScope() {
        if (!tableExists("bank_sys_user")) {
            return;
        }
        ensureColumnExists(
                "bank_sys_user",
                "data_scope",
                "ALTER TABLE `bank_sys_user` ADD COLUMN `data_scope` VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT 'ALL all data, SELF own data only' AFTER `role`"
        );
        jdbcTemplate.update("UPDATE `bank_sys_user` SET `data_scope` = 'ALL' WHERE `data_scope` IS NULL OR `data_scope` = ''");
    }

    private void ensureFunctionalTestAccount() {
        if (!tableExists("bank_sys_user")) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `bank_sys_user` WHERE `username` = 'test'",
                Integer.class
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(sql(
                    "UPDATE `bank_sys_user`",
                    "SET `password` = ?,",
                    "    `nickname` = '功能测试账号',",
                    "    `role` = 'ADMIN',",
                    "    `data_scope` = 'SELF',",
                    "    `status` = 0,",
                    "    `is_deleted` = 0,",
                    "    `update_time` = NOW()",
                    "WHERE `username` = 'test'"
            ), TEST_ACCOUNT_PASSWORD_HASH);
            return;
        }

        jdbcTemplate.update(sql(
                "INSERT INTO `bank_sys_user`",
                "(`username`, `password`, `nickname`, `role`, `data_scope`, `status`, `is_deleted`, `create_by`, `create_time`, `update_time`, `_openid`)",
                "VALUES ('test', ?, '功能测试账号', 'ADMIN', 'SELF', 0, 0, 'system', NOW(), NOW(), '')"
        ), TEST_ACCOUNT_PASSWORD_HASH);
        log.info("Created functional test account: test");
    }

    private void ensureMonitorAccount() {
        if (!tableExists("bank_sys_user")) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `bank_sys_user` WHERE `username` = 'monitor'",
                Integer.class
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(sql(
                    "UPDATE `bank_sys_user`",
                    "SET `role` = 'MONITOR',",
                    "    `data_scope` = 'ALL',",
                    "    `status` = 0,",
                    "    `is_deleted` = 0,",
                    "    `update_time` = NOW()",
                    "WHERE `username` = 'monitor'"
            ));
            return;
        }

        jdbcTemplate.update(sql(
                "INSERT INTO `bank_sys_user`",
                "(`username`, `password`, `nickname`, `role`, `data_scope`, `status`, `is_deleted`, `create_by`, `create_time`, `update_time`, `_openid`)",
                "VALUES ('monitor', ?, '监控账号', 'MONITOR', 'ALL', 0, 0, 'system', NOW(), NOW(), '')"
        ), MONITOR_ACCOUNT_PASSWORD_HASH);
        log.info("Created monitor account: monitor");
    }

    private void ensureCardUserTable() {
        jdbcTemplate.execute(sql(
                "CREATE TABLE IF NOT EXISTS `card_user` (",
                "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',",
                "    `parent_id` BIGINT DEFAULT NULL COMMENT 'parent user id',",
                "    `name` VARCHAR(64) NOT NULL COMMENT 'user name',",
                "    `phone` VARCHAR(20) DEFAULT NULL COMMENT 'phone',",
                "    `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT 'fee rate percent (1 means 1 percent)',",
                "    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark',",
                "    `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'sort order',",
                "    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 disabled',",
                "    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,",
                "    `create_by` VARCHAR(64) DEFAULT NULL,",
                "    `create_time` DATETIME DEFAULT NULL,",
                "    `update_by` VARCHAR(64) DEFAULT NULL,",
                "    `update_time` DATETIME DEFAULT NULL,",
                "    `_openid` VARCHAR(64) DEFAULT '' NOT NULL,",
                "    PRIMARY KEY (`id`),",
                "    KEY `idx_parent_id` (`parent_id`),",
                "    KEY `idx_status` (`status`),",
                "    KEY `idx_name` (`name`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='card users'"
        ));
    }

    private void ensureCardUserColumns() {
        ensureColumnExists(
                "card_user",
                "parent_id",
                "ALTER TABLE `card_user` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT 'parent user id' AFTER `id`"
        );
        ensureColumnExists(
                "card_user",
                "phone",
                "ALTER TABLE `card_user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT 'phone' AFTER `name`"
        );
        ensureColumnExists(
                "card_user",
                "fee_rate",
                "ALTER TABLE `card_user` ADD COLUMN `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT 'fee rate percent (1 means 1 percent)' AFTER `phone`"
        );
        ensureColumnExists(
                "card_user",
                "remark",
                "ALTER TABLE `card_user` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark' AFTER `fee_rate`"
        );
        ensureColumnExists(
                "card_user",
                "sort_order",
                "ALTER TABLE `card_user` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'sort order' AFTER `remark`"
        );
        ensureColumnExists(
                "card_user",
                "status",
                "ALTER TABLE `card_user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 disabled' AFTER `sort_order`"
        );
        ensureColumnExists(
                "card_user",
                "_openid",
                "ALTER TABLE `card_user` ADD COLUMN `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'cloudbase openid'"
        );
    }

    private int migrateLegacyCardOwnerData() {
        if (!tableExists("card_owner") || !tableExists("card_user")) {
            return 0;
        }

        String migratedSql = String.format(sql(
                "INSERT INTO `card_user` (",
                "    `id`, `parent_id`, `name`, `phone`, `fee_rate`, `remark`, `sort_order`, `status`,",
                "    `is_deleted`, `create_by`, `create_time`, `update_by`, `update_time`, `_openid`",
                ")",
                "SELECT",
                "    co.`id`,",
                "    NULL,",
                "    %s,",
                "    %s,",
                "    0.00,",
                "    %s,",
                "    0,",
                "    %s,",
                "    %s,",
                "    %s,",
                "    %s,",
                "    %s,",
                "    %s,",
                "    %s",
                "FROM `card_owner` co",
                "LEFT JOIN `card_user` cu ON cu.`id` = co.`id`",
                "WHERE cu.`id` IS NULL"
        ),
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
            log.info("Migrated legacy card_owner to card_user: {} rows", migrated);
        }
        return migrated;
    }

    private int ensureBankCardUserIdColumn() {
        if (!tableExists("bank_card")) {
            return 0;
        }

        if (!columnExists("bank_card", "user_id")) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT 'user id reference to card_user' AFTER `id`");
            log.info("Added column bank_card.user_id");
        }

        int updated = 0;
        if (columnExists("bank_card", "owner_id") && columnExists("bank_card", "user_id")) {
            updated = jdbcTemplate.update(
                    "UPDATE `bank_card` SET `user_id` = `owner_id` WHERE (`user_id` IS NULL OR `user_id` = 0) AND `owner_id` IS NOT NULL"
            );
            if (updated > 0) {
                log.info("Backfilled historical bank_card.user_id data: {} rows", updated);
            }
        }

        if (!indexExists("bank_card", "idx_user_id")) {
            jdbcTemplate.execute("ALTER TABLE `bank_card` ADD INDEX `idx_user_id` (`user_id`)");
            log.info("Added index bank_card.idx_user_id");
        }
        return updated;
    }

    private void ensureBankCardColumns() {
        ensureColumnExists(
                "bank_card",
                "repay_method",
                "ALTER TABLE `bank_card` ADD COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT 'repay method' AFTER `status`"
        );
        ensureColumnExists(
                "bank_card",
                "balance",
                "ALTER TABLE `bank_card` ADD COLUMN `balance` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'current balance' AFTER `credit_limit`"
        );

        fixBankCardOwnerIdDefault();
    }

    private void alignBankCardAppFields() {
        if (!tableExists("bank_card")) {
            return;
        }
        if (columnExists("bank_card", "total_limit") && columnExists("bank_card", "credit_limit")) {
            jdbcTemplate.update(sql(
                    "UPDATE `bank_card`",
                    "SET `credit_limit` = `total_limit`",
                    "WHERE `credit_limit` IS NULL AND `total_limit` IS NOT NULL"
            ));
        }
        if (columnExists("bank_card", "repay_method")) {
            jdbcTemplate.update("UPDATE `bank_card` SET `repay_method` = 'other' WHERE `repay_method` = 'invoice'");
            jdbcTemplate.update("UPDATE `bank_card` SET `repay_method` = 'cloudpay' WHERE `repay_method` IS NULL OR `repay_method` = ''");
            jdbcTemplate.update(sql(
                    "UPDATE `bank_card`",
                    "SET `repay_method` = 'other'",
                    "WHERE `repay_method` NOT IN ('cloudpay', 'wechat', 'alipay', 'bankapp', 'none', 'other')"
            ));
            jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `repay_method` VARCHAR(20) DEFAULT 'cloudpay' COMMENT 'repay method' AFTER `status`");
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
                jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `owner_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'legacy owner id kept for compatibility'");
                log.info("Fixed default value for bank_card.owner_id to DEFAULT 0");
            }
        } catch (Exception e) {
            log.warn("Failed to inspect default value of bank_card.owner_id: {}", e.getMessage());
        }
    }

    private void ensureCardBillColumns() {
        ensureColumnExists(
                "card_bill",
                "supplier_id",
                "ALTER TABLE `card_bill` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT 'source id reserved for merged queries' AFTER `owner_id`"
        );
        ensureColumnExists(
                "card_bill",
                "bill_day",
                "ALTER TABLE `card_bill` ADD COLUMN `bill_day` TINYINT DEFAULT NULL COMMENT 'bill day' AFTER `bill_month`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_rate",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT 'fee rate percent (1 means 1 percent)' AFTER `remark`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'current fee amount' AFTER `fee_rate`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_paid",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_paid` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'fee paid flag' AFTER `fee_amount`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_paid_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_paid_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'fee paid amount' AFTER `fee_paid`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_pay_time",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_pay_time` DATETIME DEFAULT NULL COMMENT 'latest fee payment time' AFTER `fee_paid_amount`"
        );
        ensureColumnExists(
                "card_bill",
                "fee_pay_method",
                "ALTER TABLE `card_bill` ADD COLUMN `fee_pay_method` VARCHAR(20) DEFAULT NULL COMMENT 'fee payment method: wechat/alipay/cash/other' AFTER `fee_pay_time`"
        );
        jdbcTemplate.update(sql(
                "UPDATE `card_bill`",
                "SET `fee_paid_amount` = IFNULL(`fee_amount`, 0)",
                "WHERE `fee_paid` = 1",
                "  AND IFNULL(`fee_paid_amount`, 0) = 0"
        ));
        ensureColumnExists(
                "card_bill",
                "verified",
                "ALTER TABLE `card_bill` ADD COLUMN `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'current bill verified' AFTER `fee_paid`"
        );
        ensureColumnExists(
                "card_bill",
                "expense_verified",
                "ALTER TABLE `card_bill` ADD COLUMN `expense_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'current expense details verified' AFTER `verified`"
        );
        ensureColumnExists(
                "card_bill",
                "pos_cost_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `pos_cost_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'pos machine cost amount' AFTER `expense_verified`"
        );
        ensureColumnExists(
                "card_bill",
                "other_fee_amount",
                "ALTER TABLE `card_bill` ADD COLUMN `other_fee_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'other fee amount' AFTER `pos_cost_amount`"
        );
        ensureColumnExists(
                "card_bill",
                "net_profit",
                "ALTER TABLE `card_bill` ADD COLUMN `net_profit` DECIMAL(18,2) DEFAULT 0.00 COMMENT 'net profit' AFTER `other_fee_amount`"
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
            jdbcTemplate.execute("ALTER TABLE `bank_card` MODIFY COLUMN `expire_date` VARCHAR(32) DEFAULT NULL COMMENT 'expire date text as entered by user'");
            log.info("Expanded column length of bank_card.expire_date");
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
                            "    `net_profit` = ROUND(ROUND(IFNULL(`bill_amount`, 0) * IFNULL(`fee_rate`, 0) / 100, 2) - IFNULL(`pos_cost_amount`, 0) - IFNULL(`other_fee_amount`, 0), 2) " +
                            "WHERE `is_deleted` = 0"
            );
            log.info("Recalculated card_bill profit fields after fee rate migration: {} rows", refreshedRows);
        }

        recordPatch(
                FEE_RATE_PERCENT_PATCH_KEY,
                (migratedUserRows > 0 || migratedBillRows > 0)
                        ? "Migrated fee_rate from decimal ratio to percent"
                        : "Fee rate format already confirmed"
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
        log.info("Migrated legacy fee_rate data for table {}: {} rows", tableName, updated);
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
                "ALTER TABLE `card_transaction` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT 'source id reserved for merged queries' AFTER `owner_id`"
        );
    }

    private void ensureBillDetailTable() {
        jdbcTemplate.execute(sql(
                "CREATE TABLE IF NOT EXISTS `bill_detail` (",
                "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',",
                "    `bill_id` BIGINT NOT NULL COMMENT 'related bill id',",
                "    `detail_date` DATE NOT NULL COMMENT 'detail date',",
                "    `description` VARCHAR(200) NOT NULL DEFAULT '' COMMENT 'description',",
                "    `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'amount (+income, -expense)',",
                "    `detail_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0 pos expense, 1 card repayment, 2 customer repayment',",
                "    `remark` VARCHAR(500) DEFAULT NULL,",
                "    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,",
                "    `create_by` VARCHAR(64) DEFAULT NULL,",
                "    `create_time` DATETIME DEFAULT NULL,",
                "    `update_by` VARCHAR(64) DEFAULT NULL,",
                "    `update_time` DATETIME DEFAULT NULL,",
                "    PRIMARY KEY (`id`),",
                "    KEY `idx_bill_id` (`bill_id`),",
                "    KEY `idx_detail_date` (`detail_date`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bill detail'"
        ));
    }

    private void alignFeeRateColumns() {
        if (columnExists("card_user", "fee_rate")) {
            jdbcTemplate.execute("ALTER TABLE `card_user` MODIFY COLUMN `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT 'fee rate percent (1 means 1 percent)'");
        }
        if (columnExists("card_bill", "fee_rate")) {
            jdbcTemplate.execute("ALTER TABLE `card_bill` MODIFY COLUMN `fee_rate` DECIMAL(8,2) DEFAULT 0.00 COMMENT 'fee rate percent (1 means 1 percent)'");
        }
    }

    private void ensureSpecialChannelTables() {
        jdbcTemplate.execute(sql(
                "CREATE TABLE IF NOT EXISTS `special_user_config` (",
                "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',",
                "    `user_id` BIGINT NOT NULL COMMENT 'card_user id for special channel',",
                "    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 disabled',",
                "    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark',",
                "    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,",
                "    `create_by` VARCHAR(64) DEFAULT NULL,",
                "    `create_time` DATETIME DEFAULT NULL,",
                "    `update_by` VARCHAR(64) DEFAULT NULL,",
                "    `update_time` DATETIME DEFAULT NULL,",
                "    `_openid` VARCHAR(64) NOT NULL DEFAULT '',",
                "    PRIMARY KEY (`id`),",
                "    KEY `idx_user_id` (`user_id`),",
                "    KEY `idx_status` (`status`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='special channel user config'"
        ));

        jdbcTemplate.execute(sql(
                "CREATE TABLE IF NOT EXISTS `special_bank_card` (",
                "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',",
                "    `user_id` BIGINT NOT NULL COMMENT 'card_user id',",
                "    `bank_name` VARCHAR(64) NOT NULL COMMENT 'bank name',",
                "    `card_no_last4` CHAR(4) NOT NULL COMMENT 'card last 4 digits',",
                "    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'card total limit amount',",
                "    `expire_date` VARCHAR(32) DEFAULT NULL COMMENT 'expire date text as entered by user',",
                "    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active, 1 disabled',",
                "    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark',",
                "    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,",
                "    `create_by` VARCHAR(64) DEFAULT NULL,",
                "    `create_time` DATETIME DEFAULT NULL,",
                "    `update_by` VARCHAR(64) DEFAULT NULL,",
                "    `update_time` DATETIME DEFAULT NULL,",
                "    `_openid` VARCHAR(64) NOT NULL DEFAULT '',",
                "    PRIMARY KEY (`id`),",
                "    KEY `idx_user_id` (`user_id`),",
                "    KEY `idx_status` (`status`),",
                "    KEY `idx_bank_card` (`bank_name`, `card_no_last4`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='special channel bank cards'"
        ));

        jdbcTemplate.execute(sql(
                "CREATE TABLE IF NOT EXISTS `special_card_bill` (",
                "    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',",
                "    `card_id` BIGINT NOT NULL COMMENT 'special_bank_card id',",
                "    `user_id` BIGINT NOT NULL COMMENT 'card_user id snapshot',",
                "    `bill_month` VARCHAR(7) NOT NULL COMMENT 'yyyy-MM',",
                "    `bill_year` INT NOT NULL COMMENT 'bill year',",
                "    `bill_month_no` TINYINT NOT NULL COMMENT 'bill month number',",
                "    `monthly_total_bill_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'monthly total bill amount to repay',",
                "    `bill_day` TINYINT DEFAULT NULL COMMENT 'bill day',",
                "    `repayment_day` TINYINT DEFAULT NULL COMMENT 'repayment day',",
                "    `bill_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'bill amount',",
                "    `bill_amount_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'bill amount verified',",
                "    `xiaohuan_repay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'xiaohuan repayment amount',",
                "    `xiaohuan_repay_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'xiaohuan repayment verified',",
                "    `customer_repay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer repayment amount',",
                "    `customer_repay_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer repayment verified',",
                "    `xiaohuan_consume_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'xiaohuan consume amount',",
                "    `xiaohuan_consume_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'xiaohuan consume verified',",
                "    `customer_need_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer requested amount',",
                "    `customer_need_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer requested amount verified',",
                "    `customer_consume_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer consume amount',",
                "    `customer_consume_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer consume verified',",
                "    `diff_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'repay minus consume',",
                "    `balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'manual monthly balance',",
                "    `fee_rate` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT 'fee rate percent snapshot',",
                "    `repayment_fee` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'bill_amount * fee_rate',",
                "    `consume_fee` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'consume total amount * fee_rate',",
                "    `interest_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'interest amount',",
                "    `late_fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'late fee amount',",
                "    `installment_fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'installment fee amount',",
                "    `profit_total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'profit stats total amount',",
                "    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'remark',",
                "    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,",
                "    `create_by` VARCHAR(64) DEFAULT NULL,",
                "    `create_time` DATETIME DEFAULT NULL,",
                "    `update_by` VARCHAR(64) DEFAULT NULL,",
                "    `update_time` DATETIME DEFAULT NULL,",
                "    `_openid` VARCHAR(64) NOT NULL DEFAULT '',",
                "    PRIMARY KEY (`id`),",
                "    UNIQUE KEY `uk_card_month` (`card_id`, `bill_month`),",
                "    KEY `idx_user_id` (`user_id`),",
                "    KEY `idx_bill_year_month` (`bill_year`, `bill_month_no`),",
                "    KEY `idx_bill_month` (`bill_month`)",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='special channel monthly bills'"
        ));

        ensureSpecialCardColumns();
        ensureSpecialBillColumns();
    }

    private void ensureSpecialCardColumns() {
        ensureColumnExists(
                "special_bank_card",
                "expire_date",
                "ALTER TABLE `special_bank_card` ADD COLUMN `expire_date` VARCHAR(32) DEFAULT NULL COMMENT 'expire date text as entered by user' AFTER `total_amount`"
        );
    }

    private void ensureSpecialBillColumns() {
        ensureColumnExists(
                "special_card_bill",
                "monthly_total_bill_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `monthly_total_bill_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'monthly total bill amount to repay' AFTER `bill_month_no`"
        );
        ensureColumnExists(
                "special_card_bill",
                "bill_day",
                "ALTER TABLE `special_card_bill` ADD COLUMN `bill_day` TINYINT DEFAULT NULL COMMENT 'bill day' AFTER `monthly_total_bill_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "repayment_day",
                "ALTER TABLE `special_card_bill` ADD COLUMN `repayment_day` TINYINT DEFAULT NULL COMMENT 'repayment day' AFTER `bill_day`"
        );
        ensureColumnExists(
                "special_card_bill",
                "bill_amount_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `bill_amount_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'bill amount verified' AFTER `bill_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "xiaohuan_repay_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `xiaohuan_repay_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'xiaohuan repayment verified' AFTER `xiaohuan_repay_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_repay_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_repay_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer repayment amount' AFTER `xiaohuan_repay_verified`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_repay_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_repay_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer repayment verified' AFTER `customer_repay_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "xiaohuan_consume_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `xiaohuan_consume_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'xiaohuan consume verified' AFTER `xiaohuan_consume_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_need_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_need_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer requested amount' AFTER `xiaohuan_consume_verified`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_need_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_need_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer requested amount verified' AFTER `customer_need_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_consume_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_consume_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'customer consume amount' AFTER `customer_need_verified`"
        );
        ensureColumnExists(
                "special_card_bill",
                "customer_consume_verified",
                "ALTER TABLE `special_card_bill` ADD COLUMN `customer_consume_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'customer consume verified' AFTER `customer_consume_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "interest_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `interest_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'interest amount' AFTER `consume_fee`"
        );
        ensureColumnExists(
                "special_card_bill",
                "late_fee_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `late_fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'late fee amount' AFTER `interest_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "installment_fee_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `installment_fee_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'installment fee amount' AFTER `late_fee_amount`"
        );
        ensureColumnExists(
                "special_card_bill",
                "profit_total_amount",
                "ALTER TABLE `special_card_bill` ADD COLUMN `profit_total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'profit stats total amount' AFTER `installment_fee_amount`"
        );
    }

    private void ensureCommonOpenidColumns() {
        String[] baseEntityTables = {
                "bank_sys_user",
                "card_user",
                "bank_card",
                "card_bill",
                "bill_detail",
                "card_transaction",
                "reminder_task",
                "book_category",
                "personal_book",
                "calendar_event",
                "user_feedback",
                "user_feedback_attachment",
                "user_feedback_process_log",
                "special_user_config",
                "special_bank_card",
                "special_card_bill"
        };
        for (String tableName : baseEntityTables) {
            ensureOpenidColumnIfTableExists(tableName);
        }
    }

    private void ensureOpenidColumnIfTableExists(String tableName) {
        if (!tableExists(tableName)) {
            return;
        }
        ensureColumnExists(
                tableName,
                "_openid",
                "ALTER TABLE `" + tableName + "` ADD COLUMN `_openid` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'cloudbase openid'"
        );
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
        log.info("Recorded schema patch: {}", patchKey);
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
        log.info("Added column {}.{}", tableName, columnName);
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`");
        log.info("Dropped column {}.{}", tableName, columnName);
    }

    private static String sql(String... lines) {
        return String.join("\n", lines);
    }
}
