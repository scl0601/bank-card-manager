package com.bank.admin.module.transaction.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.transaction.dto.CardTransactionQueryDTO;
import com.bank.admin.module.transaction.dto.CardTransactionSaveDTO;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.bank.admin.module.transaction.service.CardTransactionService;
import com.bank.admin.module.transaction.vo.CardTransactionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class CardTransactionServiceImpl
        extends ServiceImpl<CardTransactionMapper, CardTransaction>
        implements CardTransactionService {

    private final BankCardMapper bankCardMapper;
    private final CardUserMapper cardUserMapper;

    @Override
    public PageResult<CardTransactionVO> page(CardTransactionQueryDTO query) {
        Page<CardTransactionVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<CardTransactionVO> result = (Page<CardTransactionVO>)
                baseMapper.selectPageWithInfo(page,
                        query.getCardId(), query.getOwnerId(),
                        query.getTxType(), query.getTxDateStart(), query.getTxDateEnd());

        result.getRecords().forEach(vo ->
                vo.setTxTypeDesc(vo.getTxType() != null && vo.getTxType() == 1 ? "收入" : "支出"));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CardTransactionSaveDTO dto) {
        BankCard card = requireCard(dto.getCardId());
        validateCardWritable(card, "录入流水");
        CardTransaction entity = new CardTransaction();
        BeanUtils.copyProperties(dto, entity);
        entity.setOwnerId(card.getUserId());
        entity.setSupplierId(resolveTopUserId(card.getUserId()));
        super.save(entity);
        rebuildCardBalanceSnapshots(card.getId(), defaultZero(card.getBalance()).add(resolveSignedAmount(entity)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CardTransactionSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        CardTransaction entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "流水记录不存在");
        }

        Long originalCardId = entity.getCardId();
        BigDecimal originalEffect = resolveSignedAmount(entity);
        BankCard originalCard = requireCard(originalCardId);
        BankCard targetCard = requireCard(dto.getCardId());

        if (!originalCardId.equals(dto.getCardId())) {
            validateCardWritable(targetCard, "调整流水归属");
        }
        BeanUtils.copyProperties(dto, entity);
        entity.setOwnerId(targetCard.getUserId());
        entity.setSupplierId(resolveTopUserId(targetCard.getUserId()));
        updateById(entity);

        BigDecimal newEffect = resolveSignedAmount(entity);
        if (originalCardId != null && originalCardId.equals(entity.getCardId())) {
            rebuildCardBalanceSnapshots(originalCardId, defaultZero(originalCard.getBalance()).subtract(originalEffect).add(newEffect));
            return;
        }
        rebuildCardBalanceSnapshots(originalCardId, defaultZero(originalCard.getBalance()).subtract(originalEffect));
        rebuildCardBalanceSnapshots(entity.getCardId(), defaultZero(targetCard.getBalance()).add(newEffect));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CardTransaction entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "流水记录不存在");
        }
        BankCard card = requireCard(entity.getCardId());
        BigDecimal removedEffect = resolveSignedAmount(entity);
        removeById(id);
        rebuildCardBalanceSnapshots(card.getId(), defaultZero(card.getBalance()).subtract(removedEffect));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<CardTransaction> records = super.list(new LambdaQueryWrapper<CardTransaction>().in(CardTransaction::getId, ids));
        if (records.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> removedEffectMap = new HashMap<>();
        for (CardTransaction record : records) {
            removedEffectMap.merge(record.getCardId(), resolveSignedAmount(record), BigDecimal::add);
        }
        removeByIds(ids);
        for (Map.Entry<Long, BigDecimal> entry : removedEffectMap.entrySet()) {
            BankCard card = requireCard(entry.getKey());
            rebuildCardBalanceSnapshots(entry.getKey(), defaultZero(card.getBalance()).subtract(entry.getValue()));
        }
    }

    @Override
    public void exportExcel(CardTransactionQueryDTO query, OutputStream out) {
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<CardTransactionVO> result = page(query);
        List<CardTransactionVO> records = result.getRecords();

        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("持卡人"));
        heads.add(List.of("银行卡"));
        heads.add(List.of("交易类型"));
        heads.add(List.of("金额"));
        heads.add(List.of("交易日期"));
        heads.add(List.of("描述"));
        heads.add(List.of("对手方"));
        heads.add(List.of("备注"));

        List<List<Object>> dataList = new ArrayList<>();
        for (CardTransactionVO vo : records) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getOwnerName());
            row.add(vo.getBankName() + " *" + vo.getCardNoLast4());
            row.add(vo.getTxTypeDesc());
            row.add(vo.getAmount() != null ? vo.getAmount().toString() : "0");
            row.add(vo.getTxDate());
            row.add(vo.getDescription());
            row.add(vo.getCounterpart());
            row.add(vo.getRemark());
            dataList.add(row);
        }

        EasyExcel.write(out)
                .head(heads)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("流水记录")
                .doWrite(dataList);
    }

    private BankCard requireCard(Long cardId) {
        BankCard card = bankCardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        return card;
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

    private void rebuildCardBalanceSnapshots(Long cardId, BigDecimal anchoredCurrentBalance) {
        if (cardId == null) {
            return;
        }
        BankCard card = bankCardMapper.selectById(cardId);
        if (card == null) {
            return;
        }

        List<CardTransaction> transactions = super.list(new LambdaQueryWrapper<CardTransaction>()
                .eq(CardTransaction::getCardId, cardId)
                .orderByAsc(CardTransaction::getTxDate)
                .orderByAsc(CardTransaction::getCreateTime)
                .orderByAsc(CardTransaction::getId));

        if (!isDebitCard(card)) {
            List<CardTransaction> needClear = transactions.stream()
                    .filter(tx -> tx.getBalanceSnapshot() != null)
                    .peek(tx -> tx.setBalanceSnapshot(null))
                    .toList();
            if (!needClear.isEmpty()) {
                updateBatchById(needClear);
            }
            return;
        }

        BigDecimal totalDelta = transactions.stream()
                .map(this::resolveSignedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingBalance = scaleMoney(defaultZero(anchoredCurrentBalance).subtract(totalDelta));
        BigDecimal runningBalance = openingBalance;
        List<CardTransaction> needUpdate = new ArrayList<>();
        for (CardTransaction transaction : transactions) {
            runningBalance = scaleMoney(runningBalance.add(resolveSignedAmount(transaction)));
            if (transaction.getBalanceSnapshot() == null || transaction.getBalanceSnapshot().compareTo(runningBalance) != 0) {
                transaction.setBalanceSnapshot(runningBalance);
                needUpdate.add(transaction);
            }
        }
        if (!needUpdate.isEmpty()) {
            updateBatchById(needUpdate);
        }
        card.setBalance(scaleMoney(runningBalance));
        bankCardMapper.updateById(card);
    }

    private boolean isDebitCard(BankCard card) {
        return card.getCardType() != null && card.getCardType() == 1;
    }

    private BigDecimal resolveSignedAmount(CardTransaction transaction) {
        BigDecimal amount = defaultZero(transaction.getAmount()).abs();
        if (transaction.getTxType() != null && transaction.getTxType() == 1) {
            return amount;
        }
        return amount.negate();
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
