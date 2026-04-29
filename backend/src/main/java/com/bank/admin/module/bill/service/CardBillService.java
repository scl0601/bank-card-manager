package com.bank.admin.module.bill.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.vo.CardBillOverviewVO;
import com.bank.admin.module.bill.vo.CardBillVO;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账单 Service 接口
 */
public interface CardBillService {

    PageResult<CardBillVO> page(CardBillQueryDTO query);

    CardBillOverviewVO overview(CardBillQueryDTO query);

    CardBillVO detail(Long id);

    void save(CardBillSaveDTO dto);

    void update(CardBillSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void markRepaid(Long id, BigDecimal actualPayAmount, LocalDate actualPayDate);

    void exportExcel(CardBillQueryDTO query, OutputStream out);

    /**
     * 为新信用卡自动生成当年 1-12 月账单记录
     */
    void generateAnnualBills(Long cardId, Long ownerId, Integer billDay, Integer repayDay, BigDecimal feeRate);

    /**
     * 为信用卡生成指定年份 1-12 月账单记录
     */
    void generateAnnualBills(Long cardId, Long ownerId, Integer billDay, Integer repayDay, BigDecimal feeRate, Integer year);

    /**
     * 联动更新当前月份到当年 12 月的账单日、还款日、费率等信息
     */
    void syncScheduleFromMonth(Long cardId, String fromBillMonth, Integer newBillDay, Integer newRepayDay, BigDecimal feeRate, Long ownerId);

    /**
     * 手续费率调整后，同步刷新未来账单的费率与收益
     */
    void syncFutureFeeRateByOwnerIds(List<Long> ownerIds, BigDecimal feeRate);

    /**
     * 刷新账单生命周期，统一逾期状态
     */
    void refreshBillLifecycle();

    /**
     * 根据账单明细回写账单汇总金额与状态
     */
    void refreshBillAmountFromDetails(Long billId);
}
