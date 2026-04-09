package com.bank.admin.module.bill.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.bill.vo.CardBillVO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 账单 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class CardBillServiceImpl
        extends ServiceImpl<CardBillMapper, CardBill>
        implements CardBillService {

    private final BankCardMapper bankCardMapper;

    private static final String[] STATUS_DESC = {"待还款", "已还清", "部分还款", "逾期"};

    @Override
    public PageResult<CardBillVO> page(CardBillQueryDTO query) {
        Page<CardBillVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<CardBillVO> result = (Page<CardBillVO>)
                baseMapper.selectPageWithInfo(page,
                        query.getCardId(), query.getOwnerId(),
                        query.getBillMonth(), query.getStatus());
        result.getRecords().forEach(vo -> {
            int s = vo.getStatus() == null ? 0 : vo.getStatus();
            vo.setStatusDesc(s < STATUS_DESC.length ? STATUS_DESC[s] : "未知");
        });
        return PageResult.of(result);
    }

    @Override
    public CardBillVO detail(Long id) {
        CardBill bill = getById(id);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        CardBillVO vo = new CardBillVO();
        BeanUtils.copyProperties(bill, vo);
        int s = bill.getStatus() == null ? 0 : bill.getStatus();
        vo.setStatusDesc(s < STATUS_DESC.length ? STATUS_DESC[s] : "未知");
        return vo;
    }

    @Override
    public void save(CardBillSaveDTO dto) {
        BankCard card = bankCardMapper.selectById(dto.getCardId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        CardBill entity = new CardBill();
        BeanUtils.copyProperties(dto, entity);
        entity.setOwnerId(card.getOwnerId());
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        super.save(entity);
    }

    @Override
    public void update(CardBillSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        CardBill entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRepaid(Long id, BigDecimal actualPayAmount, LocalDate actualPayDate) {
        CardBill bill = getById(id);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }
        bill.setActualPayAmount(actualPayAmount);
        bill.setActualPayDate(actualPayDate);
        // 判断是否全额还清
        if (actualPayAmount.compareTo(bill.getBillAmount()) >= 0) {
            bill.setStatus(1); // 已还清
        } else {
            bill.setStatus(2); // 部分还款
        }
        updateById(bill);
    }

    @Override
    public void exportExcel(CardBillQueryDTO query, OutputStream out) {
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<CardBillVO> result = page(query);
        List<CardBillVO> records = result.getRecords();

        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("持卡人"));
        heads.add(List.of("银行卡"));
        heads.add(List.of("账单月份"));
        heads.add(List.of("账单金额"));
        heads.add(List.of("最低还款"));
        heads.add(List.of("还款日"));
        heads.add(List.of("状态"));
        heads.add(List.of("备注"));

        List<List<Object>> dataList = new ArrayList<>();
        for (CardBillVO vo : records) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getOwnerName());
            row.add(vo.getBankName() + " *" + vo.getCardNoLast4());
            row.add(vo.getBillMonth());
            row.add(vo.getBillAmount() != null ? vo.getBillAmount().toString() : "0");
            row.add(vo.getMinPayAmount() != null ? vo.getMinPayAmount().toString() : "0");
            row.add(vo.getRepayDate());
            row.add(vo.getStatusDesc());
            row.add(vo.getRemark());
            dataList.add(row);
        }

        EasyExcel.write(out)
                .head(heads)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("账单记录")
                .doWrite(dataList);
    }
}
