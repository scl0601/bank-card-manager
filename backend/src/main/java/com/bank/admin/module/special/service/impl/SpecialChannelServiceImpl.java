package com.bank.admin.module.special.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.special.dto.SpecialBillAfterYearDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillBatchDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillQueryDTO;
import com.bank.admin.module.special.dto.SpecialBillUpdateDTO;
import com.bank.admin.module.special.dto.SpecialCardSaveDTO;
import com.bank.admin.module.special.dto.SpecialProfitExtraFeeUpdateDTO;
import com.bank.admin.module.special.dto.SpecialProfitQueryDTO;
import com.bank.admin.module.special.dto.SpecialUserConfigSaveDTO;
import com.bank.admin.module.special.entity.SpecialBankCard;
import com.bank.admin.module.special.entity.SpecialCardBill;
import com.bank.admin.module.special.entity.SpecialUserConfig;
import com.bank.admin.module.special.mapper.SpecialBankCardMapper;
import com.bank.admin.module.special.mapper.SpecialCardBillMapper;
import com.bank.admin.module.special.mapper.SpecialUserConfigMapper;
import com.bank.admin.module.special.service.SpecialChannelService;
import com.bank.admin.module.special.vo.SpecialBillVO;
import com.bank.admin.module.special.vo.SpecialBillImportResultVO;
import com.bank.admin.module.special.vo.SpecialCardVO;
import com.bank.admin.module.special.vo.SpecialConfigVO;
import com.bank.admin.module.special.vo.SpecialProfitCardVO;
import com.bank.admin.module.special.vo.SpecialProfitMonthVO;
import com.bank.admin.module.special.vo.SpecialProfitOverviewVO;
import com.bank.admin.module.special.vo.SpecialProfitRowVO;
import com.bank.admin.module.special.vo.SpecialProfitStatsVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SpecialChannelServiceImpl implements SpecialChannelService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int START_YEAR = 2020;
    private static final int END_YEAR = 2026;
    private static final int MIN_IMPORT_YEAR = 1900;
    private static final int MAX_IMPORT_YEAR = 2100;
    private static final int EXPECTED_IMPORT_MONTH_COUNT = 12;
    private static final Pattern IMPORT_YEAR_PATTERN = Pattern.compile("(?<!\\d)((?:19|20)\\d{2}|2100)(?!\\d)");

    private final SpecialUserConfigMapper specialUserConfigMapper;
    private final SpecialBankCardMapper specialBankCardMapper;
    private final SpecialCardBillMapper specialCardBillMapper;
    private final CardUserMapper cardUserMapper;

    @Override
    public SpecialConfigVO getConfig() {
        SpecialUserConfig config = getActiveConfig();
        if (config == null) {
            return new SpecialConfigVO();
        }
        return toConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpecialConfigVO saveConfig(SpecialUserConfigSaveDTO dto) {
        CardUser user = requireActiveUser(dto.getUserId());
        SpecialUserConfig config = getActiveConfig();
        if (config == null) {
            config = new SpecialUserConfig();
            config.setUserId(user.getId());
            config.setStatus(0);
            config.setRemark(dto.getRemark());
            specialUserConfigMapper.insert(config);
        } else {
            config.setUserId(user.getId());
            config.setStatus(0);
            config.setRemark(dto.getRemark());
            specialUserConfigMapper.updateById(config);
        }
        syncAllSpecialDataUser(user.getId());
        return toConfigVO(config);
    }

    @Override
    public List<SpecialCardVO> listCards() {
        Long userId = resolveConfiguredUserId();
        if (userId == null) {
            return List.of();
        }
        List<SpecialBankCard> cards = specialBankCardMapper.selectList(new LambdaQueryWrapper<SpecialBankCard>()
                .eq(SpecialBankCard::getUserId, userId)
                .orderByAsc(SpecialBankCard::getId));
        if (cards.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> billCountMap = new HashMap<>();
        for (SpecialBankCard card : cards) {
            Long count = specialCardBillMapper.selectCount(new LambdaQueryWrapper<SpecialCardBill>()
                    .eq(SpecialCardBill::getCardId, card.getId()));
            billCountMap.put(card.getId(), count == null ? 0L : count);
        }
        CardUser user = cardUserMapper.selectById(userId);
        BigDecimal feeRate = resolveEffectiveFeeRate(userId);
        return cards.stream()
                .map(card -> toCardVO(card, user, feeRate, billCountMap.getOrDefault(card.getId(), 0L), loadCardDayTemplate(card.getId())))
                .sorted(Comparator.comparing(SpecialCardVO::getRepaymentDay, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SpecialCardVO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCard(SpecialCardSaveDTO dto) {
        Long targetUserId = dto.getUserId() != null ? dto.getUserId() : resolveConfiguredUserId();
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先配置特殊用户");
        }
        requireActiveUser(targetUserId);
        validateCard(dto, null);

        SpecialBankCard card = new SpecialBankCard();
        fillCard(card, dto, targetUserId);
        specialBankCardMapper.insert(card);
        generateFixedRangeBills(card.getId(), targetUserId, normalizeDay(dto.getBillDay()), normalizeDay(dto.getRepaymentDay()));
        return card.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCard(SpecialCardSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        SpecialBankCard card = specialBankCardMapper.selectById(dto.getId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        Long targetUserId = dto.getUserId() != null ? dto.getUserId() : card.getUserId();
        requireActiveUser(targetUserId);
        validateCard(dto, dto.getId());

        boolean userChanged = !Objects.equals(card.getUserId(), targetUserId);
        fillCard(card, dto, targetUserId);
        specialBankCardMapper.updateById(card);
        if (userChanged) {
            syncCardBillsUserAndRate(card.getId(), targetUserId);
        }
        syncCardBillDays(card.getId(), dto.getBillDay(), dto.getRepaymentDay());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCard(Long id) {
        SpecialBankCard card = specialBankCardMapper.selectById(id);
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        List<SpecialCardBill> bills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, id));
        for (SpecialCardBill bill : bills) {
            specialCardBillMapper.deleteById(bill.getId());
        }
        specialBankCardMapper.deleteById(id);
    }

    @Override
    public PageResult<SpecialBillVO> pageBills(SpecialBillQueryDTO query) {
        List<SpecialBankCard> cards = listTargetCards(query.getCardId(), query.getCardKeyword());
        if (cards.isEmpty()) {
            return emptyBillPage(query);
        }
        List<Long> cardIds = cards.stream().map(SpecialBankCard::getId).toList();
        Page<SpecialCardBill> billPage = specialCardBillMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<SpecialCardBill>()
                        .in(SpecialCardBill::getCardId, cardIds)
                        .eq(query.getYear() != null, SpecialCardBill::getBillYear, query.getYear())
                        .eq(query.getMonth() != null, SpecialCardBill::getBillMonthNo, query.getMonth())
                        .orderByAsc(SpecialCardBill::getBillYear)
                        .orderByAsc(SpecialCardBill::getBillMonthNo)
                        .orderByAsc(SpecialCardBill::getCardId));

        Map<Long, SpecialBankCard> cardMap = cards.stream()
                .collect(Collectors.toMap(SpecialBankCard::getId, card -> card, (left, right) -> left, LinkedHashMap::new));
        Map<Long, CardUser> userMap = loadUserMap(cards);
        List<SpecialBillVO> voList = billPage.getRecords().stream()
                .map(bill -> toBillVO(bill, cardMap.get(bill.getCardId()), userMap))
                .toList();
        Page<SpecialBillVO> voPage = new Page<>(billPage.getCurrent(), billPage.getSize(), billPage.getTotal());
        voPage.setRecords(voList);
        return PageResult.of(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBill(SpecialBillUpdateDTO dto) {
        SpecialCardBill bill = specialCardBillMapper.selectById(dto.getId());
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊账单不存在");
        }
        SpecialBankCard card = specialBankCardMapper.selectById(bill.getCardId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        validateSpecialCardWritable(card, "编辑账单");
        bill.setUserId(card.getUserId());
        if (dto.getMonthlyTotalBillAmount() != null) {
            bill.setMonthlyTotalBillAmount(scaleMoney(dto.getMonthlyTotalBillAmount()));
        }
        bill.setBillDay(normalizeDay(dto.getBillDay()));
        bill.setRepaymentDay(normalizeDay(dto.getRepaymentDay()));
        bill.setBillAmount(scaleMoney(dto.getBillAmount()));
        bill.setBillAmountVerified(Boolean.TRUE.equals(dto.getBillAmountVerified()));
        bill.setXiaohuanRepayAmount(scaleMoney(dto.getXiaohuanRepayAmount()));
        bill.setXiaohuanRepayVerified(Boolean.TRUE.equals(dto.getXiaohuanRepayVerified()));
        bill.setCustomerRepayAmount(scaleMoney(dto.getCustomerRepayAmount()));
        bill.setCustomerRepayVerified(Boolean.TRUE.equals(dto.getCustomerRepayVerified()));
        bill.setXiaohuanConsumeAmount(scaleMoney(dto.getXiaohuanConsumeAmount()));
        bill.setXiaohuanConsumeVerified(Boolean.TRUE.equals(dto.getXiaohuanConsumeVerified()));
        bill.setCustomerNeedAmount(scaleMoney(dto.getCustomerNeedAmount()));
        bill.setCustomerNeedVerified(Boolean.TRUE.equals(dto.getCustomerNeedVerified()));
        bill.setCustomerConsumeAmount(scaleMoney(dto.getCustomerConsumeAmount()));
        bill.setCustomerConsumeVerified(Boolean.TRUE.equals(dto.getCustomerConsumeVerified()));
        bill.setBalance(scaleMoney(dto.getBalance()));
        bill.setInterestAmount(scaleMoney(dto.getInterestAmount()));
        bill.setLateFeeAmount(scaleMoney(dto.getLateFeeAmount()));
        bill.setInstallmentFeeAmount(scaleMoney(dto.getInstallmentFeeAmount()));
        bill.setRemark(dto.getRemark());
        bill.setFeeRate(resolveEffectiveFeeRate(card.getUserId()));
        recalculateBill(bill);
        specialCardBillMapper.updateById(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SpecialBillImportResultVO importBills(MultipartFile file, Integer year) {
        validateImportFile(file);
        Integer targetYear = resolveImportYear(file, year);
        validateImportYear(targetYear);

        SpecialUserConfig config = getActiveConfig();
        if (config == null || config.getUserId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先配置特殊用户");
        }
        Long userId = config.getUserId();
        BigDecimal feeRate = resolveEffectiveFeeRate(userId);
        List<ImportedSpecialBillRow> importedRows = parseSpecialBillWorkbook(file, targetYear);
        Set<String> importedCardNames = importedRows.stream()
                .map(ImportedSpecialBillRow::bankName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validateImportedRowsComplete(importedRows, importedCardNames);

        Map<String, SpecialBankCard> cardMap = buildImportCardMap(userId);
        for (String bankName : importedCardNames) {
            if (!cardMap.containsKey(normalizeBankName(bankName))) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "Excel中的特殊银行卡不存在：" + bankName);
            }
        }

        ensureImportYearBills(importedCardNames, cardMap, userId, feeRate, targetYear);
        Map<String, SpecialCardBill> billMap = loadImportTargetBills(importedRows, cardMap, targetYear);
        SpecialBillImportResultVO result = new SpecialBillImportResultVO();
        result.setYear(targetYear);
        result.setUpdatedRows(0);
        result.setImportedCardNames(new ArrayList<>(importedCardNames));
        result.setImportedCardCount(importedCardNames.size());
        result.setSkippedCardNames(cardMap.values().stream()
                .map(SpecialBankCard::getBankName)
                .filter(StringUtils::hasText)
                .filter(bankName -> !importedCardNames.contains(bankName.trim()))
                .distinct()
                .toList());
        result.setWarnings(new ArrayList<>());

        BigDecimal totalBillAmount = moneyZero();
        BigDecimal totalRepayAmount = moneyZero();
        BigDecimal totalConsumeAmount = moneyZero();
        BigDecimal totalRepaymentFee = moneyZero();
        BigDecimal totalConsumeFee = moneyZero();
        BigDecimal totalProfitAmount = moneyZero();

        for (ImportedSpecialBillRow row : importedRows) {
            SpecialBankCard card = cardMap.get(normalizeBankName(row.bankName()));
            SpecialCardBill bill = billMap.get(importBillKey(card.getId(), row.monthNo()));
            if (bill == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "缺少特殊账单：" + row.bankName() + " " + targetYear + "-" + String.format("%02d", row.monthNo()));
            }
            bill.setUserId(userId);
            bill.setFeeRate(feeRate);
            bill.setBillAmount(row.billAmount());
            bill.setXiaohuanRepayAmount(row.xiaohuanRepayAmount());
            bill.setXiaohuanConsumeAmount(row.xiaohuanConsumeAmount());
            recalculateBill(bill);
            specialCardBillMapper.updateById(bill);

            totalBillAmount = totalBillAmount.add(bill.getBillAmount());
            totalRepayAmount = totalRepayAmount.add(bill.getXiaohuanRepayAmount());
            totalConsumeAmount = totalConsumeAmount.add(bill.getXiaohuanConsumeAmount());
            totalRepaymentFee = totalRepaymentFee.add(bill.getRepaymentFee());
            totalConsumeFee = totalConsumeFee.add(bill.getConsumeFee());
            totalProfitAmount = totalProfitAmount.add(bill.getProfitTotalAmount());
            result.setUpdatedRows(result.getUpdatedRows() + 1);
            result.getWarnings().addAll(row.warnings());
        }

        result.setTotalBillAmount(totalBillAmount.setScale(2, RoundingMode.HALF_UP));
        result.setTotalXiaohuanRepayAmount(totalRepayAmount.setScale(2, RoundingMode.HALF_UP));
        result.setTotalXiaohuanConsumeAmount(totalConsumeAmount.setScale(2, RoundingMode.HALF_UP));
        result.setTotalRepaymentFee(totalRepaymentFee.setScale(2, RoundingMode.HALF_UP));
        result.setTotalConsumeFee(totalConsumeFee.setScale(2, RoundingMode.HALF_UP));
        result.setTotalProfitAmount(totalProfitAmount.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBillsBeforeYear(SpecialBillBatchDeleteDTO dto) {
        if (dto.getBeforeYear() < MIN_IMPORT_YEAR || dto.getBeforeYear() > MAX_IMPORT_YEAR + 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "保留起始年份必须在1900-2101之间");
        }
        List<SpecialBankCard> targetCards = listTargetCards(dto.getCardId(), null);
        if (targetCards.isEmpty()) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        return specialCardBillMapper.delete(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, dto.getCardId())
                .lt(SpecialCardBill::getBillYear, dto.getBeforeYear()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBillsAfterYear(SpecialBillAfterYearDeleteDTO dto) {
        if (dto.getAfterYear() < MIN_IMPORT_YEAR - 1 || dto.getAfterYear() > MAX_IMPORT_YEAR) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "保留截止年份必须在1899-2100之间");
        }
        List<SpecialBankCard> targetCards = listTargetCards(dto.getCardId(), null);
        if (targetCards.isEmpty()) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        return specialCardBillMapper.delete(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, dto.getCardId())
                .gt(SpecialCardBill::getBillYear, dto.getAfterYear()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteBills(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择需要删除的账单");
        }
        List<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择需要删除的账单");
        }
        List<Long> allowedCardIds = listTargetCards(null, null).stream()
                .map(SpecialBankCard::getId)
                .toList();
        if (CollectionUtils.isEmpty(allowedCardIds)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        specialCardBillMapper.delete(new LambdaQueryWrapper<SpecialCardBill>()
                .in(SpecialCardBill::getId, distinctIds)
                .in(SpecialCardBill::getCardId, allowedCardIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfitExtraFees(SpecialProfitExtraFeeUpdateDTO dto) {
        SpecialCardBill bill = specialCardBillMapper.selectById(dto.getBillId());
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊账单不存在");
        }
        SpecialBankCard card = specialBankCardMapper.selectById(bill.getCardId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "特殊银行卡不存在");
        }
        validateSpecialCardWritable(card, "编辑账单");
        bill.setUserId(card.getUserId());
        bill.setInterestAmount(scaleMoney(dto.getInterestAmount()));
        bill.setLateFeeAmount(scaleMoney(dto.getLateFeeAmount()));
        bill.setInstallmentFeeAmount(scaleMoney(dto.getInstallmentFeeAmount()));
        bill.setFeeRate(resolveEffectiveFeeRate(card.getUserId()));
        recalculateBill(bill);
        specialCardBillMapper.updateById(bill);
    }

    @Override
    public SpecialProfitStatsVO stats(SpecialProfitQueryDTO query) {
        List<SpecialBankCard> cards = listTargetCards(query.getCardId(), null);
        Map<Long, SpecialBankCard> cardMap = cards.stream()
                .collect(Collectors.toMap(SpecialBankCard::getId, card -> card, (left, right) -> left, LinkedHashMap::new));
        List<SpecialCardBill> bills = listBillsForStats(query, cards);
        bills.forEach(this::recalculateBill);

        SpecialProfitStatsVO stats = new SpecialProfitStatsVO();
        stats.setOverview(buildOverview(query, bills));
        stats.setRows(buildProfitRows(bills, cardMap));
        stats.setCardStats(buildCardStats(bills, cardMap));
        stats.setMonthStats(buildMonthStats(bills));
        return stats;
    }

    private SpecialUserConfig getActiveConfig() {
        List<SpecialUserConfig> configs = specialUserConfigMapper.selectList(new LambdaQueryWrapper<SpecialUserConfig>()
                .eq(SpecialUserConfig::getStatus, 0)
                .orderByAsc(SpecialUserConfig::getId)
                .last("LIMIT 1"));
        return configs.isEmpty() ? null : configs.get(0);
    }

    private Long resolveConfiguredUserId() {
        SpecialUserConfig config = getActiveConfig();
        return config == null ? null : config.getUserId();
    }

    private SpecialConfigVO toConfigVO(SpecialUserConfig config) {
        SpecialConfigVO vo = new SpecialConfigVO();
        vo.setId(config.getId());
        vo.setUserId(config.getUserId());
        vo.setRemark(config.getRemark());
        CardUser user = config.getUserId() == null ? null : cardUserMapper.selectById(config.getUserId());
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setPhone(user.getPhone());
            vo.setFeeRate(resolveEffectiveFeeRate(user.getId()));
        }
        return vo;
    }

    private CardUser requireActiveUser(Long userId) {
        CardUser user = cardUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该用户已停用");
        }
        return user;
    }

    private void validateCard(SpecialCardSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getCardNoLast4()) || !dto.getCardNoLast4().trim().matches("\\d{4}")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "卡号后四位必须为4位数字");
        }
        BigDecimal totalAmount = scaleMoney(dto.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "总金额不能为负数");
        }
        normalizeDay(dto.getBillDay());
        normalizeDay(dto.getRepaymentDay());
        Long targetUserId = dto.getUserId() != null ? dto.getUserId() : resolveConfiguredUserId();
        if (targetUserId != null) {
            Long duplicateCount = specialBankCardMapper.selectCount(new LambdaQueryWrapper<SpecialBankCard>()
                    .eq(SpecialBankCard::getUserId, targetUserId)
                    .eq(SpecialBankCard::getBankName, dto.getBankName())
                    .eq(SpecialBankCard::getCardNoLast4, dto.getCardNoLast4().trim())
                    .ne(excludeId != null, SpecialBankCard::getId, excludeId));
            if (duplicateCount != null && duplicateCount > 0) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该特殊银行卡已存在");
            }
        }
    }

    private void fillCard(SpecialBankCard card, SpecialCardSaveDTO dto, Long userId) {
        card.setUserId(userId);
        card.setBankName(dto.getBankName().trim());
        card.setCardNoLast4(dto.getCardNoLast4().trim());
        card.setTotalAmount(scaleMoney(dto.getTotalAmount()));
        card.setExpireDate(dto.getExpireDate());
        card.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        card.setRemark(dto.getRemark());
    }

    private void generateFixedRangeBills(Long cardId, Long userId, Integer billDay, Integer repaymentDay) {
        BigDecimal feeRate = resolveEffectiveFeeRate(userId);
        List<SpecialCardBill> existingBills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, cardId));
        Set<String> existingMonths = existingBills.stream()
                .map(SpecialCardBill::getBillMonth)
                .collect(Collectors.toSet());

        List<SpecialCardBill> toInsert = new ArrayList<>();
        for (int year = START_YEAR; year <= END_YEAR; year++) {
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                String billMonth = ym.format(MONTH_FMT);
                if (existingMonths.contains(billMonth)) {
                    continue;
                }
                toInsert.add(newEmptySpecialBill(cardId, userId, year, month, billDay, repaymentDay, feeRate));
            }
        }
        for (SpecialCardBill bill : toInsert) {
            specialCardBillMapper.insert(bill);
        }
    }

    private SpecialCardBill newEmptySpecialBill(
            Long cardId,
            Long userId,
            int year,
            int month,
            Integer billDay,
            Integer repaymentDay,
            BigDecimal feeRate) {
        YearMonth ym = YearMonth.of(year, month);
        SpecialCardBill bill = new SpecialCardBill();
        bill.setCardId(cardId);
        bill.setUserId(userId);
        bill.setBillMonth(ym.format(MONTH_FMT));
        bill.setBillYear(year);
        bill.setBillMonthNo(month);
        bill.setMonthlyTotalBillAmount(moneyZero());
        bill.setBillDay(billDay);
        bill.setRepaymentDay(repaymentDay);
        bill.setBillAmount(moneyZero());
        bill.setBillAmountVerified(false);
        bill.setXiaohuanRepayAmount(moneyZero());
        bill.setXiaohuanRepayVerified(false);
        bill.setCustomerRepayAmount(moneyZero());
        bill.setCustomerRepayVerified(false);
        bill.setXiaohuanConsumeAmount(moneyZero());
        bill.setXiaohuanConsumeVerified(false);
        bill.setCustomerNeedAmount(moneyZero());
        bill.setCustomerNeedVerified(false);
        bill.setCustomerConsumeAmount(moneyZero());
        bill.setCustomerConsumeVerified(false);
        bill.setDiffAmount(moneyZero());
        bill.setBalance(moneyZero());
        bill.setFeeRate(feeRate);
        bill.setRepaymentFee(moneyZero());
        bill.setConsumeFee(moneyZero());
        bill.setInterestAmount(moneyZero());
        bill.setLateFeeAmount(moneyZero());
        bill.setInstallmentFeeAmount(moneyZero());
        bill.setProfitTotalAmount(moneyZero());
        return bill;
    }

    private void syncCardBillDays(Long cardId, Integer billDay, Integer repaymentDay) {
        Integer normalizedBillDay = normalizeDay(billDay);
        Integer normalizedRepaymentDay = normalizeDay(repaymentDay);
        List<SpecialCardBill> bills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, cardId));
        for (SpecialCardBill bill : bills) {
            bill.setBillDay(normalizedBillDay);
            bill.setRepaymentDay(normalizedRepaymentDay);
            specialCardBillMapper.updateById(bill);
        }
    }

    private void syncAllSpecialDataUser(Long userId) {
        BigDecimal feeRate = resolveEffectiveFeeRate(userId);
        List<SpecialBankCard> cards = specialBankCardMapper.selectList(new LambdaQueryWrapper<>());
        for (SpecialBankCard card : cards) {
            card.setUserId(userId);
            specialBankCardMapper.updateById(card);
        }
        List<SpecialCardBill> bills = specialCardBillMapper.selectList(new LambdaQueryWrapper<>());
        for (SpecialCardBill bill : bills) {
            bill.setUserId(userId);
            bill.setFeeRate(feeRate);
            recalculateBill(bill);
            specialCardBillMapper.updateById(bill);
        }
    }

    private void syncCardBillsUserAndRate(Long cardId, Long userId) {
        BigDecimal feeRate = resolveEffectiveFeeRate(userId);
        List<SpecialCardBill> bills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, cardId));
        for (SpecialCardBill bill : bills) {
            bill.setUserId(userId);
            bill.setFeeRate(feeRate);
            recalculateBill(bill);
            specialCardBillMapper.updateById(bill);
        }
    }

    private List<SpecialBankCard> listTargetCards(Long cardId, String cardKeyword) {
        Long userId = resolveConfiguredUserId();
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<SpecialBankCard> wrapper = new LambdaQueryWrapper<SpecialBankCard>()
                .eq(SpecialBankCard::getUserId, userId)
                .eq(cardId != null, SpecialBankCard::getId, cardId)
                .and(StringUtils.hasText(cardKeyword), nested -> nested
                        .like(SpecialBankCard::getBankName, cardKeyword.trim())
                        .or()
                        .like(SpecialBankCard::getCardNoLast4, cardKeyword.trim()))
                .orderByAsc(SpecialBankCard::getId);
        return specialBankCardMapper.selectList(wrapper);
    }

    private List<SpecialCardBill> listBillsForStats(SpecialProfitQueryDTO query, List<SpecialBankCard> cards) {
        if (CollectionUtils.isEmpty(cards)) {
            return List.of();
        }
        List<Long> cardIds = cards.stream().map(SpecialBankCard::getId).toList();
        return specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .in(SpecialCardBill::getCardId, cardIds)
                .eq(query.getYear() != null, SpecialCardBill::getBillYear, query.getYear())
                .eq(query.getMonth() != null, SpecialCardBill::getBillMonthNo, query.getMonth())
                .orderByAsc(SpecialCardBill::getRepaymentDay)
                .orderByAsc(SpecialCardBill::getBillYear)
                .orderByAsc(SpecialCardBill::getBillMonthNo)
                .orderByAsc(SpecialCardBill::getCardId));
    }

    private SpecialProfitOverviewVO buildOverview(SpecialProfitQueryDTO query, List<SpecialCardBill> bills) {
        SpecialProfitOverviewVO vo = new SpecialProfitOverviewVO();
        vo.setYear(query.getYear());
        vo.setMonth(query.getMonth());
        Set<Long> cardIds = bills.stream().map(SpecialCardBill::getCardId).collect(Collectors.toSet());
        vo.setCardCount((long) cardIds.size());
        vo.setBillCount((long) bills.size());
        applyTotals(vo, bills);
        return vo;
    }

    private List<SpecialProfitRowVO> buildProfitRows(List<SpecialCardBill> bills, Map<Long, SpecialBankCard> cardMap) {
        return bills.stream()
                .map(bill -> {
                    SpecialBankCard card = cardMap.get(bill.getCardId());
                    SpecialProfitRowVO vo = new SpecialProfitRowVO();
                    vo.setBillId(bill.getId());
                    vo.setCardId(bill.getCardId());
                    vo.setBillYear(bill.getBillYear());
                    vo.setBillMonthNo(bill.getBillMonthNo());
                    vo.setBillDay(bill.getBillDay());
                    vo.setRepaymentDay(bill.getRepaymentDay());
                    vo.setTotalAmount(scaleMoney(bill.getBillAmount()));
                    vo.setXiaohuanRepayAmount(scaleMoney(bill.getXiaohuanRepayAmount()));
                    vo.setXiaohuanConsumeAmount(scaleMoney(bill.getXiaohuanConsumeAmount()));
                    vo.setRepaymentFee(scaleMoney(bill.getRepaymentFee()));
                    vo.setConsumeFee(scaleMoney(bill.getConsumeFee()));
                    vo.setInterestAmount(scaleMoney(bill.getInterestAmount()));
                    vo.setLateFeeAmount(scaleMoney(bill.getLateFeeAmount()));
                    vo.setInstallmentFeeAmount(scaleMoney(bill.getInstallmentFeeAmount()));
                    vo.setTotalProfitAmount(scaleMoney(bill.getProfitTotalAmount()));
                    if (card != null) {
                        vo.setBankName(card.getBankName());
                        vo.setCardNoLast4(card.getCardNoLast4());
                        vo.setCardStatus(card.getStatus());
                    }
                    return vo;
                })
                .sorted(Comparator.comparing(SpecialProfitRowVO::getRepaymentDay, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SpecialProfitRowVO::getBillYear, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SpecialProfitRowVO::getBillMonthNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SpecialProfitRowVO::getCardId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<SpecialProfitCardVO> buildCardStats(List<SpecialCardBill> bills, Map<Long, SpecialBankCard> cardMap) {
        Map<Long, List<SpecialCardBill>> grouped = bills.stream()
                .collect(Collectors.groupingBy(SpecialCardBill::getCardId, LinkedHashMap::new, Collectors.toList()));
        List<SpecialProfitCardVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<SpecialCardBill>> entry : grouped.entrySet()) {
            SpecialBankCard card = cardMap.get(entry.getKey());
            SpecialProfitCardVO vo = new SpecialProfitCardVO();
            vo.setCardId(entry.getKey());
            if (card != null) {
                vo.setBankName(card.getBankName());
                vo.setCardNoLast4(card.getCardNoLast4());
                vo.setTotalAmount(scaleMoney(card.getTotalAmount()));
            }
            vo.setBillCount((long) entry.getValue().size());
            vo.setTotalMonthlyTotalBillAmount(sum(entry.getValue(), SpecialCardBill::getMonthlyTotalBillAmount));
            vo.setTotalBillAmount(sum(entry.getValue(), SpecialCardBill::getBillAmount));
            vo.setTotalXiaohuanRepayAmount(sum(entry.getValue(), SpecialCardBill::getXiaohuanRepayAmount));
            vo.setTotalCustomerRepayAmount(sum(entry.getValue(), SpecialCardBill::getCustomerRepayAmount));
            vo.setTotalXiaohuanConsumeAmount(sum(entry.getValue(), SpecialCardBill::getXiaohuanConsumeAmount));
            vo.setTotalCustomerNeedAmount(sum(entry.getValue(), SpecialCardBill::getCustomerNeedAmount));
            vo.setTotalCustomerConsumeAmount(sum(entry.getValue(), SpecialCardBill::getCustomerConsumeAmount));
            vo.setTotalDiffAmount(sum(entry.getValue(), SpecialCardBill::getDiffAmount));
            vo.setTotalRepaymentFee(sum(entry.getValue(), SpecialCardBill::getRepaymentFee));
            vo.setTotalConsumeFee(sum(entry.getValue(), SpecialCardBill::getConsumeFee));
            vo.setTotalInterestAmount(sum(entry.getValue(), SpecialCardBill::getInterestAmount));
            vo.setTotalLateFeeAmount(sum(entry.getValue(), SpecialCardBill::getLateFeeAmount));
            vo.setTotalInstallmentFeeAmount(sum(entry.getValue(), SpecialCardBill::getInstallmentFeeAmount));
            vo.setTotalProfitAmount(sum(entry.getValue(), SpecialCardBill::getProfitTotalAmount));
            result.add(vo);
        }
        result.sort(Comparator.comparing(SpecialProfitCardVO::getCardId, Comparator.nullsLast(Long::compareTo)));
        return result;
    }

    private List<SpecialProfitMonthVO> buildMonthStats(List<SpecialCardBill> bills) {
        Map<String, List<SpecialCardBill>> grouped = bills.stream()
                .collect(Collectors.groupingBy(SpecialCardBill::getBillMonth, LinkedHashMap::new, Collectors.toList()));
        List<SpecialProfitMonthVO> result = new ArrayList<>();
        for (Map.Entry<String, List<SpecialCardBill>> entry : grouped.entrySet()) {
            SpecialProfitMonthVO vo = new SpecialProfitMonthVO();
            vo.setBillMonth(entry.getKey());
            vo.setBillCount((long) entry.getValue().size());
            vo.setCardCount((long) entry.getValue().stream().map(SpecialCardBill::getCardId).collect(Collectors.toSet()).size());
            vo.setTotalMonthlyTotalBillAmount(sum(entry.getValue(), SpecialCardBill::getMonthlyTotalBillAmount));
            vo.setTotalBillAmount(sum(entry.getValue(), SpecialCardBill::getBillAmount));
            vo.setTotalXiaohuanRepayAmount(sum(entry.getValue(), SpecialCardBill::getXiaohuanRepayAmount));
            vo.setTotalCustomerRepayAmount(sum(entry.getValue(), SpecialCardBill::getCustomerRepayAmount));
            vo.setTotalXiaohuanConsumeAmount(sum(entry.getValue(), SpecialCardBill::getXiaohuanConsumeAmount));
            vo.setTotalCustomerNeedAmount(sum(entry.getValue(), SpecialCardBill::getCustomerNeedAmount));
            vo.setTotalCustomerConsumeAmount(sum(entry.getValue(), SpecialCardBill::getCustomerConsumeAmount));
            vo.setTotalDiffAmount(sum(entry.getValue(), SpecialCardBill::getDiffAmount));
            vo.setTotalRepaymentFee(sum(entry.getValue(), SpecialCardBill::getRepaymentFee));
            vo.setTotalConsumeFee(sum(entry.getValue(), SpecialCardBill::getConsumeFee));
            vo.setTotalInterestAmount(sum(entry.getValue(), SpecialCardBill::getInterestAmount));
            vo.setTotalLateFeeAmount(sum(entry.getValue(), SpecialCardBill::getLateFeeAmount));
            vo.setTotalInstallmentFeeAmount(sum(entry.getValue(), SpecialCardBill::getInstallmentFeeAmount));
            vo.setTotalProfitAmount(sum(entry.getValue(), SpecialCardBill::getProfitTotalAmount));
            result.add(vo);
        }
        result.sort(Comparator.comparing(SpecialProfitMonthVO::getBillMonth, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private void applyTotals(SpecialProfitOverviewVO vo, List<SpecialCardBill> bills) {
        vo.setTotalMonthlyTotalBillAmount(sum(bills, SpecialCardBill::getMonthlyTotalBillAmount));
        vo.setTotalBillAmount(sum(bills, SpecialCardBill::getBillAmount));
        vo.setTotalXiaohuanRepayAmount(sum(bills, SpecialCardBill::getXiaohuanRepayAmount));
        vo.setTotalCustomerRepayAmount(sum(bills, SpecialCardBill::getCustomerRepayAmount));
        vo.setTotalXiaohuanConsumeAmount(sum(bills, SpecialCardBill::getXiaohuanConsumeAmount));
        vo.setTotalCustomerNeedAmount(sum(bills, SpecialCardBill::getCustomerNeedAmount));
        vo.setTotalCustomerConsumeAmount(sum(bills, SpecialCardBill::getCustomerConsumeAmount));
        vo.setTotalDiffAmount(sum(bills, SpecialCardBill::getDiffAmount));
        vo.setTotalRepaymentFee(sum(bills, SpecialCardBill::getRepaymentFee));
        vo.setTotalConsumeFee(sum(bills, SpecialCardBill::getConsumeFee));
        vo.setTotalInterestAmount(sum(bills, SpecialCardBill::getInterestAmount));
        vo.setTotalLateFeeAmount(sum(bills, SpecialCardBill::getLateFeeAmount));
        vo.setTotalInstallmentFeeAmount(sum(bills, SpecialCardBill::getInstallmentFeeAmount));
        vo.setTotalProfitAmount(sum(bills, SpecialCardBill::getProfitTotalAmount));
    }

    private BigDecimal sum(List<SpecialCardBill> bills, java.util.function.Function<SpecialCardBill, BigDecimal> mapper) {
        return bills.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择需要导入的Excel文件");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持导入.xlsx文件");
        }
    }

    private Integer resolveImportYear(MultipartFile file, Integer year) {
        Integer filenameYear = extractImportYearFromFilename(file.getOriginalFilename());
        if (year != null && filenameYear != null && !Objects.equals(year, filenameYear)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件名年份" + filenameYear + "与选择导入年份" + year + "不一致");
        }
        if (year != null) {
            return year;
        }
        if (filenameYear != null) {
            return filenameYear;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "请选择导入年份，或在Excel文件名中包含4位年份");
    }

    private Integer extractImportYearFromFilename(String filename) {
        if (StringUtils.hasText(filename)) {
            Matcher matcher = IMPORT_YEAR_PATTERN.matcher(filename);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return null;
    }

    private void validateImportYear(Integer year) {
        if (year == null || year < MIN_IMPORT_YEAR || year > MAX_IMPORT_YEAR) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入年份必须在1900-2100之间");
        }
    }

    private List<ImportedSpecialBillRow> parseSpecialBillWorkbook(MultipartFile file, Integer targetYear) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Map<Integer, Sheet> monthSheets = collectImportMonthSheets(workbook);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            List<ImportedSpecialBillRow> rows = new ArrayList<>();
            Set<String> rowKeys = new HashSet<>();
            for (int month = 1; month <= EXPECTED_IMPORT_MONTH_COUNT; month++) {
                Sheet sheet = monthSheets.get(month);
                List<ImportedSpecialBillRow> monthRows = parseSpecialBillSheet(sheet, month, targetYear, evaluator, formatter);
                if (monthRows.isEmpty()) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, sheet.getSheetName() + "没有可导入的银行卡区块");
                }
                for (ImportedSpecialBillRow row : monthRows) {
                    String key = normalizeBankName(row.bankName()) + ":" + row.monthNo();
                    if (!rowKeys.add(key)) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "Excel存在重复银行卡区块：" + row.bankName() + " " + row.monthNo() + "月");
                    }
                }
                rows.addAll(monthRows);
            }
            return rows;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "读取Excel失败");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "解析Excel失败：" + e.getMessage());
        }
    }

    private void validateImportedRowsComplete(List<ImportedSpecialBillRow> importedRows, Set<String> importedCardNames) {
        if (importedRows.isEmpty() || importedCardNames.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel没有可导入的特殊账单数据");
        }
        int expectedRows = importedCardNames.size() * EXPECTED_IMPORT_MONTH_COUNT;
        if (importedRows.size() != expectedRows) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel银行卡区块不完整，每张导入卡必须包含12个月记录");
        }
        for (String bankName : importedCardNames) {
            Set<Integer> months = importedRows.stream()
                    .filter(row -> Objects.equals(row.bankName(), bankName))
                    .map(ImportedSpecialBillRow::monthNo)
                    .collect(Collectors.toSet());
            for (int month = 1; month <= EXPECTED_IMPORT_MONTH_COUNT; month++) {
                if (!months.contains(month)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "Excel缺少" + bankName + month + "月记录");
                }
            }
        }
    }

    private Map<Integer, Sheet> collectImportMonthSheets(Workbook workbook) {
        Map<Integer, Sheet> monthSheets = new LinkedHashMap<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Integer month = parseMonthSheetName(sheet.getSheetName());
            if (month == null) {
                continue;
            }
            if (monthSheets.put(month, sheet) != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在重复月份工作表：" + month + "月");
            }
        }
        if (monthSheets.size() != EXPECTED_IMPORT_MONTH_COUNT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "Excel必须包含1月到12月共12张工作表");
        }
        for (int month = 1; month <= EXPECTED_IMPORT_MONTH_COUNT; month++) {
            if (!monthSheets.containsKey(month)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "缺少工作表：" + month + "月");
            }
        }
        return monthSheets;
    }

    private Integer parseMonthSheetName(String sheetName) {
        if (!StringUtils.hasText(sheetName)) {
            return null;
        }
        String trimmed = sheetName.trim();
        if (!trimmed.endsWith("月")) {
            return null;
        }
        try {
            int month = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
            return month >= 1 && month <= 12 ? month : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ImportedSpecialBillRow> parseSpecialBillSheet(
            Sheet sheet,
            int month,
            Integer targetYear,
            FormulaEvaluator evaluator,
            DataFormatter formatter) {
        List<ImportedSpecialBillRow> rows = new ArrayList<>();
        int lastRow = Math.max(sheet.getLastRowNum(), 0);
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String bankName = cellText(row.getCell(1), evaluator, formatter);
            String billLabel = cellText(row.getCell(2), evaluator, formatter);
            String repaymentLabel = cellText(row.getCell(3), evaluator, formatter);
            if (!StringUtils.hasText(bankName) || !billLabel.startsWith("账单") || !repaymentLabel.startsWith("还款")) {
                continue;
            }
            List<String> warnings = new ArrayList<>();
            BigDecimal billAmount = readImportMoney(sheet, rowIndex, 8, sheet.getSheetName(), bankName, "账单金额", evaluator, warnings);
            BigDecimal repayAmount = readImportMoney(sheet, rowIndex + 1, 8, sheet.getSheetName(), bankName, "小焕还款", evaluator, warnings);
            BigDecimal consumeAmount = readImportMoney(sheet, rowIndex + 2, 8, sheet.getSheetName(), bankName, "小焕消费", evaluator, warnings);
            rows.add(new ImportedSpecialBillRow(
                    normalizeBankName(bankName),
                    targetYear,
                    month,
                    billAmount,
                    repayAmount,
                    consumeAmount,
                    warnings));
        }
        return rows;
    }

    private BigDecimal readImportMoney(
            Sheet sheet,
            int rowIndex,
            int columnIndex,
            String sheetName,
            String bankName,
            String fieldName,
            FormulaEvaluator evaluator,
            List<String> warnings) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            warnings.add(sheetName + " " + bankName + " " + fieldName + "为空，按0导入");
            return moneyZero();
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return scaleMoney(BigDecimal.valueOf(cell.getNumericCellValue()));
            }
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue value = evaluator.evaluate(cell);
                if (value == null || value.getCellType() == CellType.BLANK) {
                    warnings.add(sheetName + " " + bankName + " " + fieldName + "为空，按0导入");
                    return moneyZero();
                }
                if (value.getCellType() == CellType.NUMERIC) {
                    return scaleMoney(BigDecimal.valueOf(value.getNumberValue()));
                }
                throw new BusinessException(ResultCode.PARAM_ERROR, sheetName + " " + bankName + " " + fieldName + "公式结果不是数字");
            }
            String text = cell.toString().trim();
            if (!StringUtils.hasText(text)) {
                warnings.add(sheetName + " " + bankName + " " + fieldName + "为空，按0导入");
                return moneyZero();
            }
            return scaleMoney(new BigDecimal(text.replace(",", "")));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, sheetName + " " + bankName + " " + fieldName + "不是数字");
        }
    }

    private String cellText(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private Map<String, SpecialCardBill> loadImportTargetBills(
            List<ImportedSpecialBillRow> importedRows,
            Map<String, SpecialBankCard> cardMap,
            Integer targetYear) {
        Set<Long> cardIds = importedRows.stream()
                .map(row -> cardMap.get(normalizeBankName(row.bankName())))
                .filter(Objects::nonNull)
                .map(SpecialBankCard::getId)
                .collect(Collectors.toSet());
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                        .in(SpecialCardBill::getCardId, cardIds)
                        .eq(SpecialCardBill::getBillYear, targetYear))
                .stream()
                .collect(Collectors.toMap(
                        bill -> importBillKey(bill.getCardId(), bill.getBillMonthNo()),
                        bill -> bill,
                        (left, right) -> left));
    }

    private void ensureImportYearBills(
            Set<String> importedCardNames,
            Map<String, SpecialBankCard> cardMap,
            Long userId,
            BigDecimal feeRate,
            Integer targetYear) {
        for (String bankName : importedCardNames) {
            SpecialBankCard card = cardMap.get(normalizeBankName(bankName));
            if (card == null) {
                continue;
            }
            ensureCardYearBills(card, userId, feeRate, targetYear);
        }
    }

    private void ensureCardYearBills(SpecialBankCard card, Long userId, BigDecimal feeRate, Integer targetYear) {
        List<SpecialCardBill> existingBills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, card.getId())
                .eq(SpecialCardBill::getBillYear, targetYear));
        Set<Integer> existingMonths = existingBills.stream()
                .map(SpecialCardBill::getBillMonthNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean allMonthsExist = true;
        for (int month = 1; month <= EXPECTED_IMPORT_MONTH_COUNT; month++) {
            if (!existingMonths.contains(month)) {
                allMonthsExist = false;
                break;
            }
        }
        if (allMonthsExist) {
            return;
        }
        SpecialCardBill template = loadCardDayTemplate(card.getId());
        Integer billDay = template == null ? null : template.getBillDay();
        Integer repaymentDay = template == null ? null : template.getRepaymentDay();
        for (int month = 1; month <= EXPECTED_IMPORT_MONTH_COUNT; month++) {
            if (existingMonths.contains(month)) {
                continue;
            }
            SpecialCardBill bill = newEmptySpecialBill(card.getId(), userId, targetYear, month, billDay, repaymentDay, feeRate);
            specialCardBillMapper.insert(bill);
        }
    }

    private Map<String, SpecialBankCard> buildImportCardMap(Long userId) {
        Map<String, SpecialBankCard> cardMap = new LinkedHashMap<>();
        for (SpecialBankCard card : specialBankCardMapper.selectList(new LambdaQueryWrapper<SpecialBankCard>()
                .eq(SpecialBankCard::getUserId, userId))) {
            String bankName = normalizeBankName(card.getBankName());
            if (!StringUtils.hasText(bankName)) {
                continue;
            }
            if (cardMap.containsKey(bankName)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "特殊银行卡银行名重复，无法按银行名导入：" + bankName);
            }
            cardMap.put(bankName, card);
        }
        return cardMap;
    }

    private String importBillKey(Long cardId, Integer monthNo) {
        return cardId + ":" + monthNo;
    }

    private String normalizeBankName(String bankName) {
        return bankName == null ? "" : bankName.trim();
    }

    private void recalculateBill(SpecialCardBill bill) {
        BigDecimal monthlyTotalBillAmount = scaleMoney(bill.getMonthlyTotalBillAmount());
        BigDecimal billAmount = scaleMoney(bill.getBillAmount());
        BigDecimal repayAmount = scaleMoney(bill.getXiaohuanRepayAmount());
        BigDecimal customerRepayAmount = scaleMoney(bill.getCustomerRepayAmount());
        BigDecimal consumeAmount = scaleMoney(bill.getXiaohuanConsumeAmount());
        BigDecimal customerNeedAmount = scaleMoney(bill.getCustomerNeedAmount());
        BigDecimal customerConsumeAmount = scaleMoney(bill.getCustomerConsumeAmount());
        BigDecimal interestAmount = scaleMoney(bill.getInterestAmount());
        BigDecimal lateFeeAmount = scaleMoney(bill.getLateFeeAmount());
        BigDecimal installmentFeeAmount = scaleMoney(bill.getInstallmentFeeAmount());
        BigDecimal feeRate = normalizeFeeRate(bill.getFeeRate());
        bill.setMonthlyTotalBillAmount(monthlyTotalBillAmount);
        bill.setBillAmount(billAmount);
        bill.setBillAmountVerified(Boolean.TRUE.equals(bill.getBillAmountVerified()));
        bill.setXiaohuanRepayAmount(repayAmount);
        bill.setXiaohuanRepayVerified(Boolean.TRUE.equals(bill.getXiaohuanRepayVerified()));
        bill.setCustomerRepayAmount(customerRepayAmount);
        bill.setCustomerRepayVerified(Boolean.TRUE.equals(bill.getCustomerRepayVerified()));
        bill.setXiaohuanConsumeAmount(consumeAmount);
        bill.setXiaohuanConsumeVerified(Boolean.TRUE.equals(bill.getXiaohuanConsumeVerified()));
        bill.setCustomerNeedAmount(customerNeedAmount);
        bill.setCustomerNeedVerified(Boolean.TRUE.equals(bill.getCustomerNeedVerified()));
        bill.setCustomerConsumeAmount(customerConsumeAmount);
        bill.setCustomerConsumeVerified(Boolean.TRUE.equals(bill.getCustomerConsumeVerified()));
        bill.setDiffAmount(repayAmount.subtract(consumeAmount).setScale(2, RoundingMode.HALF_UP));
        bill.setBalance(scaleMoney(bill.getBalance()));
        bill.setFeeRate(feeRate);
        bill.setRepaymentFee(billAmount.multiply(feeRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        bill.setConsumeFee(consumeAmount.add(customerConsumeAmount).multiply(feeRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        bill.setInterestAmount(interestAmount);
        bill.setLateFeeAmount(lateFeeAmount);
        bill.setInstallmentFeeAmount(installmentFeeAmount);
        bill.setProfitTotalAmount(bill.getRepaymentFee()
                .add(bill.getConsumeFee())
                .add(interestAmount)
                .add(lateFeeAmount)
                .add(installmentFeeAmount)
                .setScale(2, RoundingMode.HALF_UP));
    }

    private boolean hasManualBillData(SpecialCardBill bill) {
        return isPositiveOrNegative(bill.getMonthlyTotalBillAmount())
                || bill.getBillDay() != null
                || bill.getRepaymentDay() != null
                || isPositiveOrNegative(bill.getBillAmount())
                || Boolean.TRUE.equals(bill.getBillAmountVerified())
                || isPositiveOrNegative(bill.getXiaohuanRepayAmount())
                || Boolean.TRUE.equals(bill.getXiaohuanRepayVerified())
                || isPositiveOrNegative(bill.getCustomerRepayAmount())
                || Boolean.TRUE.equals(bill.getCustomerRepayVerified())
                || isPositiveOrNegative(bill.getXiaohuanConsumeAmount())
                || Boolean.TRUE.equals(bill.getXiaohuanConsumeVerified())
                || isPositiveOrNegative(bill.getCustomerNeedAmount())
                || Boolean.TRUE.equals(bill.getCustomerNeedVerified())
                || isPositiveOrNegative(bill.getCustomerConsumeAmount())
                || Boolean.TRUE.equals(bill.getCustomerConsumeVerified())
                || isPositiveOrNegative(bill.getBalance())
                || isPositiveOrNegative(bill.getDiffAmount())
                || isPositiveOrNegative(bill.getRepaymentFee())
                || isPositiveOrNegative(bill.getConsumeFee())
                || isPositiveOrNegative(bill.getInterestAmount())
                || isPositiveOrNegative(bill.getLateFeeAmount())
                || isPositiveOrNegative(bill.getInstallmentFeeAmount())
                || isPositiveOrNegative(bill.getProfitTotalAmount())
                || StringUtils.hasText(bill.getRemark());
    }

    private boolean isPositiveOrNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private Map<Long, CardUser> loadUserMap(List<SpecialBankCard> cards) {
        Set<Long> userIds = cards.stream()
                .map(SpecialBankCard::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return cardUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(CardUser::getId, user -> user, (left, right) -> left));
    }

    private SpecialCardVO toCardVO(SpecialBankCard card, CardUser user, BigDecimal feeRate, Long billCount, SpecialCardBill dayTemplate) {
        SpecialCardVO vo = new SpecialCardVO();
        BeanUtils.copyProperties(card, vo);
        vo.setUserName(user == null ? null : user.getName());
        vo.setFeeRate(feeRate);
        vo.setStatusDesc(Objects.equals(card.getStatus(), 1) ? "停用" : "正常");
        vo.setBillCount(billCount);
        if (dayTemplate != null) {
            vo.setBillDay(dayTemplate.getBillDay());
            vo.setRepaymentDay(dayTemplate.getRepaymentDay());
        }
        return vo;
    }

    private SpecialCardBill loadCardDayTemplate(Long cardId) {
        List<SpecialCardBill> bills = specialCardBillMapper.selectList(new LambdaQueryWrapper<SpecialCardBill>()
                .eq(SpecialCardBill::getCardId, cardId)
                .and(wrapper -> wrapper
                        .isNotNull(SpecialCardBill::getBillDay)
                        .or()
                        .isNotNull(SpecialCardBill::getRepaymentDay))
                .orderByDesc(SpecialCardBill::getBillYear)
                .orderByDesc(SpecialCardBill::getBillMonthNo)
                .last("LIMIT 1"));
        return bills.isEmpty() ? null : bills.get(0);
    }

    private SpecialBillVO toBillVO(SpecialCardBill bill, SpecialBankCard card, Map<Long, CardUser> userMap) {
        SpecialBillVO vo = new SpecialBillVO();
        BeanUtils.copyProperties(bill, vo);
        if (card != null) {
            vo.setBankName(card.getBankName());
            vo.setCardNoLast4(card.getCardNoLast4());
            vo.setCardStatus(card.getStatus());
            vo.setTotalAmount(scaleMoney(card.getTotalAmount()));
            CardUser user = userMap.get(card.getUserId());
            vo.setUserName(user == null ? null : user.getName());
        }
        return vo;
    }

    private void validateSpecialCardWritable(SpecialBankCard card, String actionName) {
        if (Objects.equals(card.getStatus(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "特殊银行卡已停用，不允许" + actionName);
        }
    }

    private PageResult<SpecialBillVO> emptyBillPage(SpecialBillQueryDTO query) {
        Page<SpecialBillVO> page = new Page<>(query.getCurrent(), query.getSize(), 0);
        page.setRecords(List.of());
        return PageResult.of(page);
    }

    private BigDecimal resolveEffectiveFeeRate(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        CardUser user = cardUserMapper.selectById(userId);
        if (user == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (user.getParentId() == null) {
            return normalizeFeeRate(user.getFeeRate());
        }
        CardUser parent = cardUserMapper.selectById(user.getParentId());
        return parent == null ? normalizeFeeRate(user.getFeeRate()) : normalizeFeeRate(parent.getFeeRate());
    }

    private BigDecimal normalizeFeeRate(BigDecimal feeRate) {
        if (feeRate == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "费率必须为0-100之间的数字，例如1表示1%");
        }
        return feeRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizeDay(Integer day) {
        if (day == null) {
            return null;
        }
        if (day < 1 || day > 31) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账单日和还款日必须在1-31之间");
        }
        return day;
    }

    private BigDecimal moneyZero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record ImportedSpecialBillRow(
            String bankName,
            Integer year,
            Integer monthNo,
            BigDecimal billAmount,
            BigDecimal xiaohuanRepayAmount,
            BigDecimal xiaohuanConsumeAmount,
            List<String> warnings) {
    }
}
