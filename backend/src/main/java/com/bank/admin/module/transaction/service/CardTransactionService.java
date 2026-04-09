package com.bank.admin.module.transaction.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.transaction.dto.CardTransactionQueryDTO;
import com.bank.admin.module.transaction.dto.CardTransactionSaveDTO;
import com.bank.admin.module.transaction.vo.CardTransactionVO;

import java.io.OutputStream;
import java.util.List;

/**
 * 流水 Service 接口
 */
public interface CardTransactionService {

    PageResult<CardTransactionVO> page(CardTransactionQueryDTO query);

    void save(CardTransactionSaveDTO dto);

    void update(CardTransactionSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void exportExcel(CardTransactionQueryDTO query, OutputStream out);
}
