package com.bank.admin.module.calendar.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.calendar.dto.CalendarEventQueryDTO;
import com.bank.admin.module.calendar.dto.CalendarEventSaveDTO;
import com.bank.admin.module.calendar.service.CalendarEventService;
import com.bank.admin.module.calendar.vo.CalendarEventVO;
import com.bank.admin.module.calendar.vo.CalendarStatsVO;
import com.bank.admin.module.calendar.vo.CalendarYearStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 日历计划 Controller
 */
@Tag(name = "日历计划", description = "日程事项管理接口")
@RestController
@RequestMapping("/api/calendar/events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    /**
     * 获取某月的所有事项（日历标记用）
     */
    @Operation(summary = "获取月度事项列表")
    @GetMapping("/month")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<List<CalendarEventVO>> listByMonth(@Valid CalendarEventQueryDTO query) {
        return Result.success(calendarEventService.listByMonth(query.getMonth()));
    }

    /**
     * 获取某日的所有事项
     */
    @Operation(summary = "获取指定日期的事项列表")
    @GetMapping("/day")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<List<CalendarEventVO>> listByDay(@Valid CalendarEventQueryDTO query) {
        return Result.success(calendarEventService.listByDay(
                query.getEventDate(), query.getCategory(), query.getStatus()));
    }

    /**
     * 获取单个事项详情
     */
    @Operation(summary = "获取事项详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<CalendarEventVO> getDetail(@PathVariable Long id) {
        return Result.success(calendarEventService.getDetail(id));
    }

    /**
     * 新增事项
     */
    @Operation(summary = "新增日程事项")
    @Log(module = "日历计划", type = ActionTypeEnum.INSERT, description = "新增日程事项[title=#title]")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Long> save(@Valid @RequestBody CalendarEventSaveDTO dto) {
        return Result.success(calendarEventService.save(dto));
    }

    /**
     * 编辑事项
     */
    @Operation(summary = "编辑日程事项")
    @Log(module = "日历计划", type = ActionTypeEnum.UPDATE, description = "编辑日程事项[id=#id]")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CalendarEventSaveDTO dto) {
        calendarEventService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除事项
     */
    @Operation(summary = "删除日程事项")
    @Log(module = "日历计划", type = ActionTypeEnum.DELETE, description = "删除日程事项[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> delete(@PathVariable Long id) {
        calendarEventService.delete(id);
        return Result.success();
    }

    /**
     * 更新事项状态
     */
    @Operation(summary = "更新事项状态")
    @Log(module = "日历计划", type = ActionTypeEnum.OTHER, description = "更新事项状态[id=#id][status=#status]")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        calendarEventService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取当月统计
     */
    @Operation(summary = "获取月度统计数据")
    @GetMapping("/stats/month")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<CalendarStatsVO> getMonthStats(@RequestParam String month) {
        return Result.success(calendarEventService.getMonthStats(month));
    }

    /**
     * 获取年度统计
     */
    @Operation(summary = "获取年度统计数据")
    @GetMapping("/stats/year")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<CalendarYearStatsVO> getYearStats(@RequestParam String year) {
        return Result.success(calendarEventService.getYearStats(year));
    }
}
