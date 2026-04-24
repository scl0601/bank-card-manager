package com.bank.admin.module.card.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.card.dto.BankCardQueryDTO;
import com.bank.admin.module.card.dto.BankCardSaveDTO;
import com.bank.admin.module.card.vo.BankCardVO;
import com.bank.admin.module.card.vo.UserCardGroupVO;

import java.util.List;

/**
 * 银行卡 Service 接口
 */
public interface BankCardService {

    PageResult<BankCardVO> page(BankCardQueryDTO query);

    List<BankCardVO> listAll();

    BankCardVO detail(Long id);

    Long save(BankCardSaveDTO dto);

    void update(BankCardSaveDTO dto);

    void delete(Long id);

    /**
     * 按用户分组查询银行卡（两级层级模型）
     */
    List<UserCardGroupVO> listGroupedByUser(BankCardQueryDTO query);
}
