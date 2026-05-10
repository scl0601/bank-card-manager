package com.bank.admin.module.special.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpecialProfitStatsVO {
    private SpecialProfitOverviewVO overview;
    private List<SpecialProfitRowVO> rows;
    private List<SpecialProfitCardVO> cardStats;
    private List<SpecialProfitMonthVO> monthStats;
}
