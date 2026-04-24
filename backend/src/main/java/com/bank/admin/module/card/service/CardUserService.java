package com.bank.admin.module.card.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.card.dto.CardUserQueryDTO;
import com.bank.admin.module.card.dto.CardUserSaveDTO;
import com.bank.admin.module.card.dto.UserFeeRateUpdateDTO;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.vo.CardUserVO;

import java.util.List;

/**
 * 用户（含两级层级）服务接口
 */
public interface CardUserService extends IService<CardUser> {

    /** 树形分页查询：一级用户+子用户内嵌 */
    PageResult<CardUserVO> pageTree(CardUserQueryDTO dto);

    /** 获取所有一级用户（含子用户列表） */
    List<CardUserVO> listTreeAll();

    /** 获取所有启用的一级用户（用于下拉选择） */
    List<CardUserVO> listActiveTopLevel();

    /** 详情 */
    CardUserVO detail(Long id);

    /** 新增 */
    Long save(CardUserSaveDTO dto);

    /** 编辑 */
    void update(CardUserSaveDTO dto);

    /** 删除 */
    void delete(Long id);

    /** 修改手续费率（级联更新所有子用户） */
    void updateFeeRateCascade(UserFeeRateUpdateDTO dto);
}
