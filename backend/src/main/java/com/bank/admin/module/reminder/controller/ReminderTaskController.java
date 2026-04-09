package com.bank.admin.module.reminder.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.reminder.dto.ReminderTaskQueryDTO;
import com.bank.admin.module.reminder.service.ReminderTaskService;
import com.bank.admin.module.reminder.vo.ReminderTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提醒中心 Controller
 */
@Tag(name = "提醒中心", description = "到期提醒、逾期提醒、卡片过期提醒")
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderTaskController {

    private final ReminderTaskService reminderTaskService;

    /**
     * 分页查询提醒任务
     */
    @Operation(summary = "分页查询提醒任务")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<PageResult<ReminderTaskVO>> page(@Valid ReminderTaskQueryDTO query) {
        return Result.success(reminderTaskService.page(query));
    }

    /**
     * 标记已处理
     */
    @Operation(summary = "标记为已处理")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "标记提醒已处理[id=#id]")
    @PatchMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> markHandled(@PathVariable Long id) {
        reminderTaskService.markHandled(id);
        return Result.success();
    }

    /**
     * 标记已忽略
     */
    @Operation(summary = "标记为已忽略")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "标记提醒已忽略[id=#id]")
    @PatchMapping("/{id}/ignore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> markIgnored(@PathVariable Long id) {
        reminderTaskService.markIgnored(id);
        return Result.success();
    }

    /**
     * 手动触发账单到期扫描（ADMIN 专用，方便测试）
     */
    @Operation(summary = "手动触发账单到期扫描")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "手动触发账单到期扫描")
    @PostMapping("/scan/bill")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> scanBill() {
        reminderTaskService.scanBillDueReminders();
        return Result.success();
    }

    /**
     * 手动触发卡片即将过期扫描（ADMIN 专用，方便测试）
     */
    @Operation(summary = "手动触发卡片过期扫描")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "手动触发卡片过期扫描")
    @PostMapping("/scan/card")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> scanCard() {
        reminderTaskService.scanCardExpiringReminders();
        return Result.success();
    }

    /**
     * 批量标记已处理
     */
    @Operation(summary = "批量标记已处理")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "批量标记提醒已处理")
    @PatchMapping("/batch/handle")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> batchHandle(@RequestBody List<Long> ids) {
        reminderTaskService.batchMarkHandled(ids);
        return Result.success();
    }

    /**
     * 批量标记已忽略
     */
    @Operation(summary = "批量标记已忽略")
    @Log(module = "提醒中心", type = ActionTypeEnum.OTHER, description = "批量标记提醒已忽略")
    @PatchMapping("/batch/ignore")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> batchIgnore(@RequestBody List<Long> ids) {
        reminderTaskService.batchMarkIgnored(ids);
        return Result.success();
    }
}
