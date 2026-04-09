package com.bank.admin.module.calendar.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.calendar.dto.CalendarEventQueryDTO;
import com.bank.admin.module.calendar.dto.CalendarEventSaveDTO;
import com.bank.admin.module.calendar.entity.CalendarEvent;
import com.bank.admin.module.calendar.vo.CalendarEventVO;
import com.bank.admin.module.calendar.vo.CalendarStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 日程事项 Service 接口
 */
public interface CalendarEventService {

    /**
     * 获取某月所有事项（用于日历标记）
     */
    List<CalendarEventVO> listByMonth(String month);

    /**
     * 获取某日所有事项
     */
    List<CalendarEventVO> listByDay(LocalDate eventDate, Integer category, Integer status);

    /**
     * 获取单个详情
     */
    CalendarEventVO getDetail(Long id);

    /**
     * 新增事项
     */
    Long save(CalendarEventSaveDTO dto);

    /**
     * 编辑事项
     */
    void update(Long id, CalendarEventSaveDTO dto);

    /**
     * 删除事项
     */
    void delete(Long id);

    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取当月统计
     */
    CalendarStatsVO getMonthStats(String month);
}
