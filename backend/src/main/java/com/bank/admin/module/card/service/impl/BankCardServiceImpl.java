package com.bank.admin.module.card.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.entity.BillDetail;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.BillDetailMapper;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.card.dto.BankCardQueryDTO;
import com.bank.admin.module.card.dto.BankCardSaveDTO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.card.service.BankCardService;
import com.bank.admin.module.card.vo.BankCardVO;
import com.bank.admin.module.card.vo.UserCardGroupVO;
import com.bank.admin.module.reminder.service.ReminderTaskService;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 银行卡 ServiceImpl（基于 card_user 两级用户模型）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankCardServiceImpl
        extends ServiceImpl<BankCardMapper, BankCard>
        implements BankCardService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CardUserMapper cardUserMapper;
    private final CardBillService cardBillService;
    private final CardBillMapper cardBillMapper;
    private final BillDetailMapper billDetailMapper;
    private final CardTransactionMapper cardTransactionMapper;
    private final ReminderTaskService reminderTaskService;

    @Override
    public PageResult<BankCardVO> page(BankCardQueryDTO query) {
        LambdaQueryWrapper<BankCard> wrapper = new LambdaQueryWrapper<BankCard>()
                .eq(query.getUserId() != null, BankCard::getUserId, query.getUserId())
                .like(StringUtils.hasText(query.getBankName()), BankCard::getBankName, query.getBankName())
                .like(StringUtils.hasText(query.getCardNoLast4()), BankCard::getCardNoLast4, query.getCardNoLast4())
                .eq(query.getCardType() != null, BankCard::getCardType, query.getCardType())
                .eq(query.getStatus() != null, BankCard::getStatus, query.getStatus())
                .orderByDesc(BankCard::getCreateTime);

        Page<BankCard> page = super.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<BankCardVO> voList = toVOList(page.getRecords());

        Page<BankCardVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return PageResult.of(voPage);
    }

    @Override
    public List<BankCardVO> listAll() {
        return super.list(new LambdaQueryWrapper<BankCard>()
                        .eq(BankCard::getStatus, 0)
                        .orderByAsc(BankCard::getUserId)
                        .orderByAsc(BankCard::getBankName))
                .stream().map(this::toVO).toList();
    }

    @Override
    public BankCardVO detail(Long id) {
        BankCard card = getById(id);
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        return toVO(card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(BankCardSaveDTO dto) {
        validateCardInput(dto);
        CardUser user = requireUser(dto.getUserId());

        BankCard entity = new BankCard();
        fillEntity(entity, dto);
        super.save(entity);

        if (shouldGenerateBills(entity)) {
            cardBillService.generateAnnualBills(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getBillDay(),
                    entity.getRepayDay(),
                    resolveEffectiveFeeRate(user));
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BankCardSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        validateCardInput(dto);
        BankCard entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        BankCard original = new BankCard();
        BeanUtils.copyProperties(entity, original);
        validateCardTypeChange(original, dto.getCardType());
        CardUser user = requireUser(dto.getUserId());

        fillEntity(entity, dto);
        updateById(entity);
        handleCardLifecycleAfterUpdate(original, entity, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BankCard entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        List<CardBill> bills = listBillsByCardId(id);
        long transactionCount = countTransactionHistory(id);
        if (hasBillBusinessHistory(bills) || transactionCount > 0) {
            throw new BusinessException(
                    ResultCode.OPERATION_FAILED,
                    "该银行卡已存在账单或流水历史，请改为冻结/注销，不允许直接删除");
        }
        removePlaceholderBills(bills, false);
        reminderTaskService.removeTasksByCardId(id);
        removeById(id);
    }

    /**
     * 按用户分组查询银行卡（用于前端展示）
     */
    @Override
    public List<UserCardGroupVO> listGroupedByUser(BankCardQueryDTO query) {
        List<CardUser> allUsers = cardUserMapper.selectList(
                new LambdaQueryWrapper<CardUser>()
                        .orderByAsc(CardUser::getSortOrder)
                        .orderByAsc(CardUser::getName));

        List<CardUser> topUsers = allUsers.stream()
                .filter(user -> user.getParentId() == null)
                .toList();

        LambdaQueryWrapper<BankCard> cardWrapper = new LambdaQueryWrapper<BankCard>()
                .like(StringUtils.hasText(query.getBankName()), BankCard::getBankName, query.getBankName())
                .like(StringUtils.hasText(query.getCardNoLast4()), BankCard::getCardNoLast4, query.getCardNoLast4())
                .eq(query.getCardType() != null, BankCard::getCardType, query.getCardType())
                .eq(query.getStatus() != null, BankCard::getStatus, query.getStatus())
                .orderByDesc(BankCard::getCreateTime);

        if (query.getUserId() != null) {
            cardWrapper.eq(BankCard::getUserId, query.getUserId());
        }

        List<BankCard> allCards = super.list(cardWrapper);
        List<BankCardVO> allCardVOs = toVOList(allCards);

        Map<Long, Long> userToTopMap = new HashMap<>();
        for (CardUser user : allUsers) {
            userToTopMap.put(user.getId(), user.getParentId() == null ? user.getId() : user.getParentId());
        }

        List<UserCardGroupVO> result = new ArrayList<>();
        for (CardUser topUser : topUsers) {
            List<BankCardVO> groupCards = allCardVOs.stream()
                    .filter(card -> topUser.getId().equals(userToTopMap.get(card.getUserId())))
                    .toList();

            UserCardGroupVO group = new UserCardGroupVO();
            group.setUserId(topUser.getId());
            group.setUserName(topUser.getName());
            group.setFeeRate(topUser.getFeeRate());
            group.setPhone(topUser.getPhone());
            group.setStatus(topUser.getStatus());
            group.setCardCount(groupCards.size());
            group.setCards(groupCards);
            result.add(group);
        }

        Set<Long> allAssignedIds = userToTopMap.keySet();
        List<BankCardVO> unassignedCards = allCardVOs.stream()
                .filter(card -> !allAssignedIds.contains(card.getUserId()))
                .toList();
        if (!unassignedCards.isEmpty()) {
            UserCardGroupVO unassigned = new UserCardGroupVO();
            unassigned.setUserId(0L);
            unassigned.setUserName("未分配");
            unassigned.setFeeRate(BigDecimal.ZERO);
            unassigned.setStatus(0);
            unassigned.setCardCount(unassignedCards.size());
            unassigned.setCards(unassignedCards);
            result.add(unassigned);
        }
        return result;
    }

    private void validateCardInput(BankCardSaveDTO dto) {
        if (dto.getUserId() == null || dto.getUserId() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新增银行卡时必须关联用户");
        }
        if (!StringUtils.hasText(dto.getCardNoLast4()) || !dto.getCardNoLast4().matches("\\d{4}")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "卡号后四位必须为4位数字");
        }
        if (dto.getCardType() != null && dto.getCardType() == 2) {
            if (dto.getBillDay() == null || dto.getBillDay() < 1 || dto.getBillDay() > 31) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "信用卡账单日必须在1-31之间");
            }
            if (dto.getRepayDay() == null || dto.getRepayDay() < 1 || dto.getRepayDay() > 31) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "信用卡还款日必须在1-31之间");
            }
        }
    }

    private void validateCardTypeChange(BankCard original, Integer targetCardType) {
        if (targetCardType == null || Objects.equals(original.getCardType(), targetCardType)) {
            return;
        }
        if (hasBillBusinessHistory(listBillsByCardId(original.getId())) || countTransactionHistory(original.getId()) > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该银行卡已存在业务历史，不允许变更卡片类型");
        }
    }

    private CardUser requireUser(Long userId) {
        CardUser user = cardUserMapper.selectById(userId);
        if (user == null) {
            log.warn("[新增/编辑银行卡] 用户不存在, 传入 userId={}", userId);
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void fillEntity(BankCard entity, BankCardSaveDTO dto) {
        entity.setUserId(dto.getUserId());
        entity.setBankName(dto.getBankName());
        entity.setCardNoLast4(dto.getCardNoLast4());
        entity.setOwnerRelation(StringUtils.hasText(dto.getOwnerRelation()) ? dto.getOwnerRelation().trim() : "本人");
        entity.setOwnerName(StringUtils.hasText(dto.getOwnerName()) ? dto.getOwnerName().trim() : null);
        entity.setCardType(dto.getCardType());
        entity.setExpireDate(dto.getExpireDate());
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        entity.setRepayMethod(dto.getRepayMethod());
        entity.setCreditLimit(parseDecimal(dto.getCreditLimit()));
        entity.setBalance(parseDecimal(dto.getBalance()));
        entity.setTotalLimit(parseDecimal(dto.getTotalLimit()));
        entity.setBillDay(dto.getBillDay());
        entity.setRepayDay(dto.getRepayDay());

        if (isCreditCard(entity)) {
            entity.setBalance(null);
            entity.setTotalLimit(null);
        } else {
            entity.setCreditLimit(null);
            entity.setBillDay(null);
            entity.setRepayDay(null);
        }

        if ("cloudpay".equals(dto.getRepayMethod())) {
            entity.setVerified(dto.getVerified() != null ? dto.getVerified() : Boolean.FALSE);
        } else {
            entity.setVerified(null);
        }
    }

    private void handleCardLifecycleAfterUpdate(BankCard original, BankCard current, CardUser user) {
        if (!isCardActive(current)) {
            reminderTaskService.removeTasksByCardId(current.getId());
        }
        if (hasOwnerInfoChanged(original, current)) {
            syncBillOwnerInfoForCurrentYear(current.getId(), current.getUserId());
        }
        if (shouldGenerateBills(current)) {
            cardBillService.syncScheduleFromMonth(
                    current.getId(),
                    YearMonth.now().format(MONTH_FMT),
                    current.getBillDay(),
                    current.getRepayDay(),
                    resolveEffectiveFeeRate(user),
                    current.getUserId());
            return;
        }
        List<CardBill> bills = listBillsByCardId(current.getId());
        if (isCreditCard(original) && !isCreditCard(current)) {
            removePlaceholderBills(bills, false);
            return;
        }
        if (isCreditCard(current) && !isCardActive(current)) {
            removePlaceholderBills(bills, true);
        }
    }

    private boolean isCreditCard(BankCard card) {
        return card.getCardType() != null && card.getCardType() == 2;
    }

    private boolean isCardActive(BankCard card) {
        return card.getStatus() == null || card.getStatus() == 0;
    }

    private boolean shouldGenerateBills(BankCard card) {
        return isCreditCard(card)
                && isCardActive(card)
                && card.getBillDay() != null
                && card.getRepayDay() != null;
    }

    private BigDecimal resolveEffectiveFeeRate(CardUser user) {
        if (user == null) {
            return BigDecimal.ZERO;
        }
        if (user.getParentId() == null) {
            return defaultZero(user.getFeeRate());
        }
        CardUser parent = cardUserMapper.selectById(user.getParentId());
        return parent == null ? defaultZero(user.getFeeRate()) : defaultZero(parent.getFeeRate());
    }

    private boolean hasOwnerInfoChanged(BankCard original, BankCard current) {
        return !Objects.equals(original.getUserId(), current.getUserId())
                || !Objects.equals(original.getOwnerName(), current.getOwnerName())
                || !Objects.equals(original.getOwnerRelation(), current.getOwnerRelation());
    }

    private void syncBillOwnerInfoForCurrentYear(Long cardId, Long ownerId) {
        String startMonth = YearMonth.now().withMonth(1).format(MONTH_FMT);
        String endMonth = YearMonth.now().withMonth(12).format(MONTH_FMT);
        Long supplierId = resolveTopUserId(ownerId);
        List<CardBill> bills = cardBillMapper.selectList(new LambdaQueryWrapper<CardBill>()
                .eq(CardBill::getCardId, cardId)
                .ge(CardBill::getBillMonth, startMonth)
                .le(CardBill::getBillMonth, endMonth));
        if (bills.isEmpty()) {
            return;
        }
        for (CardBill bill : bills) {
            bill.setOwnerId(ownerId);
            bill.setSupplierId(supplierId);
            cardBillMapper.updateById(bill);
        }
    }

    private Long resolveTopUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        CardUser user = cardUserMapper.selectById(userId);
        if (user == null) {
            return userId;
        }
        return user.getParentId() == null ? user.getId() : user.getParentId();
    }

    private List<CardBill> listBillsByCardId(Long cardId) {
        return cardBillMapper.selectList(new LambdaQueryWrapper<CardBill>().eq(CardBill::getCardId, cardId));
    }

    private long countTransactionHistory(Long cardId) {
        return cardTransactionMapper.selectCount(new LambdaQueryWrapper<CardTransaction>().eq(CardTransaction::getCardId, cardId));
    }

    private boolean hasBillBusinessHistory(List<CardBill> bills) {
        if (bills == null || bills.isEmpty()) {
            return false;
        }
        Set<Long> detailBillIds = loadBillIdsWithDetails(bills);
        return bills.stream().anyMatch(bill -> !isPlaceholderBill(bill, detailBillIds.contains(bill.getId())));
    }

    private void removePlaceholderBills(List<CardBill> bills, boolean futureOnly) {
        if (bills == null || bills.isEmpty()) {
            return;
        }
        String currentMonth = YearMonth.now().format(MONTH_FMT);
        Set<Long> detailBillIds = loadBillIdsWithDetails(bills);
        List<Long> removableIds = bills.stream()
                .filter(bill -> isPlaceholderBill(bill, detailBillIds.contains(bill.getId())))
                .filter(bill -> !futureOnly || (StringUtils.hasText(bill.getBillMonth()) && bill.getBillMonth().compareTo(currentMonth) >= 0))
                .map(CardBill::getId)
                .toList();
        if (removableIds.isEmpty()) {
            return;
        }
        removableIds.forEach(reminderTaskService::removeTasksByBillId);
        removableIds.forEach(cardBillMapper::deleteById);
    }

    private Set<Long> loadBillIdsWithDetails(List<CardBill> bills) {
        Set<Long> billIds = bills.stream().map(CardBill::getId).collect(Collectors.toSet());
        if (billIds.isEmpty()) {
            return Set.of();
        }
        return billDetailMapper.selectList(new LambdaQueryWrapper<BillDetail>().in(BillDetail::getBillId, billIds))
                .stream()
                .map(BillDetail::getBillId)
                .collect(Collectors.toSet());
    }

    private boolean isPlaceholderBill(CardBill bill, boolean hasDetails) {
        return !hasDetails
                && defaultZero(bill.getBillAmount()).compareTo(BigDecimal.ZERO) == 0
                && defaultZero(bill.getActualPayAmount()).compareTo(BigDecimal.ZERO) == 0
                && bill.getActualPayDate() == null;
    }

    private List<BankCardVO> toVOList(List<BankCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = cards.stream().map(BankCard::getUserId).collect(Collectors.toSet());
        Map<Long, CardUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<CardUser> users = cardUserMapper.selectBatchIds(userIds);
            for (CardUser user : users) {
                userMap.put(user.getId(), user);
            }
        }
        return cards.stream().map(card -> toVOWithUser(card, userMap)).toList();
    }

    private BankCardVO toVO(BankCard card) {
        return toVOWithUser(card, null);
    }

    private BankCardVO toVOWithUser(BankCard card, Map<Long, CardUser> userMap) {
        BankCardVO vo = new BankCardVO();
        BeanUtils.copyProperties(card, vo);
        vo.setCardTypeDesc(card.getCardType() != null && card.getCardType() == 2 ? "信用卡" : "借记卡");
        vo.setStatusDesc(switch (card.getStatus() == null ? 0 : card.getStatus()) {
            case 1 -> "冻结";
            case 2 -> "注销";
            default -> "正常";
        });
        if (card.getCreditLimit() != null) {
            vo.setAvailableAmount(card.getCreditLimit());
        } else if (card.getBalance() != null) {
            vo.setAvailableAmount(card.getBalance());
        }
        if (card.getUserId() != null && card.getUserId() > 0) {
            CardUser user = userMap != null ? userMap.get(card.getUserId()) : cardUserMapper.selectById(card.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
                vo.setEffectiveFeeRate(resolveEffectiveFeeRate(user));
            }
        }
        return vo;
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额格式不正确");
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
