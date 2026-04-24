package com.bank.admin.module.bill.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.entity.BillDetail;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.BillDetailMapper;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.bill.vo.CardBillOverviewVO;
import com.bank.admin.module.bill.vo.CardBillVO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.reminder.service.ReminderTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账单 ServiceImpl（含自动生成年账单、账单日/还款日联动、收益计算）
 */
@Service
@RequiredArgsConstructor
public class CardBillServiceImpl
        extends ServiceImpl<CardBillMapper, CardBill>
        implements CardBillService {

    private static final String[] STATUS_DESC = {"待还款", "已还清", "部分还款", "逾期"};
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BankCardMapper bankCardMapper;
    private final CardUserMapper cardUserMapper;
    private final BillDetailMapper billDetailMapper;
    private final ReminderTaskService reminderTaskService;

    @Override
    public PageResult<CardBillVO> page(CardBillQueryDTO query) {
        Page<CardBillVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<CardBillVO> result = (Page<CardBillVO>) baseMapper.selectPageWithInfo(
                page,
                query.getCardId(),
                query.getOwnerId(),
                query.getCardName(),
                query.getBillMonth(),
                query.getYear(),
                query.getStatus());
        result.getRecords().forEach(this::fillStatusDesc);
        return PageResult.of(result);
    }

    @Override
    public CardBillOverviewVO overview(CardBillQueryDTO query) {
        CardBillOverviewVO overview = baseMapper.selectOverview(
                query.getCardId(),
                query.getOwnerId(),
                query.getCardName(),
                query.getBillMonth(),
                query.getYear(),
                query.getStatus());
        if (overview == null) {
            overview = new CardBillOverviewVO();
        }
        Integer targetYear = query.getYear();
        if (targetYear == null && query.getBillMonth() != null && query.getBillMonth().length() >= 4) {
            targetYear = Integer.parseInt(query.getBillMonth().substring(0, 4));
        }
        overview.setYear(targetYear);
        overview.setBillCount(defaultLong(overview.getBillCount()));
        overview.setPendingCount(defaultLong(overview.getPendingCount()));
        overview.setRepaidCount(defaultLong(overview.getRepaidCount()));
        overview.setPartialCount(defaultLong(overview.getPartialCount()));
        overview.setOverdueCount(defaultLong(overview.getOverdueCount()));
        overview.setTotalBillAmount(scaleMoney(overview.getTotalBillAmount()));
        overview.setTotalFeeAmount(scaleMoney(overview.getTotalFeeAmount()));
        overview.setTotalPosCostAmount(scaleMoney(overview.getTotalPosCostAmount()));
        overview.setTotalNetProfit(scaleMoney(overview.getTotalNetProfit()));
        return overview;
    }

    @Override
    public CardBillVO detail(Long id) {
        CardBill bill = getById(id);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        CardBillVO vo = new CardBillVO();
        BeanUtils.copyProperties(bill, vo);
        if (bill.getRepayDate() != null) {
            vo.setRepayDay(bill.getRepayDate().getDayOfMonth());
        }
        fillStatusDesc(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CardBillSaveDTO dto) {
        BankCard card = requireCard(dto.getCardId());
        validateCardWritable(card, "录入账单");
        ensureNoDuplicate(dto.getCardId(), dto.getBillMonth(), null);

        CardBill entity = new CardBill();
        BeanUtils.copyProperties(dto, entity);
        entity.setBillAmount(scaleMoney(dto.getBillAmount()));
        applyOwnerInfo(entity, card.getUserId());
        entity.setBillDay(dto.getBillDay() != null ? dto.getBillDay() : card.getBillDay());
        applyFeeAndRepayInfo(entity, dto, card);
        recalculateProfit(entity);
        refreshBillState(entity);
        super.save(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            reminderTaskService.closeBillReminders(entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CardBillSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        CardBill entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        Long targetCardId = dto.getCardId() != null ? dto.getCardId() : entity.getCardId();
        BankCard card = requireCard(targetCardId);
        ensureNoDuplicate(targetCardId, dto.getBillMonth(), dto.getId());

        entity.setCardId(targetCardId);
        entity.setBillMonth(dto.getBillMonth());
        entity.setBillAmount(scaleMoney(dto.getBillAmount()));
        entity.setMinPayAmount(dto.getMinPayAmount());
        entity.setRemark(dto.getRemark());
        entity.setBillDay(dto.getBillDay() != null ? dto.getBillDay() : (entity.getBillDay() != null ? entity.getBillDay() : card.getBillDay()));
        if (dto.getActualPayAmount() != null) {
            entity.setActualPayAmount(scaleMoney(dto.getActualPayAmount()));
        }
        if (dto.getActualPayDate() != null) {
            entity.setActualPayDate(dto.getActualPayDate());
        }
        applyOwnerInfo(entity, card.getUserId());
        applyFeeAndRepayInfo(entity, dto, card);
        recalculateProfit(entity);
        refreshBillState(entity);
        updateById(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            reminderTaskService.closeBillReminders(entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        billDetailMapper.delete(new LambdaQueryWrapper<BillDetail>().eq(BillDetail::getBillId, id));
        reminderTaskService.removeTasksByBillId(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRepaid(Long id, BigDecimal actualPayAmount, LocalDate actualPayDate) {
        CardBill bill = getById(id);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        bill.setActualPayAmount(scaleMoney(actualPayAmount));
        bill.setActualPayDate(actualPayDate);
        refreshBillState(bill);
        updateById(bill);
        reminderTaskService.closeBillReminders(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateAnnualBills(Long cardId, Long ownerId, Integer billDay, Integer repayDay, BigDecimal feeRate) {
        BankCard card = requireCard(cardId);
        validateCreditCard(card);
        validateCardWritable(card, "生成账单");
        validateDayRange(billDay, "账单日");
        validateDayRange(repayDay, "还款日");
        YearMonth current = YearMonth.now();
        YearMonth start = YearMonth.of(current.getYear(), 1);
        YearMonth end = YearMonth.of(current.getYear(), 12);
        upsertBillsForRange(cardId, ownerId != null ? ownerId : card.getUserId(), billDay, repayDay, feeRate, start, end);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncScheduleFromMonth(Long cardId, String fromBillMonth, Integer newBillDay, Integer newRepayDay, BigDecimal feeRate, Long ownerId) {
        BankCard card = requireCard(cardId);
        validateCreditCard(card);
        validateCardWritable(card, "同步账单计划");
        Integer targetBillDay = newBillDay != null ? newBillDay : card.getBillDay();
        Integer targetRepayDay = newRepayDay != null ? newRepayDay : card.getRepayDay();
        validateDayRange(targetBillDay, "账单日");
        validateDayRange(targetRepayDay, "还款日");

        BigDecimal targetFeeRate = feeRate != null ? normalizeFeeRate(feeRate) : resolveEffectiveFeeRate(ownerId != null ? ownerId : card.getUserId());
        Long targetOwnerId = ownerId != null ? ownerId : card.getUserId();

        card.setBillDay(targetBillDay);
        card.setRepayDay(targetRepayDay);
        card.setUserId(targetOwnerId);
        bankCardMapper.updateById(card);

        YearMonth start = YearMonth.parse(fromBillMonth, MONTH_FMT);
        YearMonth end = YearMonth.of(start.getYear(), 12);
        upsertBillsForRange(cardId, targetOwnerId, targetBillDay, targetRepayDay, targetFeeRate, start, end);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFutureFeeRateByOwnerIds(List<Long> ownerIds, BigDecimal feeRate) {
        if (CollectionUtils.isEmpty(ownerIds)) {
            return;
        }
        BigDecimal normalizedRate = normalizeFeeRate(feeRate);
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        List<CardBill> bills = super.list(new LambdaQueryWrapper<CardBill>()
                .in(CardBill::getOwnerId, ownerIds)
                .ge(CardBill::getBillMonth, currentMonth));
        if (bills.isEmpty()) {
            return;
        }
        for (CardBill bill : bills) {
            applyOwnerInfo(bill, bill.getOwnerId());
            bill.setFeeRate(normalizedRate);
            recalculateProfit(bill);
            refreshBillState(bill);
        }
        updateBatchById(bills);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshBillLifecycle() {
        LocalDate today = LocalDate.now();
        List<CardBill> overdueBills = super.list(new LambdaQueryWrapper<CardBill>()
                .in(CardBill::getStatus, 0, 2)
                .isNotNull(CardBill::getRepayDate)
                .gt(CardBill::getBillAmount, BigDecimal.ZERO)
                .lt(CardBill::getRepayDate, today));
        if (overdueBills.isEmpty()) {
            return;
        }
        overdueBills.forEach(this::refreshBillState);
        updateBatchById(overdueBills);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshBillAmountFromDetails(Long billId) {
        if (billId == null) {
            return;
        }
        CardBill bill = getById(billId);
        if (bill == null) {
            return;
        }
        // 只刷新还款状态，不重新计算 billAmount
        refreshBillState(bill);
        updateById(bill);
        if (bill.getStatus() != null && bill.getStatus() == 1) {
            reminderTaskService.closeBillReminders(billId);
        }
    }

    @Override
    public void exportExcel(CardBillQueryDTO query, OutputStream out) {
        query.setCurrent(1);
        query.setSize(10000);
        List<CardBillVO> records = page(query).getRecords();

        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("客户"));
        heads.add(List.of("银行卡"));
        heads.add(List.of("账单月份"));
        heads.add(List.of("账单日"));
        heads.add(List.of("代还金额"));
        heads.add(List.of("手续费率"));
        heads.add(List.of("手续费收入"));
        heads.add(List.of("POS成本"));
        heads.add(List.of("净利润"));
        heads.add(List.of("还款日"));
        heads.add(List.of("状态"));
        heads.add(List.of("备注"));

        List<List<Object>> dataList = new ArrayList<>();
        for (CardBillVO vo : records) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getOwnerName());
            row.add(vo.getBankName() + " *" + vo.getCardNoLast4());
            row.add(vo.getBillMonth());
            row.add(vo.getBillDay());
            row.add(defaultZero(vo.getBillAmount()).toString());
            row.add(formatRatePercent(vo.getFeeRate()));
            row.add(defaultZero(vo.getFeeAmount()).toString());
            row.add(defaultZero(vo.getPosCostAmount()).toString());
            row.add(defaultZero(vo.getNetProfit()).toString());
            row.add(vo.getRepayDate());
            row.add(vo.getStatusDesc());
            row.add(vo.getRemark());
            dataList.add(row);
        }

        EasyExcel.write(out)
                .head(heads)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("代还账单")
                .doWrite(dataList);
    }

    private void applyFeeAndRepayInfo(CardBill entity, CardBillSaveDTO dto, BankCard card) {
        if (dto.getRepayDate() != null) {
            entity.setRepayDate(dto.getRepayDate());
        } else {
            Integer repayDay = dto.getRepayDay() != null ? dto.getRepayDay() : card.getRepayDay();
            if (repayDay != null && entity.getBillMonth() != null) {
                entity.setRepayDate(calcRepayDate(YearMonth.parse(entity.getBillMonth(), MONTH_FMT), repayDay));
            }
        }
        BigDecimal feeRate = dto.getFeeRate() != null ? dto.getFeeRate() : resolveEffectiveFeeRate(card.getUserId());
        entity.setFeeRate(normalizeFeeRate(feeRate));
        entity.setPosCostAmount(scaleMoney(dto.getPosCostAmount() != null ? dto.getPosCostAmount() : entity.getPosCostAmount()));
    }

    private void upsertBillsForRange(Long cardId, Long ownerId, Integer billDay, Integer repayDay, BigDecimal feeRate, YearMonth start, YearMonth end) {
        BigDecimal normalizedFeeRate = normalizeFeeRate(feeRate);
        String startMonth = start.format(MONTH_FMT);
        String endMonth = end.format(MONTH_FMT);
        List<CardBill> existingBills = super.list(new LambdaQueryWrapper<CardBill>()
                .eq(CardBill::getCardId, cardId)
                .ge(CardBill::getBillMonth, startMonth)
                .le(CardBill::getBillMonth, endMonth));
        Map<String, CardBill> existingMap = new HashMap<>();
        for (CardBill bill : existingBills) {
            existingMap.putIfAbsent(bill.getBillMonth(), bill);
        }

        List<CardBill> toInsert = new ArrayList<>();
        List<CardBill> toUpdate = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            String billMonth = cursor.format(MONTH_FMT);
            CardBill bill = existingMap.get(billMonth);
            if (bill == null) {
                bill = new CardBill();
                bill.setCardId(cardId);
                applyOwnerInfo(bill, ownerId);
                bill.setBillMonth(billMonth);
                bill.setBillDay(billDay);
                bill.setBillAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                bill.setMinPayAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                bill.setRepayDate(calcRepayDate(cursor, repayDay));
                bill.setRemark(null);
                bill.setFeeRate(normalizedFeeRate);
                bill.setFeeAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                bill.setPosCostAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                bill.setNetProfit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                refreshBillState(bill);
                toInsert.add(bill);
                continue;
            }
            applyOwnerInfo(bill, ownerId);
            bill.setBillDay(billDay);
            bill.setRepayDate(calcRepayDate(cursor, repayDay));
            bill.setFeeRate(normalizedFeeRate);
            recalculateProfit(bill);
            refreshBillState(bill);
            toUpdate.add(bill);
        }
        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }
    }

    private void ensureNoDuplicate(Long cardId, String billMonth, Long excludeId) {
        long count = super.count(new LambdaQueryWrapper<CardBill>()
                .eq(CardBill::getCardId, cardId)
                .eq(CardBill::getBillMonth, billMonth)
                .ne(excludeId != null, CardBill::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该银行卡本月账单已存在");
        }
    }

    private BankCard requireCard(Long cardId) {
        BankCard card = bankCardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        return card;
    }

    private void validateCreditCard(BankCard card) {
        if (card.getCardType() == null || card.getCardType() != 2) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "仅信用卡支持账单计划操作");
        }
    }

    private void validateCardWritable(BankCard card, String actionName) {
        int status = card.getStatus() == null ? 0 : card.getStatus();
        if (status == 0) {
            return;
        }
        throw new BusinessException(
                ResultCode.OPERATION_FAILED,
                "银行卡已" + (status == 1 ? "冻结" : "注销") + "，不允许" + actionName);
    }

    private BigDecimal resolveEffectiveFeeRate(Long ownerId) {
        if (ownerId == null) {
            return BigDecimal.ZERO;
        }
        CardUser user = cardUserMapper.selectById(ownerId);
        if (user == null) {
            return BigDecimal.ZERO;
        }
        if (user.getParentId() == null) {
            return defaultZero(user.getFeeRate());
        }
        CardUser parent = cardUserMapper.selectById(user.getParentId());
        return parent == null ? defaultZero(user.getFeeRate()) : defaultZero(parent.getFeeRate());
    }

    private void applyOwnerInfo(CardBill entity, Long ownerId) {
        entity.setOwnerId(ownerId);
        entity.setSupplierId(resolveTopUserId(ownerId));
    }

    private Long resolveTopUserId(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        CardUser user = cardUserMapper.selectById(ownerId);
        if (user == null) {
            return ownerId;
        }
        return user.getParentId() == null ? user.getId() : user.getParentId();
    }

    private void recalculateProfit(CardBill entity) {
        BigDecimal amount = scaleMoney(entity.getBillAmount());
        BigDecimal feeRate = normalizeFeeRate(entity.getFeeRate());
        BigDecimal feeAmount = amount.multiply(feeRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal posCost = scaleMoney(entity.getPosCostAmount());
        entity.setBillAmount(amount);
        entity.setFeeRate(feeRate);
        entity.setFeeAmount(feeAmount);
        entity.setPosCostAmount(posCost);
        entity.setNetProfit(feeAmount.subtract(posCost).setScale(2, RoundingMode.HALF_UP));
    }

    private void refreshBillState(CardBill entity) {
        // 根据明细自动计算实际还款金额
        BigDecimal totalIncome = calculateTotalIncomeFromDetails(entity.getId());
        entity.setActualPayAmount(totalIncome);

        // 根据实际还款金额计算状态
        entity.setStatus(resolveBillStatus(entity, LocalDate.now()));
    }

    /**
     * 计算账单明细中的收入总额（detailType=1）
     */
    private BigDecimal calculateTotalIncomeFromDetails(Long billId) {
        if (billId == null) {
            return BigDecimal.ZERO;
        }
        List<BillDetail> details = billDetailMapper.selectList(
            new LambdaQueryWrapper<BillDetail>()
                .eq(BillDetail::getBillId, billId)
                .eq(BillDetail::getDetailType, 1) // 收入类型
        );
        return details.stream()
            .map(BillDetail::getAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int resolveBillStatus(CardBill entity, LocalDate today) {
        BigDecimal billAmount = defaultZero(entity.getBillAmount());
        BigDecimal actualPayAmount = defaultZero(entity.getActualPayAmount());
        if (actualPayAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (billAmount.compareTo(BigDecimal.ZERO) <= 0 || actualPayAmount.compareTo(billAmount) >= 0) {
                return 1; // 已还清
            }
            if (entity.getRepayDate() != null && entity.getRepayDate().isBefore(today)) {
                return 3; // 逾期
            }
            return 2; // 部分还款
        }
        if (entity.getRepayDate() != null && entity.getRepayDate().isBefore(today) && billAmount.compareTo(BigDecimal.ZERO) > 0) {
            return 3; // 逾期
        }
        return 0; // 待还款
    }

    private BigDecimal extractBillAmountContribution(BillDetail detail) {
        if (detail.getDetailType() == null || detail.getDetailType() == 2) {
            return BigDecimal.ZERO;
        }
        return defaultZero(detail.getAmount()).abs();
    }

    private void fillStatusDesc(CardBillVO vo) {
        int status = vo.getStatus() == null ? 0 : vo.getStatus();
        vo.setStatusDesc(status < STATUS_DESC.length ? STATUS_DESC[status] : "未知");
    }

    private void validateDayRange(Integer day, String fieldName) {
        if (day == null || day < 1 || day > 31) {
            throw new BusinessException(ResultCode.PARAM_ERROR, fieldName + "必须在1-31之间");
        }
    }

    private LocalDate calcRepayDate(YearMonth ym, Integer repayDay) {
        if (repayDay == null || repayDay < 1) {
            return null;
        }
        YearMonth repayMonth = ym.plusMonths(1);
        int actualDay = Math.min(repayDay, repayMonth.lengthOfMonth());
        return repayMonth.atDay(actualDay);
    }

    private BigDecimal normalizeFeeRate(BigDecimal feeRate) {
        if (feeRate == null) {
            return BigDecimal.ZERO;
        }
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账单费率必须为0-100之间的数字，例如1表示1%");
        }
        return feeRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String formatRatePercent(BigDecimal feeRate) {
        return normalizeFeeRate(feeRate).setScale(2, RoundingMode.HALF_UP) + "%";
    }
}
