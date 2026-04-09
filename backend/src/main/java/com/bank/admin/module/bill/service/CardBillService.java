package com.bank.admin.module.bill.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.vo.CardBillVO;

import java.io.OutputStream;

/**
 * 账单 Service 接口
 */
public interface CardBillService {

    PageResult<CardBillVO> page(CardBillQueryDTO query);

    CardBillVO detail(Long id);

    void save(CardBillSaveDTO dto);

    void update(CardBillSaveDTO dto);

    void delete(Long id);

    void markRepaid(Long id, java.math.BigDecimal actualPayAmount, java.time.LocalDate actualPayDate);

    void exportExcel(CardBillQueryDTO query, OutputStream out);
}
