package com.bank.admin.module.owner.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.owner.dto.CardOwnerQueryDTO;
import com.bank.admin.module.owner.dto.CardOwnerSaveDTO;
import com.bank.admin.module.owner.entity.CardOwner;
import com.bank.admin.module.owner.mapper.CardOwnerMapper;
import com.bank.admin.module.owner.service.CardOwnerService;
import com.bank.admin.module.owner.vo.CardOwnerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 持卡人 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class CardOwnerServiceImpl
        extends ServiceImpl<CardOwnerMapper, CardOwner>
        implements CardOwnerService {

    @Override
    public PageResult<CardOwnerVO> page(CardOwnerQueryDTO query) {
        LambdaQueryWrapper<CardOwner> wrapper = new LambdaQueryWrapper<CardOwner>()
                .like(StringUtils.hasText(query.getName()), CardOwner::getName, query.getName())
                .like(StringUtils.hasText(query.getPhone()), CardOwner::getPhone, query.getPhone())
                .eq(query.getStatus() != null, CardOwner::getStatus, query.getStatus())
                .orderByDesc(CardOwner::getCreateTime);

        Page<CardOwner> page = super.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);

        Page<CardOwnerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public CardOwnerVO detail(Long id) {
        CardOwner owner = getById(id);
        if (owner == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "持卡人不存在");
        }
        return toVO(owner); // 统一脱敏
    }

    @Override
    public void save(CardOwnerSaveDTO dto) {
        // 新增时手机号必填
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号不能为空");
        }
        validatePhone(dto.getPhone());
        validateIdCard(dto.getIdCard());
        CardOwner entity = new CardOwner();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        super.save(entity);
    }

    @Override
    public void update(CardOwnerSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        CardOwner entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "持卡人不存在");
        }
        // 只更新非空字段，跳过脱敏格式的手机号和身份证
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.getStatus());
        // 手机号：为空保留原值，非空时校验并更新
        if (StringUtils.hasText(dto.getPhone())) {
            if (isMaskedPhone(dto.getPhone())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "请输入完整的手机号");
            }
            validatePhone(dto.getPhone());
            entity.setPhone(dto.getPhone());
        }
        // 身份证：为空保留原值，非空时校验并更新
        if (StringUtils.hasText(dto.getIdCard())) {
            if (isMaskedIdCard(dto.getIdCard())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "请输入完整的身份证号");
            }
            validateIdCard(dto.getIdCard());
            entity.setIdCard(dto.getIdCard());
        }
        updateById(entity);
    }

    /**
     * 校验手机号格式
     */
    private void validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号格式不正确");
        }
    }

    /**
     * 校验身份证格式（18位或15位）
     */
    private void validateIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return;
        }
        // 18位身份证：6位地区码 + 8位出生日期 + 3位顺序码 + 1位校验码
        // 15位身份证：6位地区码 + 6位出生日期 + 3位顺序码
        if (!idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$") &&
            !idCard.matches("^[1-9]\\d{5}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份证号格式不正确");
        }
    }

    /**
     * 判断是否为脱敏手机号（如 138****1234）
     */
    private boolean isMaskedPhone(String phone) {
        return phone != null && phone.contains("*");
    }

    /**
     * 判断是否为脱敏身份证（包含 *）
     */
    private boolean isMaskedIdCard(String idCard) {
        return idCard != null && idCard.contains("*");
    }

    @Override
    public void delete(Long id) {
        CardOwner entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "持卡人不存在");
        }
        removeById(id);
    }

    private CardOwnerVO toVO(CardOwner owner) {
        CardOwnerVO vo = new CardOwnerVO();
        BeanUtils.copyProperties(owner, vo);
        vo.setStatusDesc(owner.getStatus() == null || owner.getStatus() == 0 ? "正常" : "禁用");
        // 手机号脱敏
        if (StringUtils.hasText(owner.getPhone()) && owner.getPhone().length() >= 7) {
            vo.setPhone(owner.getPhone().substring(0, 3) + "****" + owner.getPhone().substring(7));
        }
        // 身份证脱敏
        if (StringUtils.hasText(owner.getIdCard()) && owner.getIdCard().length() >= 14) {
            vo.setIdCard(owner.getIdCard().substring(0, 6) + "**********" +
                    owner.getIdCard().substring(owner.getIdCard().length() - 2));
        }
        return vo;
    }
}
