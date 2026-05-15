package com.bank.admin.module.bill.mapper;

import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public class CardBillSqlProvider {

    public String selectPageWithInfo(Map<String, Object> params) {
        CardBillQueryDTO query = (CardBillQueryDTO) params.get("query");
        @SuppressWarnings("unchecked")
        List<Long> cardIds = (List<Long>) params.get("cardIds");
        String repayOrderDateExpr = "COALESCE(cb.repay_date, STR_TO_DATE(CONCAT(cb.bill_month, '-01'), '%Y-%m-%d'))";
        String billYearExpr = "CAST(SUBSTRING_INDEX(cb.bill_month, '-', 1) AS UNSIGNED)";
        String billMonthExpr = "CAST(SUBSTRING_INDEX(cb.bill_month, '-', -1) AS UNSIGNED)";

        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT
                    cb.id,
                    cb.card_id,
                    bc.card_no_last4,
                    bc.bank_name,
                    bc.status AS card_status,
                    cb.owner_id,
                    cu.name AS owner_name,
                    cb.bill_month,
                    cb.bill_day,
                    cb.bill_amount,
                    cb.min_pay_amount,
                    cb.repay_date,
                    DAY(cb.repay_date) AS repay_day,
                    cb.actual_pay_amount,
                    COALESCE((
                        SELECT SUM(ABS(bd.amount))
                        FROM bill_detail bd
                        WHERE bd.bill_id = cb.id
                          AND bd.detail_type = 0
                          AND bd.is_deleted = 0
                    ), 0) AS consume_amount,
                    cb.actual_pay_date,
                    cb.status,
                    cb.remark,
                    cb.fee_rate,
                    cb.fee_amount,
                    cb.fee_paid,
                    cb.fee_paid_amount,
                    cb.fee_pay_time,
                    cb.fee_pay_method,
                    cb.pos_cost_amount,
                    cb.other_fee_amount,
                    cb.net_profit,
                    bc.repay_method,
                    cb.verified,
                    cb.expense_verified,
                    cb.create_time
                FROM card_bill cb
                LEFT JOIN bank_card bc ON bc.id = cb.card_id AND bc.is_deleted = 0
                LEFT JOIN card_user cu ON cu.id = cb.owner_id AND cu.is_deleted = 0
                WHERE cb.is_deleted = 0
                """);
        appendFilters(sql, query, cardIds);
        boolean monthAscSort = isMonthAscSort(query, cardIds);
        sql.append(" ORDER BY cb.repay_date IS NULL ASC, ");
        if (monthAscSort && !isRepayDateScope(query)) {
            sql.append(billYearExpr)
                    .append(" DESC, ")
                    .append(billMonthExpr)
                    .append(" ASC, ")
                    .append(repayOrderDateExpr)
                    .append(" ASC, ");
        } else if (!monthAscSort) {
            sql.append("MOD(MONTH(")
                    .append(repayOrderDateExpr)
                    .append(") - CAST(SUBSTRING_INDEX(#{currentMonth}, '-', -1) AS SIGNED) + 12, 12) ASC, DAY(")
                    .append(repayOrderDateExpr)
                    .append(") ASC, YEAR(")
                    .append(repayOrderDateExpr)
                    .append(") DESC, ");
        } else {
            sql.append("YEAR(").append(repayOrderDateExpr).append(") DESC, ");
            sql.append("MONTH(")
                    .append(repayOrderDateExpr)
                    .append(") ASC, DAY(")
                    .append(repayOrderDateExpr)
                    .append(") ASC, ");
        }
        sql.append(" cb.card_id ASC, cb.create_time DESC, cb.id DESC");
        return sql.toString();
    }

    private boolean isMonthAscSort(CardBillQueryDTO query, List<Long> cardIds) {
        if (query == null) {
            return false;
        }
        if ("monthAsc".equalsIgnoreCase(query.getSortMode())) {
            return true;
        }
        if ("currentFirst".equalsIgnoreCase(query.getSortMode())) {
            return false;
        }
        return query.getCardId() != null && query.getOwnerId() == null && (cardIds == null || cardIds.isEmpty());
    }

    private boolean isRepayDateScope(CardBillQueryDTO query) {
        return query != null && (StringUtils.hasText(query.getRepayMonth()) || query.getRepayYear() != null);
    }

    @SuppressWarnings("unused")
    public String selectOverview(Map<String, Object> params) {
        CardBillQueryDTO query = (CardBillQueryDTO) params.get("query");
        @SuppressWarnings("unchecked")
        List<Long> cardIds = (List<Long>) params.get("cardIds");

        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT
                    COUNT(1) AS billCount,
                    SUM(CASE WHEN cb.status = 0 THEN 1 ELSE 0 END) AS pendingCount,
                    SUM(CASE WHEN cb.status = 1 THEN 1 ELSE 0 END) AS repaidCount,
                    SUM(CASE WHEN cb.status = 2 THEN 1 ELSE 0 END) AS partialCount,
                    SUM(CASE WHEN cb.status = 3 THEN 1 ELSE 0 END) AS overdueCount,
                    IFNULL(SUM(cb.bill_amount), 0) AS totalBillAmount,
                    IFNULL(SUM(cb.fee_amount), 0) AS totalFeeAmount,
                    IFNULL(SUM(cb.pos_cost_amount), 0) AS totalPosCostAmount,
                    IFNULL(SUM(cb.other_fee_amount), 0) AS totalOtherFeeAmount,
                    IFNULL(SUM(cb.net_profit), 0) AS totalNetProfit
                FROM card_bill cb
                LEFT JOIN bank_card bc ON bc.id = cb.card_id AND bc.is_deleted = 0
                LEFT JOIN card_user cu ON cu.id = cb.owner_id AND cu.is_deleted = 0
                WHERE cb.is_deleted = 0
                """);
        appendFilters(sql, query, cardIds);
        return sql.toString();
    }

    @SuppressWarnings("unused")
    public String selectUpcomingBills() {
        return """
                SELECT
                    cb.id,
                    cu.name AS owner_name,
                    bc.bank_name,
                    bc.card_no_last4,
                    cb.bill_month,
                    cb.bill_amount,
                    cb.repay_date,
                    cb.status
                FROM card_bill cb
                LEFT JOIN bank_card bc ON bc.id = cb.card_id AND bc.is_deleted = 0
                LEFT JOIN card_user cu ON cu.id = cb.owner_id AND cu.is_deleted = 0
                WHERE cb.is_deleted = 0
                  AND cb.status IN (0, 2)
                  AND cb.repay_date IS NOT NULL
                  AND cb.repay_date >= #{today}
                  AND cb.repay_date <= DATE_ADD(#{today}, INTERVAL 7 DAY)
                ORDER BY cb.repay_date ASC
                LIMIT 5
                """;
    }

    private void appendFilters(StringBuilder sql, CardBillQueryDTO query, List<Long> cardIds) {
        if (query != null && query.getCardId() != null) {
            sql.append(" AND cb.card_id = #{query.cardId}");
        }
        if (cardIds != null && !cardIds.isEmpty()) {
            sql.append(" AND cb.card_id IN (");
            for (int i = 0; i < cardIds.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("#{cardIds[").append(i).append("]}");
            }
            sql.append(")");
        }
        if (query != null && query.getOwnerId() != null) {
            sql.append(" AND (cb.owner_id = #{query.ownerId} OR cu.parent_id = #{query.ownerId})");
        }
        if (query != null && StringUtils.hasText(query.getOwnerName())) {
            sql.append(" AND cu.name LIKE CONCAT('%', #{query.ownerName}, '%')");
        }
        if (query != null && StringUtils.hasText(query.getCardName())) {
            sql.append("""
                     AND (
                        bc.bank_name LIKE CONCAT('%', #{query.cardName}, '%')
                        OR CONCAT(bc.bank_name, CASE WHEN bc.bank_name LIKE '%银行' THEN '' ELSE '银行' END) LIKE CONCAT('%', #{query.cardName}, '%')
                        OR bc.card_no_last4 LIKE CONCAT('%', #{query.cardName}, '%')
                    )
                    """);
        }
        if (query != null && StringUtils.hasText(query.getBillMonth())) {
            sql.append(" AND cb.bill_month = #{query.billMonth}");
        }
        if (query != null && query.getYear() != null) {
            sql.append(" AND LEFT(cb.bill_month, 4) = #{query.year}");
        }
        if (query != null && StringUtils.hasText(query.getRepayMonth())) {
            sql.append(" AND DATE_FORMAT(cb.repay_date, '%Y-%m') = #{query.repayMonth}");
        }
        if (query != null && query.getRepayYear() != null) {
            sql.append(" AND YEAR(cb.repay_date) = #{query.repayYear}");
        }
        if (query != null && StringUtils.hasText(query.getStartBillMonth())) {
            sql.append(" AND cb.bill_month >= #{query.startBillMonth}");
        }
        if (query != null && StringUtils.hasText(query.getEndBillMonth())) {
            sql.append(" AND cb.bill_month <= #{query.endBillMonth}");
        }
        if (query != null && query.getStatus() != null) {
            sql.append(" AND cb.status = #{query.status}");
        }
        if (query != null && query.getFeePaid() != null) {
            sql.append(" AND cb.fee_paid = #{query.feePaid}");
        }
    }
}
