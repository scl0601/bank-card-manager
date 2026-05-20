package com.bank.admin.module.announcement.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.announcement.dto.AnnouncementQueryDTO;
import com.bank.admin.module.announcement.dto.AnnouncementSaveDTO;
import com.bank.admin.module.announcement.service.SystemAnnouncementService;
import com.bank.admin.module.announcement.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统公告")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class SystemAnnouncementController {

    private final SystemAnnouncementService announcementService;

    @Operation(summary = "查询最新已发布公告")
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<AnnouncementVO> latest() {
        return Result.success(announcementService.latest());
    }

    @Operation(summary = "分页查询公告历史")
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<PageResult<AnnouncementVO>> history(@Valid AnnouncementQueryDTO query) {
        return Result.success(announcementService.history(query));
    }

    @Operation(summary = "查询未读公告数量")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<Integer> unreadCount() {
        return Result.success(announcementService.unreadCount());
    }

    @Operation(summary = "标记公告已读")
    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<Void> markRead(@PathVariable Long id) {
        announcementService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "记录公告今日已弹")
    @PostMapping("/{id}/popup-shown")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<Void> markPopupShown(@PathVariable Long id) {
        announcementService.markPopupShown(id);
        return Result.success();
    }

    @Operation(summary = "今日不再弹出公告")
    @PostMapping("/{id}/silent-today")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER','MONITOR')")
    public Result<Void> silentToday(@PathVariable Long id) {
        announcementService.silentToday(id);
        return Result.success();
    }

    @Operation(summary = "公告管理分页查询")
    @GetMapping("/admin/page")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<PageResult<AnnouncementVO>> adminPage(@Valid AnnouncementQueryDTO query) {
        return Result.success(announcementService.adminPage(query));
    }

    @Operation(summary = "新增公告")
    @Log(module = "系统公告", type = ActionTypeEnum.INSERT, description = "新增公告")
    @PostMapping("/admin")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<Long> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        return Result.success(announcementService.create(dto));
    }

    @Operation(summary = "编辑公告")
    @Log(module = "系统公告", type = ActionTypeEnum.UPDATE, description = "编辑公告[id=#id]")
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementSaveDTO dto) {
        announcementService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "发布公告")
    @Log(module = "系统公告", type = ActionTypeEnum.OTHER, description = "发布公告[id=#id]")
    @PatchMapping("/admin/{id}/publish")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<Void> publish(@PathVariable Long id) {
        announcementService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下线公告")
    @Log(module = "系统公告", type = ActionTypeEnum.OTHER, description = "下线公告[id=#id]")
    @PatchMapping("/admin/{id}/offline")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<Void> offline(@PathVariable Long id) {
        announcementService.offline(id);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @Log(module = "系统公告", type = ActionTypeEnum.DELETE, description = "删除公告[id=#id]")
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('MONITOR')")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }
}
