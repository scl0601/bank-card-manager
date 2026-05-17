package com.bank.admin.module.monitor.service;

import com.bank.admin.module.monitor.vo.MonitorTodayVO;

import java.time.LocalDate;

public interface MonitorService {

    MonitorTodayVO getToday(LocalDate date);
}
