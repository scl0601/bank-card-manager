package com.bank.admin.module.monitor.service.impl;

import com.bank.admin.module.calendar.entity.CalendarEvent;
import com.bank.admin.module.calendar.mapper.CalendarEventMapper;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.mapper.OperationLogMapper;
import com.bank.admin.module.monitor.service.MonitorService;
import com.bank.admin.module.monitor.vo.MonitorSummaryVO;
import com.bank.admin.module.monitor.vo.MonitorTimelineItemVO;
import com.bank.admin.module.monitor.vo.MonitorTodayVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private static final String TYPE_OPERATION = "OPERATION";
    private static final String TYPE_CALENDAR = "CALENDAR";
    private static final int CALENDAR_STATUS_DONE = 2;

    private final OperationLogMapper operationLogMapper;
    private final CalendarEventMapper calendarEventMapper;

    @Override
    public MonitorTodayVO getToday(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<OperationLog> operationLogs = operationLogMapper.selectList(
                Wrappers.<OperationLog>lambdaQuery()
                        .ge(OperationLog::getCreateTime, start)
                        .lt(OperationLog::getCreateTime, end)
                        .orderByDesc(OperationLog::getCreateTime)
        );

        List<CalendarEvent> calendarEvents = calendarEventMapper.selectList(
                Wrappers.<CalendarEvent>lambdaQuery()
                        .eq(CalendarEvent::getEventDate, targetDate)
                        .orderByAsc(CalendarEvent::getStartTime)
                        .orderByAsc(CalendarEvent::getCreateTime)
        );

        List<MonitorTimelineItemVO> timeline = new ArrayList<>();
        operationLogs.forEach(log -> timeline.add(toOperationItem(log)));
        calendarEvents.forEach(event -> timeline.add(toCalendarItem(event, targetDate)));
        timeline.sort(Comparator.comparing(MonitorTimelineItemVO::getTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        MonitorSummaryVO summary = new MonitorSummaryVO();
        summary.setOperationCount(operationLogs.size());
        summary.setSuccessCount(operationLogs.stream().filter(log -> Integer.valueOf(0).equals(log.getResult())).count());
        summary.setFailCount(operationLogs.stream().filter(log -> Integer.valueOf(1).equals(log.getResult())).count());
        summary.setCalendarCount(calendarEvents.size());
        summary.setCompletedCalendarCount(calendarEvents.stream()
                .filter(event -> Integer.valueOf(CALENDAR_STATUS_DONE).equals(event.getStatus()))
                .count());

        MonitorTodayVO vo = new MonitorTodayVO();
        vo.setDate(targetDate);
        vo.setSummary(summary);
        vo.setTimeline(timeline);
        return vo;
    }

    private MonitorTimelineItemVO toOperationItem(OperationLog log) {
        MonitorTimelineItemVO item = new MonitorTimelineItemVO();
        item.setType(TYPE_OPERATION);
        item.setOperator(log.getOperator());
        item.setTime(log.getCreateTime());
        item.setModule(log.getModule());
        item.setAction(log.getAction());
        item.setTitle(buildOperationTitle(log));
        item.setDescription(log.getDescription());
        item.setResult(log.getResult());
        item.setSourceId(log.getId());
        return item;
    }

    private String buildOperationTitle(OperationLog log) {
        String module = log.getModule() != null ? log.getModule() : "系统操作";
        String action = log.getAction() != null ? log.getAction() : "";
        return action.isBlank() ? module : module + " - " + action;
    }

    private MonitorTimelineItemVO toCalendarItem(CalendarEvent event, LocalDate targetDate) {
        MonitorTimelineItemVO item = new MonitorTimelineItemVO();
        item.setType(TYPE_CALENDAR);
        item.setOperator(resolveCalendarOperator(event));
        item.setTime(resolveCalendarTime(event, targetDate));
        item.setModule("日历计划");
        item.setAction(resolveCalendarAction(event));
        item.setTitle(event.getTitle());
        item.setDescription(event.getDescription());
        item.setResult(event.getStatus());
        item.setSourceId(event.getId());
        return item;
    }

    private String resolveCalendarOperator(CalendarEvent event) {
        if (event.getUpdateBy() != null && !event.getUpdateBy().isBlank()) {
            return event.getUpdateBy();
        }
        if (event.getCreateBy() != null && !event.getCreateBy().isBlank()) {
            return event.getCreateBy();
        }
        return "system";
    }

    private LocalDateTime resolveCalendarTime(CalendarEvent event, LocalDate targetDate) {
        LocalTime startTime = event.getStartTime();
        if (startTime != null) {
            return LocalDateTime.of(targetDate, startTime);
        }
        if (event.getUpdateTime() != null) {
            return event.getUpdateTime();
        }
        if (event.getCreateTime() != null) {
            return event.getCreateTime();
        }
        return targetDate.atStartOfDay();
    }

    private String resolveCalendarAction(CalendarEvent event) {
        Integer status = event.getStatus();
        if (Integer.valueOf(0).equals(status)) {
            return "待办";
        }
        if (Integer.valueOf(1).equals(status)) {
            return "进行中";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已完成";
        }
        if (Integer.valueOf(3).equals(status)) {
            return "已取消";
        }
        return "计划";
    }
}
