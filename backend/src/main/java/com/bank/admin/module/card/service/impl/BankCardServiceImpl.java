package com.bank.admin.module.card.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.card.dto.BankCardQueryDTO;
import com.bank.admin.module.card.dto.BankCardSaveDTO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.service.BankCardService;
import com.bank.admin.module.card.vo.BankCardVO;
import com.bank.admin.module.owner.entity.CardOwner;
import com.bank.admin.module.owner.mapper.CardOwnerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;



import java.math.BigDecimal;
import java.util.List;

/**
 * 银行卡 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class BankCardServiceImpl
        extends ServiceImpl<BankCardMapper, BankCard>
        implements BankCardService {

    private final CardOwnerMapper cardOwnerMapper;

    @Override
    public PageResult<BankCardVO> page(BankCardQueryDTO query) {
        LambdaQueryWrapper<BankCard> wrapper = new LambdaQueryWrapper<BankCard>()
                .eq(query.getOwnerId() != null, BankCard::getOwnerId, query.getOwnerId())
                .like(StringUtils.hasText(query.getBankName()), BankCard::getBankName, query.getBankName())
                .like(StringUtils.hasText(query.getCardNoLast4()), BankCard::getCardNoLast4, query.getCardNoLast4())
                .eq(query.getCardType() != null, BankCard::getCardType, query.getCardType())
                .eq(query.getStatus() != null, BankCard::getStatus, query.getStatus())
                .orderByDesc(BankCard::getCreateTime);

        Page<BankCard> page = super.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);

        Page<BankCardVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public List<BankCardVO> listAll() {
        return super.list(new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getStatus, 0)
                .orderByAsc(BankCard::getOwnerId))
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
    public void save(BankCardSaveDTO dto) {
        if (!StringUtils.hasText(dto.getCardNo())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "卡号不能为空");
        }
        BankCard entity = new BankCard();
        BeanUtils.copyProperties(dto, entity);
        // 取卡号后四位
        if (dto.getCardNo().length() >= 4) {
            entity.setCardNoLast4(dto.getCardNo().substring(dto.getCardNo().length() - 4));
        }
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        // 借记卡清空信用卡专属字段
        if (entity.getCardType() != null && entity.getCardType() == 1) {
            entity.setCreditLimit(null);
            entity.setUsedAmount(null);
            entity.setBillDay(null);
            entity.setRepayDay(null);
        }
        super.save(entity);
    }

    @Override
    public void update(BankCardSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        BankCard entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        Integer newCardType = dto.getCardType();
        
        // 使用 lambdaUpdate 构建更新，支持 null 值
        var updateChain = lambdaUpdate().eq(BankCard::getId, dto.getId());
        
        updateChain.set(BankCard::getOwnerId, dto.getOwnerId());
        updateChain.set(BankCard::getBankName, dto.getBankName());
        updateChain.set(BankCard::getCardType, newCardType);
        updateChain.set(BankCard::getExpireDate, dto.getExpireDate());
        updateChain.set(BankCard::getStatus, dto.getStatus() == null ? 0 : dto.getStatus());
        updateChain.set(BankCard::getRemark, dto.getRemark());
        
        // 更新卡号（如果有传）
        if (StringUtils.hasText(dto.getCardNo()) && dto.getCardNo().length() >= 4) {
            updateChain.set(BankCard::getCardNo, dto.getCardNo());
            updateChain.set(BankCard::getCardNoLast4, dto.getCardNo().substring(dto.getCardNo().length() - 4));
        }
        
        // 根据卡片类型设置对应字段
        if (newCardType != null && newCardType == 1) {
            // 借记卡：清空信用卡字段，设置余额
            updateChain.set(BankCard::getCreditLimit, BigDecimal.ZERO);
            updateChain.set(BankCard::getUsedAmount, BigDecimal.ZERO);
            updateChain.set(BankCard::getBillDay, null);
            updateChain.set(BankCard::getRepayDay, null);
            updateChain.set(BankCard::getBalance, dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        } else if (newCardType != null && newCardType == 2) {
            // 信用卡：清空余额，设置信用卡字段
            updateChain.set(BankCard::getBalance, BigDecimal.ZERO);
            updateChain.set(BankCard::getCreditLimit, dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO);
            updateChain.set(BankCard::getUsedAmount, dto.getUsedAmount() != null ? dto.getUsedAmount() : BigDecimal.ZERO);
            // 账单日和还款日：小于1视为无效，存null
            updateChain.set(BankCard::getBillDay, (dto.getBillDay() != null && dto.getBillDay() >= 1) ? dto.getBillDay() : null);
            updateChain.set(BankCard::getRepayDay, (dto.getRepayDay() != null && dto.getRepayDay() >= 1) ? dto.getRepayDay() : null);
        }
        
        updateChain.update();
    }

    @Override
    public void delete(Long id) {
        BankCard entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "银行卡不存在");
        }
        removeById(id);
    }

    private BankCardVO toVO(BankCard card) {
        BankCardVO vo = new BankCardVO();
        BeanUtils.copyProperties(card, vo);
        // 卡片类型描述
        vo.setCardTypeDesc(card.getCardType() != null && card.getCardType() == 2 ? "信用卡" : "借记卡");
        // 状态描述
        vo.setStatusDesc(switch (card.getStatus() == null ? 0 : card.getStatus()) {
            case 1 -> "冻结";
            case 2 -> "注销";
            default -> "正常";
        });
        // 可用额度（信用卡）
        if (card.getCreditLimit() != null && card.getUsedAmount() != null) {
            vo.setAvailableAmount(card.getCreditLimit().subtract(card.getUsedAmount()));
        } else if (card.getCreditLimit() != null) {
            vo.setAvailableAmount(card.getCreditLimit());
        }
        // 持卡人姓名
        if (card.getOwnerId() != null) {
            CardOwner owner = cardOwnerMapper.selectOne(
                new LambdaQueryWrapper<CardOwner>()
                    .eq(CardOwner::getId, card.getOwnerId())
                    .eq(CardOwner::getIsDeleted, 0)
            );
            if (owner != null) {
                vo.setOwnerName(owner.getName());
            }
        }
        return vo;
    }
}
