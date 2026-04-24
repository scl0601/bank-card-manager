package com.bank.admin.module.profit.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.profit.dto.ProfitQueryDTO;
import com.bank.admin.module.profit.vo.CardProfitVO;
import com.bank.admin.module.profit.vo.MonthlyProfitVO;
import com.bank.admin.module.profit.vo.ProfitOverviewVO;
import com.bank.admin.module.profit.vo.UserProfitVO;

import java.util.List;

/**
 * 收益统计 Service
 */
public interface ProfitStatsService {

    ProfitOverviewVO getOverview(ProfitQueryDTO query);

    PageResult<UserProfitVO> pageUserProfit(ProfitQueryDTO query);

    PageResult<CardProfitVO> pageCardProfit(ProfitQueryDTO query);

    List<MonthlyProfitVO> listMonthlyProfit(ProfitQueryDTO query);
}
