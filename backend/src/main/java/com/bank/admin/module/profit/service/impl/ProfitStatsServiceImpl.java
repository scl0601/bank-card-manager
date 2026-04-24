package com.bank.admin.module.profit.service.impl;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.profit.dto.ProfitQueryDTO;
import com.bank.admin.module.profit.mapper.ProfitStatsMapper;
import com.bank.admin.module.profit.service.ProfitStatsService;
import com.bank.admin.module.profit.vo.CardProfitVO;
import com.bank.admin.module.profit.vo.MonthlyProfitVO;
import com.bank.admin.module.profit.vo.ProfitOverviewVO;
import com.bank.admin.module.profit.vo.UserProfitVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收益统计 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class ProfitStatsServiceImpl implements ProfitStatsService {

    private final ProfitStatsMapper profitStatsMapper;

    @Override
    public ProfitOverviewVO getOverview(ProfitQueryDTO query) {
        ProfitQueryDTO normalized = normalizeQuery(query);
        ProfitOverviewVO vo = profitStatsMapper.selectOverview(normalized);
        if (vo == null) {
            vo = new ProfitOverviewVO();
        }
        vo.setYear(normalized.getYear());
        vo.setMonth(normalized.getMonth());
        vo.setUserCount(defaultZero(vo.getUserCount()));
        vo.setCardCount(defaultZero(vo.getCardCount()));
        vo.setTotalBillAmount(defaultZero(vo.getTotalBillAmount()));
        vo.setTotalFeeAmount(defaultZero(vo.getTotalFeeAmount()));
        vo.setTotalPosCostAmount(defaultZero(vo.getTotalPosCostAmount()));
        vo.setTotalNetProfit(defaultZero(vo.getTotalNetProfit()));
        return vo;
    }

    @Override
    public PageResult<UserProfitVO> pageUserProfit(ProfitQueryDTO query) {
        ProfitQueryDTO normalized = normalizeQuery(query);
        Page<UserProfitVO> page = new Page<>(normalized.getCurrent(), normalized.getSize());
        IPage<UserProfitVO> result = profitStatsMapper.selectUserProfitPage(page, normalized);
        normalizeUserRows(result.getRecords());
        return PageResult.of((Page<UserProfitVO>) result);
    }

    @Override
    public PageResult<CardProfitVO> pageCardProfit(ProfitQueryDTO query) {
        ProfitQueryDTO normalized = normalizeQuery(query);
        Page<CardProfitVO> page = new Page<>(normalized.getCurrent(), normalized.getSize());
        IPage<CardProfitVO> result = profitStatsMapper.selectCardProfitPage(page, normalized);
        normalizeCardRows(result.getRecords());
        return PageResult.of((Page<CardProfitVO>) result);
    }

    @Override
    public List<MonthlyProfitVO> listMonthlyProfit(ProfitQueryDTO query) {
        ProfitQueryDTO normalized = normalizeQuery(query);
        List<MonthlyProfitVO> rows = profitStatsMapper.selectMonthlyProfit(normalized);
        Map<String, MonthlyProfitVO> monthMap = new LinkedHashMap<>();
        if (normalized.getMonth() != null) {
            String billMonth = buildBillMonth(normalized.getYear(), normalized.getMonth());
            monthMap.put(billMonth, buildEmptyMonth(billMonth));
        } else {
            for (int month = 1; month <= 12; month++) {
                String billMonth = buildBillMonth(normalized.getYear(), month);
                monthMap.put(billMonth, buildEmptyMonth(billMonth));
            }
        }
        for (MonthlyProfitVO row : rows) {
            MonthlyProfitVO target = monthMap.getOrDefault(row.getBillMonth(), buildEmptyMonth(row.getBillMonth()));
            target.setUserCount(defaultZero(row.getUserCount()));
            target.setCardCount(defaultZero(row.getCardCount()));
            target.setTotalBillAmount(defaultZero(row.getTotalBillAmount()));
            target.setTotalFeeAmount(defaultZero(row.getTotalFeeAmount()));
            target.setTotalPosCostAmount(defaultZero(row.getTotalPosCostAmount()));
            target.setTotalNetProfit(defaultZero(row.getTotalNetProfit()));
            monthMap.put(target.getBillMonth(), target);
        }
        return new ArrayList<>(monthMap.values());
    }

    private ProfitQueryDTO normalizeQuery(ProfitQueryDTO query) {
        ProfitQueryDTO target = query == null ? new ProfitQueryDTO() : query;
        if (target.getYear() == null) {
            target.setYear(Year.now().getValue());
        }
        if (target.getMonth() != null && (target.getMonth() < 1 || target.getMonth() > 12)) {
            target.setMonth(null);
        }
        return target;
    }

    private void normalizeUserRows(List<UserProfitVO> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(row -> {
            row.setCardCount(defaultZero(row.getCardCount()));
            row.setBillMonthCount(defaultZero(row.getBillMonthCount()));
            row.setTotalBillAmount(defaultZero(row.getTotalBillAmount()));
            row.setTotalFeeAmount(defaultZero(row.getTotalFeeAmount()));
            row.setTotalPosCostAmount(defaultZero(row.getTotalPosCostAmount()));
            row.setTotalNetProfit(defaultZero(row.getTotalNetProfit()));
        });
    }

    private void normalizeCardRows(List<CardProfitVO> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(row -> {
            row.setBillCount(defaultZero(row.getBillCount()));
            row.setTotalBillAmount(defaultZero(row.getTotalBillAmount()));
            row.setTotalFeeAmount(defaultZero(row.getTotalFeeAmount()));
            row.setTotalPosCostAmount(defaultZero(row.getTotalPosCostAmount()));
            row.setTotalNetProfit(defaultZero(row.getTotalNetProfit()));
        });
    }

    private MonthlyProfitVO buildEmptyMonth(String billMonth) {
        MonthlyProfitVO vo = new MonthlyProfitVO();
        vo.setBillMonth(billMonth);
        vo.setUserCount(0L);
        vo.setCardCount(0L);
        vo.setTotalBillAmount(BigDecimal.ZERO);
        vo.setTotalFeeAmount(BigDecimal.ZERO);
        vo.setTotalPosCostAmount(BigDecimal.ZERO);
        vo.setTotalNetProfit(BigDecimal.ZERO);
        return vo;
    }

    private String buildBillMonth(Integer year, Integer month) {
        return String.format("%04d-%02d", year, month);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
