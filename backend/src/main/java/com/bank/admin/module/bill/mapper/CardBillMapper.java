package com.bank.admin.module.bill.mapper;

import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.vo.CardBillOverviewVO;
import com.bank.admin.module.bill.vo.CardBillVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 账单 Mapper
 */
@Mapper
public interface CardBillMapper extends BaseMapper<CardBill> {

    IPage<CardBillVO> selectPageWithInfo(
            Page<CardBillVO> page,
            @Param("cardId") Long cardId,
            @Param("ownerId") Long ownerId,
            @Param("cardName") String cardName,
            @Param("billMonth") String billMonth,
            @Param("year") Integer year,
            @Param("status") Integer status
    );

    CardBillOverviewVO selectOverview(
            @Param("cardId") Long cardId,
            @Param("ownerId") Long ownerId,
            @Param("cardName") String cardName,
            @Param("billMonth") String billMonth,
            @Param("year") Integer year,
            @Param("status") Integer status
    );

    /**
     * 查询即将到期账单（还款日在未来7天内，状态为待还款或部分还款）
     */
    List<Map<String, Object>> selectUpcomingBills(@Param("today") LocalDate today);
}
