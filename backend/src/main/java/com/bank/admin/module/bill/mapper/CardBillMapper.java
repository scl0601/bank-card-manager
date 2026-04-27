package com.bank.admin.module.bill.mapper;

import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.vo.CardBillOverviewVO;
import com.bank.admin.module.bill.vo.CardBillVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 账单 Mapper
 */
@Mapper
public interface CardBillMapper extends BaseMapper<CardBill> {

    @SelectProvider(type = CardBillSqlProvider.class, method = "selectPageWithInfo")
    IPage<CardBillVO> selectPageWithInfo(
            Page<CardBillVO> page,
            @Param("query") CardBillQueryDTO query,
            @Param("cardIds") List<Long> cardIds,
            @Param("currentMonth") String currentMonth
    );

    @SelectProvider(type = CardBillSqlProvider.class, method = "selectOverview")
    CardBillOverviewVO selectOverview(
            @Param("query") CardBillQueryDTO query,
            @Param("cardIds") List<Long> cardIds,
            @Param("currentMonth") String currentMonth
    );

    /**
     * 查询即将到期账单（还款日在未来7天内，状态为待还款或部分还款）
     */
    @SelectProvider(type = CardBillSqlProvider.class, method = "selectUpcomingBills")
    List<Map<String, Object>> selectUpcomingBills(@Param("today") LocalDate today);
}
