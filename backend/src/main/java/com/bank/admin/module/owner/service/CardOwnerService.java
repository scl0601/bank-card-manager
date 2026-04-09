package com.bank.admin.module.owner.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.owner.dto.CardOwnerQueryDTO;
import com.bank.admin.module.owner.dto.CardOwnerSaveDTO;
import com.bank.admin.module.owner.vo.CardOwnerVO;

/**
 * 持卡人 Service 接口
 */
public interface CardOwnerService {

    PageResult<CardOwnerVO> page(CardOwnerQueryDTO query);

    CardOwnerVO detail(Long id);

    void save(CardOwnerSaveDTO dto);

    void update(CardOwnerSaveDTO dto);

    void delete(Long id);
}
