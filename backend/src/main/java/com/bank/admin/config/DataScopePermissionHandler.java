package com.bank.admin.config;

import com.bank.admin.common.util.CurrentUserUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Restricts SELF-scoped test accounts to data they created.
 */
@Component
public class DataScopePermissionHandler implements MultiDataPermissionHandler {

    private static final Map<String, String> SCOPED_TABLE_COLUMNS = Map.ofEntries(
            Map.entry("card_user", "create_by"),
            Map.entry("bank_card", "create_by"),
            Map.entry("card_bill", "create_by"),
            Map.entry("bill_detail", "create_by"),
            Map.entry("card_transaction", "create_by"),
            Map.entry("reminder_task", "create_by"),
            Map.entry("book_category", "create_by"),
            Map.entry("personal_book", "create_by"),
            Map.entry("calendar_event", "create_by"),
            Map.entry("user_feedback", "create_by"),
            Map.entry("user_feedback_attachment", "create_by"),
            Map.entry("user_feedback_process_log", "create_by"),
            Map.entry("special_user_config", "create_by"),
            Map.entry("special_bank_card", "create_by"),
            Map.entry("special_card_bill", "create_by"),
            Map.entry("operation_log", "operator")
    );

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        if (!CurrentUserUtil.isSelfDataScope() || table == null) {
            return null;
        }

        String column = SCOPED_TABLE_COLUMNS.get(normalizeTableName(table.getName()));
        if (column == null) {
            return null;
        }

        String username = CurrentUserUtil.getUsernameOrDefault("");
        if (username.isBlank()) {
            return null;
        }

        String tableRef = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        String condition = tableRef + "." + column + " = '" + username.replace("'", "''") + "'";
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build data-scope condition: " + condition, e);
        }
    }

    private String normalizeTableName(String tableName) {
        if (tableName == null) {
            return "";
        }
        String normalized = tableName.replace("`", "").toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        return dotIndex >= 0 ? normalized.substring(dotIndex + 1) : normalized;
    }
}
