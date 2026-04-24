package com.bank.admin.module.card.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.card.dto.CardUserQueryDTO;
import com.bank.admin.module.card.dto.CardUserSaveDTO;
import com.bank.admin.module.card.dto.UserFeeRateUpdateDTO;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.card.service.CardUserService;
import com.bank.admin.module.card.vo.CardUserVO;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现（含两级层级 + 手续费率联动）
 */
@Service
@RequiredArgsConstructor
public class CardUserServiceImpl extends ServiceImpl<CardUserMapper, CardUser> implements CardUserService {

    private final BankCardMapper bankCardMapper;
    private final CardBillMapper cardBillMapper;
    private final CardTransactionMapper cardTransactionMapper;
    private final CardBillService cardBillService;

    @Override
    public PageResult<CardUserVO> pageTree(CardUserQueryDTO dto) {
        LambdaQueryWrapper<CardUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(CardUser::getParentId)
                .like(StringUtils.hasText(dto.getName()), CardUser::getName, dto.getName())
                .like(StringUtils.hasText(dto.getPhone()), CardUser::getPhone, dto.getPhone())
                .eq(dto.getStatus() != null, CardUser::getStatus, dto.getStatus())
                .orderByAsc(CardUser::getSortOrder)
                .orderByDesc(CardUser::getCreateTime);

        Page<CardUser> page = super.page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
        List<CardUserVO> voList = buildPageTree(page.getRecords());

        Page<CardUserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return PageResult.of(voPage);
    }

    @Override
    public List<CardUserVO> listTreeAll() {
        List<CardUser> all = super.list(new LambdaQueryWrapper<CardUser>()
                .orderByAsc(CardUser::getSortOrder)
                .orderByAsc(CardUser::getName));
        return buildTree(all);
    }

    @Override
    public List<CardUserVO> listActiveTopLevel() {
        List<CardUser> topUsers = super.list(new LambdaQueryWrapper<CardUser>()
                .isNull(CardUser::getParentId)
                .eq(CardUser::getStatus, 0)
                .orderByAsc(CardUser::getSortOrder)
                .orderByAsc(CardUser::getName));
        return topUsers.stream().map(this::toFlatVO).toList();
    }

    @Override
    public CardUserVO detail(Long id) {
        CardUser entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (entity.getParentId() != null) {
            CardUser parent = getById(entity.getParentId());
            Map<Long, String> nameMap = buildNameMap(parent == null ? List.of(entity) : List.of(entity, parent));
            Map<Long, Long> cardCountMap = buildCardCountMap(Set.of(entity.getId()));
            CardUserVO vo = toBaseVO(entity, nameMap);
            vo.setEffectiveFeeRate(parent == null ? defaultZero(entity.getFeeRate()) : defaultZero(parent.getFeeRate()));
            vo.setCardCount(cardCountMap.getOrDefault(entity.getId(), 0L));
            return vo;
        }
        List<CardUser> children = super.list(new LambdaQueryWrapper<CardUser>()
                .eq(CardUser::getParentId, entity.getId())
                .orderByAsc(CardUser::getSortOrder)
                .orderByAsc(CardUser::getName));
        List<CardUser> all = new ArrayList<>();
        all.add(entity);
        all.addAll(children);
        Map<Long, String> nameMap = buildNameMap(all);
        Map<Long, Long> cardCountMap = buildCardCountMap(all.stream().map(CardUser::getId).collect(Collectors.toSet()));
        return toTreeVO(entity, children, nameMap, cardCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(CardUserSaveDTO dto) {
        long count = this.count(new LambdaQueryWrapper<CardUser>().eq(CardUser::getName, dto.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名称已存在");
        }
        CardUser parent = validateParent(dto.getParentId(), null);
        BigDecimal normalizedFeeRate = parent == null ? normalizeFeeRate(dto.getFeeRate()) : BigDecimal.ZERO;

        CardUser entity = new CardUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setSortOrder(entity.getSortOrder() == null ? 0 : entity.getSortOrder());
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
        entity.setFeeRate(normalizedFeeRate);
        super.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CardUserSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        CardUser original = getById(dto.getId());
        if (original == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        long count = this.count(new LambdaQueryWrapper<CardUser>()
                .eq(CardUser::getName, dto.getName())
                .ne(CardUser::getId, dto.getId()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名称已存在");
        }
        CardUser parent = validateParent(dto.getParentId(), dto.getId());
        BigDecimal newRate = parent == null ? normalizeFeeRate(dto.getFeeRate()) : BigDecimal.ZERO;

        CardUser entity = new CardUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setFeeRate(newRate);
        super.updateById(entity);

        if (parent == null && original.getParentId() == null && compareRate(original.getFeeRate(), newRate) != 0) {
            resetChildrenRateToInherited(original.getId());
            syncFutureBillFeeRate(original.getId(), newRate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CardUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        long cardCount = bankCardMapper.selectCount(new LambdaQueryWrapper<BankCard>().eq(BankCard::getUserId, id));
        if (cardCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该用户下还有" + cardCount + "张银行卡，无法删除");
        }
        long childCount = this.count(new LambdaQueryWrapper<CardUser>().eq(CardUser::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该用户下还有" + childCount + "个子用户，请先删除或转移子用户");
        }
        long billHistoryCount = cardBillMapper.selectCount(new LambdaQueryWrapper<CardBill>()
                .and(wrapper -> wrapper.eq(CardBill::getOwnerId, id).or().eq(CardBill::getSupplierId, id)));
        long transactionHistoryCount = cardTransactionMapper.selectCount(new LambdaQueryWrapper<CardTransaction>()
                .and(wrapper -> wrapper.eq(CardTransaction::getOwnerId, id).or().eq(CardTransaction::getSupplierId, id)));
        if (billHistoryCount > 0 || transactionHistoryCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该用户已存在关联账单或流水历史，无法删除");
        }
        super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeeRateCascade(UserFeeRateUpdateDTO dto) {
        CardUser user = getById(dto.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (user.getParentId() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "子用户的手续费率继承自父级，请修改父用户的费率");
        }
        BigDecimal normalizedRate = normalizeFeeRate(dto.getFeeRate());
        user.setFeeRate(normalizedRate);
        super.updateById(user);
        resetChildrenRateToInherited(dto.getUserId());
        syncFutureBillFeeRate(dto.getUserId(), normalizedRate);
    }

    private List<CardUserVO> buildPageTree(List<CardUser> topUsers) {
        if (topUsers == null || topUsers.isEmpty()) {
            return List.of();
        }
        List<Long> topIds = topUsers.stream().map(CardUser::getId).toList();
        List<CardUser> children = super.list(new LambdaQueryWrapper<CardUser>()
                .in(CardUser::getParentId, topIds)
                .orderByAsc(CardUser::getSortOrder)
                .orderByAsc(CardUser::getName));
        List<CardUser> all = new ArrayList<>(topUsers);
        all.addAll(children);
        Map<Long, String> nameMap = buildNameMap(all);
        Map<Long, Long> cardCountMap = buildCardCountMap(all.stream().map(CardUser::getId).collect(Collectors.toSet()));
        Map<Long, List<CardUser>> childrenMap = children.stream().collect(Collectors.groupingBy(CardUser::getParentId));
        return topUsers.stream()
                .map(top -> toTreeVO(top, childrenMap.getOrDefault(top.getId(), Collections.emptyList()), nameMap, cardCountMap))
                .toList();
    }

    private CardUser validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        CardUser parent = this.getById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "父用户不存在");
        }
        if (parent.getParentId() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只支持两级层级，父用户必须是一级用户");
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能将自己设为子用户");
        }
        return parent;
    }

    private void resetChildrenRateToInherited(Long topUserId) {
        LambdaUpdateWrapper<CardUser> childUpdate = new LambdaUpdateWrapper<>();
        childUpdate.eq(CardUser::getParentId, topUserId)
                .set(CardUser::getFeeRate, BigDecimal.ZERO);
        super.update(childUpdate);
    }

    private void syncFutureBillFeeRate(Long topUserId, BigDecimal feeRate) {
        List<Long> ownerIds = collectCascadeOwnerIds(topUserId);
        cardBillService.syncFutureFeeRateByOwnerIds(ownerIds, feeRate);
    }

    private List<Long> collectCascadeOwnerIds(Long topUserId) {
        List<Long> ownerIds = new ArrayList<>();
        ownerIds.add(topUserId);
        List<CardUser> children = super.list(new LambdaQueryWrapper<CardUser>().eq(CardUser::getParentId, topUserId));
        ownerIds.addAll(children.stream().map(CardUser::getId).toList());
        return ownerIds;
    }

    private int compareRate(BigDecimal left, BigDecimal right) {
        return defaultZero(left).compareTo(defaultZero(right));
    }

    private BigDecimal normalizeFeeRate(BigDecimal feeRate) {
        if (feeRate == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "一级用户必须填写约定手续费率");
        }
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手续费率必须在0-100之间");
        }
        return feeRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private CardUserVO toTreeVO(CardUser topUser, List<CardUser> children, Map<Long, String> nameMap, Map<Long, Long> cardCountMap) {
        CardUserVO vo = toBaseVO(topUser, nameMap);
        vo.setEffectiveFeeRate(defaultZero(topUser.getFeeRate()));
        List<CardUserVO> childVOs = new ArrayList<>();
        for (CardUser child : children) {
            CardUserVO childVo = toBaseVO(child, nameMap);
            childVo.setEffectiveFeeRate(defaultZero(topUser.getFeeRate()));
            childVo.setCardCount(cardCountMap.getOrDefault(child.getId(), 0L));
            childVOs.add(childVo);
        }
        vo.setChildren(childVOs.isEmpty() ? null : childVOs);
        Set<Long> userIds = new HashSet<>();
        userIds.add(topUser.getId());
        children.forEach(child -> userIds.add(child.getId()));
        long totalCardCount = userIds.stream().mapToLong(userId -> cardCountMap.getOrDefault(userId, 0L)).sum();
        vo.setCardCount(totalCardCount);
        return vo;
    }

    private CardUserVO toBaseVO(CardUser entity, Map<Long, String> nameMap) {
        CardUserVO vo = new CardUserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusDesc(entity.getStatus() != null && entity.getStatus() == 1 ? "停用" : "正常");
        if (entity.getParentId() != null) {
            vo.setParentName(nameMap.get(entity.getParentId()));
        }
        return vo;
    }

    private CardUserVO toFlatVO(CardUser entity) {
        CardUserVO vo = new CardUserVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setParentId(entity.getParentId());
        vo.setFeeRate(defaultZero(entity.getFeeRate()));
        vo.setEffectiveFeeRate(defaultZero(entity.getFeeRate()));
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private List<CardUserVO> buildTree(List<CardUser> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        Map<Long, List<CardUser>> childrenMap = all.stream()
                .filter(user -> user.getParentId() != null)
                .collect(Collectors.groupingBy(CardUser::getParentId));
        Map<Long, String> nameMap = buildNameMap(all);
        Map<Long, Long> cardCountMap = buildCardCountMap(all.stream().map(CardUser::getId).collect(Collectors.toSet()));
        List<CardUser> topLevel = all.stream()
                .filter(user -> user.getParentId() == null)
                .sorted(Comparator.comparing(CardUser::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(CardUser::getName, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<CardUserVO> result = new ArrayList<>();
        for (CardUser top : topLevel) {
            result.add(toTreeVO(top, childrenMap.getOrDefault(top.getId(), Collections.emptyList()), nameMap, cardCountMap));
        }
        return result;
    }

    private Map<Long, String> buildNameMap(List<CardUser> users) {
        return users.stream().collect(Collectors.toMap(CardUser::getId, CardUser::getName, (left, right) -> left, HashMap::new));
    }

    private Map<Long, Long> buildCardCountMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> cardCountMap = new HashMap<>();
        bankCardMapper.selectList(new LambdaQueryWrapper<BankCard>().in(BankCard::getUserId, userIds))
                .forEach(card -> cardCountMap.merge(card.getUserId(), 1L, Long::sum));
        return cardCountMap;
    }
}
