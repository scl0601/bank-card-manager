package com.bank.admin.module.card.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.card.dto.BankCardQueryDTO;
import com.bank.admin.module.card.dto.BankCardSaveDTO;
import com.bank.admin.module.card.vo.BankCardVO;

import java.util.List;

/**
 * 银行卡 Service 接口
 */
public interface BankCardService {

    PageResult<BankCardVO> page(BankCardQueryDTO query);

    List<BankCardVO> listAll();

    BankCardVO detail(Long id);

    void save(BankCardSaveDTO dto);

    void update(BankCardSaveDTO dto);

    void delete(Long id);
}
