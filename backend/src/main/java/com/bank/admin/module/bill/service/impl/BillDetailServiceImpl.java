package com.bank.admin.module.bill.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.dto.BillDetailSaveDTO;
import com.bank.admin.module.bill.entity.BillDetail;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.BillDetailMapper;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.bill.service.BillDetailService;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.bill.vo.BillDetailVO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

/**
 * 账单明细 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class BillDetailServiceImpl
        extends ServiceImpl<BillDetailMapper, BillDetail>
        implements BillDetailService {

    private static final String[] DETAIL_TYPE_DESC = {"消费", "还款"};

    private final CardBillMapper cardBillMapper;
    private final BankCardMapper bankCardMapper;
    private final CardBillService cardBillService;

    @Override
    public List<BillDetailVO> listByBillId(Long billId) {
        if (billId == null) {
            return List.of();
        }
        List<BillDetail> list = super.list(
                new LambdaQueryWrapper<BillDetail>()
                        .eq(BillDetail::getBillId, billId)
                        .orderByAsc(BillDetail::getDetailDate)
                        .orderByAsc(BillDetail::getCreateTime)
                        .orderByAsc(BillDetail::getId)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(BillDetailSaveDTO dto) {
        requireWritableBill(dto.getBillId(), "编辑账单");
        BillDetail entity = new BillDetail();
        fillEntity(entity, dto);
        super.save(entity);
        cardBillService.refreshBillAmountFromDetails(dto.getBillId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BillDetailSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        BillDetail entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "明细不存在");
        }
        Long originalBillId = entity.getBillId();
        requireWritableBill(originalBillId, "编辑账单");
        if (dto.getBillId() != null && !dto.getBillId().equals(originalBillId)) {
            requireWritableBill(dto.getBillId(), "编辑账单");
        }
        fillEntity(entity, dto);
        updateById(entity);
        refreshAffectedBills(originalBillId, entity.getBillId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BillDetail entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "明细不存在");
        }
        Long billId = entity.getBillId();
        requireWritableBill(billId, "编辑账单");
        removeById(id);
        cardBillService.refreshBillAmountFromDetails(billId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        Set<Long> affectedBillIds = new HashSet<>();
        for (Long id : ids) {
            BillDetail detail = getById(id);
            if (detail != null) {
                affectedBillIds.add(detail.getBillId());
            }
        }
        validateBillIdsWritable(affectedBillIds, "编辑账单");

        removeByIds(ids);

        for (Long billId : affectedBillIds) {
            cardBillService.refreshBillAmountFromDetails(billId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateType(List<Long> ids, Integer detailType) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        Set<Long> affectedBillIds = new HashSet<>();
        List<BillDetail> toUpdate = new ArrayList<>();

        for (Long id : ids) {
            BillDetail detail = getById(id);
            if (detail != null) {
                detail.setDetailType(detailType);
                detail.setAmount(normalizeAmount(detail.getAmount(), detailType));
                toUpdate.add(detail);
                affectedBillIds.add(detail.getBillId());
            }
        }
        validateBillIdsWritable(affectedBillIds, "编辑账单");

        updateBatchById(toUpdate);

        for (Long billId : affectedBillIds) {
            cardBillService.refreshBillAmountFromDetails(billId);
        }
    }

    private void refreshAffectedBills(Long originalBillId, Long targetBillId) {
        if (originalBillId != null) {
            cardBillService.refreshBillAmountFromDetails(originalBillId);
        }
        if (targetBillId != null && !targetBillId.equals(originalBillId)) {
            cardBillService.refreshBillAmountFromDetails(targetBillId);
        }
    }

    private CardBill requireBill(Long billId) {
        CardBill bill = cardBillMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "关联账单不存在");
        }
        return bill;
    }

    private CardBill requireWritableBill(Long billId, String actionName) {
        CardBill bill = requireBill(billId);
        validateBillCardWritable(bill, actionName);
        return bill;
    }

    private void validateBillIdsWritable(Set<Long> billIds, String actionName) {
        for (Long billId : billIds) {
            requireWritableBill(billId, actionName);
        }
    }

    private void validateBillCardWritable(CardBill bill, String actionName) {
        BankCard card = bankCardMapper.selectById(bill.getCardId());
        if (card == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        int status = card.getStatus() == null ? 0 : card.getStatus();
        if (status != 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "银行卡已" + cardStatusName(status) + "，不允许" + actionName);
        }
    }

    private String cardStatusName(int status) {
        return switch (status) {
            case 1 -> "冻结";
            case 2 -> "注销";
            case 3 -> "停用";
            default -> "停用";
        };
    }

    private void fillEntity(BillDetail entity, BillDetailSaveDTO dto) {
        BeanUtils.copyProperties(dto, entity);
        entity.setAmount(normalizeAmount(dto.getAmount(), dto.getDetailType()));
    }

    private BigDecimal normalizeAmount(BigDecimal amount, Integer detailType) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.abs().setScale(2, RoundingMode.HALF_UP);
        if (detailType != null && detailType == 1) {
            return normalized;
        }
        return normalized.negate();
    }

    private BillDetailVO toVO(BillDetail entity) {
        BillDetailVO vo = new BillDetailVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setAmount(entity.getAmount() == null ? BigDecimal.ZERO : entity.getAmount().abs());
        int t = entity.getDetailType() == null ? 0 : entity.getDetailType();
        vo.setDetailTypeDesc(t < DETAIL_TYPE_DESC.length ? DETAIL_TYPE_DESC[t] : "未知");
        return vo;
    }
}
