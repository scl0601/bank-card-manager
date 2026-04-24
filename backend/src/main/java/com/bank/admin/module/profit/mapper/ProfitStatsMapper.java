package com.bank.admin.module.profit.mapper;

import com.bank.admin.module.profit.dto.ProfitQueryDTO;
import com.bank.admin.module.profit.vo.CardProfitVO;
import com.bank.admin.module.profit.vo.MonthlyProfitVO;
import com.bank.admin.module.profit.vo.ProfitOverviewVO;
import com.bank.admin.module.profit.vo.UserProfitVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收益统计 Mapper
 */
@Mapper
public interface ProfitStatsMapper {

    ProfitOverviewVO selectOverview(@Param("query") ProfitQueryDTO query);

    IPage<UserProfitVO> selectUserProfitPage(Page<UserProfitVO> page, @Param("query") ProfitQueryDTO query);

    IPage<CardProfitVO> selectCardProfitPage(Page<CardProfitVO> page, @Param("query") ProfitQueryDTO query);

    List<MonthlyProfitVO> selectMonthlyProfit(@Param("query") ProfitQueryDTO query);
}
