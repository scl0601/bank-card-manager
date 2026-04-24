package com.bank.admin.module.bill.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.bill.dto.BillDetailSaveDTO;
import com.bank.admin.module.bill.vo.BillDetailVO;

import java.util.List;

/**
 * 账单明细 Service 接口
 */
public interface BillDetailService {

    List<BillDetailVO> listByBillId(Long billId);

    void save(BillDetailSaveDTO dto);

    void update(BillDetailSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void batchUpdateType(List<Long> ids, Integer detailType);
}
