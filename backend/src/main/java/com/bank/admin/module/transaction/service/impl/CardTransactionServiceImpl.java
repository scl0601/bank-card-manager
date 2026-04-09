package com.bank.admin.module.transaction.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.transaction.dto.CardTransactionQueryDTO;
import com.bank.admin.module.transaction.dto.CardTransactionSaveDTO;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.bank.admin.module.transaction.service.CardTransactionService;
import com.bank.admin.module.transaction.vo.CardTransactionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 流水 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class CardTransactionServiceImpl
        extends ServiceImpl<CardTransactionMapper, CardTransaction>
        implements CardTransactionService {

    private final BankCardMapper bankCardMapper;

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
        BankCard card = bankCardMapper.selectById(dto.getCardId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        CardTransaction entity = new CardTransaction();
        BeanUtils.copyProperties(dto, entity);
        entity.setOwnerId(card.getOwnerId());
        // 更新余额快照
        if (card.getBalance() != null) {
            entity.setBalanceSnapshot(dto.getTxType() == 1
                    ? card.getBalance().add(dto.getAmount())
                    : card.getBalance().subtract(dto.getAmount()));
        }
        super.save(entity);
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
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(Long id) {
        CardTransaction entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "流水记录不存在");
        }
        removeById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        removeByIds(ids);
    }

    @Override
    public void exportExcel(CardTransactionQueryDTO query, OutputStream out) {
        // 导出时使用较大分页
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<CardTransactionVO> result = page(query);
        List<CardTransactionVO> records = result.getRecords();

        // 动态表头
        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("持卡人"));
        heads.add(List.of("银行卡"));
        heads.add(List.of("交易类型"));
        heads.add(List.of("金额"));
        heads.add(List.of("交易日期"));
        heads.add(List.of("描述"));
        heads.add(List.of("对手方"));
        heads.add(List.of("备注"));

        // 数据行
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
}
