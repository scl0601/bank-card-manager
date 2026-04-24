package com.bank.admin.module.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.admin.module.bill.entity.BillDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账单明细 Mapper
 */
@Mapper
public interface BillDetailMapper extends BaseMapper<BillDetail> {
}
