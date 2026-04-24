package com.bank.admin.module.reminder.service.impl;

import com.bank.admin.common.enums.ReminderStatusEnum;
import com.bank.admin.common.enums.ReminderTypeEnum;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.reminder.dto.ReminderTaskQueryDTO;
import com.bank.admin.module.reminder.entity.ReminderTask;
import com.bank.admin.module.reminder.mapper.ReminderTaskMapper;
import com.bank.admin.module.reminder.service.ReminderTaskService;
import com.bank.admin.module.reminder.vo.ReminderTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提醒任务 ServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderTaskServiceImpl
        extends ServiceImpl<ReminderTaskMapper, ReminderTask>
        implements ReminderTaskService {

    private final ReminderTaskMapper reminderTaskMapper;
    private final BankCardMapper bankCardMapper;
    private final CardBillMapper cardBillMapper;

    @Override
    public PageResult<ReminderTaskVO> page(ReminderTaskQueryDTO query) {
        Page<ReminderTaskVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<ReminderTaskVO> result = (Page<ReminderTaskVO>)
                reminderTaskMapper.selectPageWithInfo(page, query);

        result.getRecords().forEach(vo -> {
            vo.setReminderTypeDesc(ReminderTypeEnum.fromCode(vo.getReminderType()).getDesc());
            vo.setStatusDesc(ReminderStatusEnum.fromCode(vo.getStatus()).getDesc());
        });

        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markHandled(Long id) {
        ReminderTask task = getAndCheckPending(id);
        task.setStatus(ReminderStatusEnum.HANDLED.getCode());
        task.setHandledTime(LocalDateTime.now());
        task.setHandledBy(currentUsername());
        updateById(task);
        log.info("[提醒中心] 标记已处理，id={}, operator={}", id, task.getHandledBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markIgnored(Long id) {
        ReminderTask task = getAndCheckPending(id);
        task.setStatus(ReminderStatusEnum.IGNORED.getCode());
        task.setHandledTime(LocalDateTime.now());
        task.setHandledBy(currentUsername());
        updateById(task);
        log.info("[提醒中心] 标记已忽略，id={}, operator={}", id, task.getHandledBy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkHandled(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            ReminderTask task = getById(id);
            if (task != null && task.getStatus() == ReminderStatusEnum.PENDING.getCode()) {
                task.setStatus(ReminderStatusEnum.HANDLED.getCode());
                task.setHandledTime(now);
                task.setHandledBy(username);
                updateById(task);
            }
        }
        log.info("[提醒中心] 批量标记已处理，count={}, operator={}", ids.size(), username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkIgnored(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            ReminderTask task = getById(id);
            if (task != null && task.getStatus() == ReminderStatusEnum.PENDING.getCode()) {
                task.setStatus(ReminderStatusEnum.IGNORED.getCode());
                task.setHandledTime(now);
                task.setHandledBy(username);
                updateById(task);
            }
        }
        log.info("[提醒中心] 批量标记已忽略，count={}, operator={}", ids.size(), username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanBillDueReminders() {
        LocalDate today = LocalDate.now();
        log.info("[提醒扫描] 开始扫描账单到期提醒，扫描日期={}", today);

        LocalDate scanStart = today.minusDays(1);
        LocalDate scanEnd = today.plusDays(7);

        LambdaQueryWrapper<CardBill> wrapper = new LambdaQueryWrapper<CardBill>()
                .in(CardBill::getStatus, 0, 2, 3)
                .isNotNull(CardBill::getRepayDate)
                .ge(CardBill::getRepayDate, scanStart)
                .le(CardBill::getRepayDate, scanEnd);

        List<CardBill> bills = cardBillMapper.selectList(wrapper);
        if (bills.isEmpty()) {
            log.info("[提醒扫描] 账单到期提醒：无需扫描的账单");
            return;
        }

        Set<String> existKeys = lambdaQuery()
                .eq(ReminderTask::getIsDeleted, 0)
                .eq(ReminderTask::getReminderDate, today)
                .in(ReminderTask::getReminderType, 1, 2, 3)
                .list()
                .stream()
                .filter(t -> t.getBillId() != null)
                .map(t -> t.getBillId() + "_" + t.getReminderType())
                .collect(Collectors.toSet());

        List<ReminderTask> toInsert = new ArrayList<>();
        for (CardBill bill : bills) {
            LocalDate repayDate = bill.getRepayDate();
            int daysDiff = (int) (repayDate.toEpochDay() - today.toEpochDay());

            int reminderType;
            String content;
            if (daysDiff < 0) {
                reminderType = ReminderTypeEnum.BILL_OVERDUE.getCode();
                content = String.format("账单 %s 还款日已过（%s），尚未还清，请尽快处理！", bill.getBillMonth(), repayDate);
            } else if (daysDiff == 0) {
                reminderType = ReminderTypeEnum.BILL_DUE_TODAY.getCode();
                content = String.format("账单 %s 今日（%s）为还款截止日，请及时还款！", bill.getBillMonth(), repayDate);
            } else {
                reminderType = ReminderTypeEnum.BILL_UPCOMING.getCode();
                content = String.format("账单 %s 还款日为 %s，还有 %d 天，请提前准备。", bill.getBillMonth(), repayDate, daysDiff);
            }

            closePendingBillRemindersExceptType(bill.getId(), reminderType);

            String dedupeKey = bill.getId() + "_" + reminderType;
            if (existKeys.contains(dedupeKey)) {
                continue;
            }

            ReminderTask task = new ReminderTask();
            task.setOwnerId(bill.getOwnerId());
            task.setCardId(bill.getCardId());
            task.setBillId(bill.getId());
            task.setReminderType(reminderType);
            task.setReminderDate(today);
            task.setContent(content);
            task.setStatus(ReminderStatusEnum.PENDING.getCode());
            task.setIsDeleted(0);
            toInsert.add(task);
        }

        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
            log.info("[提醒扫描] 账单到期提醒：新增 {} 条", toInsert.size());
        } else {
            log.info("[提醒扫描] 账单到期提醒：今日已全部提醒，无新增");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanCardExpiringReminders() {
        LocalDate today = LocalDate.now();
        log.info("[提醒扫描] 开始扫描卡片即将过期提醒，扫描日期={}", today);

        LambdaQueryWrapper<BankCard> wrapper = new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getStatus, 0)
                .isNotNull(BankCard::getExpireDate)
                .ne(BankCard::getExpireDate, "");

        List<BankCard> cards = bankCardMapper.selectList(wrapper);

        Set<Long> existCardIds = lambdaQuery()
                .eq(ReminderTask::getIsDeleted, 0)
                .eq(ReminderTask::getReminderType, ReminderTypeEnum.CARD_EXPIRING.getCode())
                .eq(ReminderTask::getReminderDate, today)
                .list()
                .stream()
                .filter(t -> t.getCardId() != null)
                .map(ReminderTask::getCardId)
                .collect(Collectors.toSet());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<ReminderTask> toInsert = new ArrayList<>();

        for (BankCard card : cards) {
            try {
                YearMonth expireYM = YearMonth.parse(card.getExpireDate(), fmt);
                LocalDate expireEnd = expireYM.atEndOfMonth();
                long daysLeft = expireEnd.toEpochDay() - today.toEpochDay();

                if (daysLeft > 30 || daysLeft < 0) {
                    continue;
                }
                if (existCardIds.contains(card.getId())) {
                    continue;
                }

                String content = daysLeft <= 0
                        ? String.format("银行卡 **** %s（%s）有效期已截止（%s），请及时更换！", card.getCardNoLast4(), card.getBankName(), card.getExpireDate())
                        : String.format("银行卡 **** %s（%s）有效期将于 %s 到期，还有 %d 天，请提前申请换卡。", card.getCardNoLast4(), card.getBankName(), card.getExpireDate(), daysLeft);

                ReminderTask task = new ReminderTask();
                task.setOwnerId(card.getUserId());
                task.setCardId(card.getId());
                task.setReminderType(ReminderTypeEnum.CARD_EXPIRING.getCode());
                task.setReminderDate(today);
                task.setContent(content);
                task.setStatus(ReminderStatusEnum.PENDING.getCode());
                task.setIsDeleted(0);
                toInsert.add(task);
            } catch (Exception e) {
                log.warn("[提醒扫描] 卡片 {} 有效期格式异常：{}", card.getId(), card.getExpireDate());
            }
        }

        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
            log.info("[提醒扫描] 卡片即将过期提醒：新增 {} 条", toInsert.size());
        } else {
            log.info("[提醒扫描] 卡片即将过期提醒：今日已全部提醒或无需提醒");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeBillReminders(Long billId) {
        if (billId == null) {
            return;
        }
        update(buildCloseBillReminderWrapper(billId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTasksByBillId(Long billId) {
        if (billId == null) {
            return;
        }
        remove(new LambdaQueryWrapper<ReminderTask>().eq(ReminderTask::getBillId, billId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTasksByCardId(Long cardId) {
        if (cardId == null) {
            return;
        }
        remove(new LambdaQueryWrapper<ReminderTask>().eq(ReminderTask::getCardId, cardId));
    }

    private void closePendingBillRemindersExceptType(Long billId, Integer currentType) {
        update(new LambdaUpdateWrapper<ReminderTask>()
                .eq(ReminderTask::getBillId, billId)
                .eq(ReminderTask::getStatus, ReminderStatusEnum.PENDING.getCode())
                .in(ReminderTask::getReminderType, 1, 2, 3)
                .ne(currentType != null, ReminderTask::getReminderType, currentType)
                .set(ReminderTask::getStatus, ReminderStatusEnum.HANDLED.getCode())
                .set(ReminderTask::getHandledTime, LocalDateTime.now())
                .set(ReminderTask::getHandledBy, "system"));
    }

    private LambdaUpdateWrapper<ReminderTask> buildCloseBillReminderWrapper(Long billId) {
        return new LambdaUpdateWrapper<ReminderTask>()
                .eq(ReminderTask::getBillId, billId)
                .eq(ReminderTask::getStatus, ReminderStatusEnum.PENDING.getCode())
                .in(ReminderTask::getReminderType, 1, 2, 3)
                .set(ReminderTask::getStatus, ReminderStatusEnum.HANDLED.getCode())
                .set(ReminderTask::getHandledTime, LocalDateTime.now())
                .set(ReminderTask::getHandledBy, "system");
    }

    private ReminderTask getAndCheckPending(Long id) {
        ReminderTask task = getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "提醒任务不存在");
        }
        if (task.getStatus() != ReminderStatusEnum.PENDING.getCode()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该提醒已处理或已忽略，无需重复操作");
        }
        return task;
    }

    private String currentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
