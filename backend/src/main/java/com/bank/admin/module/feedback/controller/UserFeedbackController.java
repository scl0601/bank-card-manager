package com.bank.admin.module.feedback.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.feedback.dto.FeedbackCreateDTO;
import com.bank.admin.module.feedback.dto.FeedbackQueryDTO;
import com.bank.admin.module.feedback.dto.FeedbackRemarkDTO;
import com.bank.admin.module.feedback.dto.FeedbackStatusUpdateDTO;
import com.bank.admin.module.feedback.dto.FeedbackUpdateDTO;
import com.bank.admin.module.feedback.service.UserFeedbackService;
import com.bank.admin.module.feedback.vo.FeedbackStatsVO;
import com.bank.admin.module.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户反馈 Controller
 */
@Tag(name = "用户反馈", description = "反馈提交、状态跟踪、处理备注管理")
@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class UserFeedbackController {

    private final UserFeedbackService feedbackService;

    /**
     * 分页查询反馈列表
     */
    @Operation(summary = "分页查询反馈列表")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<PageResult<FeedbackVO>> page(@Valid FeedbackQueryDTO query) {
        return Result.success(feedbackService.page(query));
    }

    /**
     * 反馈详情（含附件 + 处理轨迹）
     */
    @Operation(summary = "反馈详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        return Result.success(feedbackService.detail(id));
    }

    /**
     * 提交反馈（multipart/form-data）
     */
    @Operation(summary = "提交反馈")
    @Log(module = "用户反馈", type = ActionTypeEnum.INSERT, description = "提交反馈")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<Long> create(
        @Valid @RequestPart("data") FeedbackCreateDTO dto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return Result.success(feedbackService.create(dto, files));
    }

    /**
     * 编辑反馈（multipart/form-data）
     */
    @Operation(summary = "编辑反馈")
    @Log(module = "用户反馈", type = ActionTypeEnum.UPDATE, description = "编辑反馈[id=#id]")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> update(
        @PathVariable Long id,
        @Valid @RequestPart("data") FeedbackUpdateDTO dto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        feedbackService.update(id, dto, files);
        return Result.success();
    }

    /**
     * 修改状态
     */
    @Operation(summary = "修改反馈状态")
    @Log(module = "用户反馈", type = ActionTypeEnum.UPDATE, description = "修改反馈状态[id=#id]")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody FeedbackStatusUpdateDTO dto
    ) {
        feedbackService.updateStatus(id, dto);
        return Result.success();
    }

    /**
     * 追加处理备注
     */
    @Operation(summary = "追加处理备注")
    @Log(module = "用户反馈", type = ActionTypeEnum.OTHER, description = "追加处理备注[id=#id]")
    @PostMapping("/{id}/remarks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> addRemark(
        @PathVariable Long id,
        @Valid @RequestBody FeedbackRemarkDTO dto
    ) {
        feedbackService.addRemark(id, dto);
        return Result.success();
    }

    /**
     * 统计数据（顶部卡片）
     */
    @Operation(summary = "反馈统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public Result<FeedbackStatsVO> stats() {
        return Result.success(feedbackService.stats());
    }

    /**
     * 附件下载
     */
    @Operation(summary = "附件下载")
    @Log(module = "用户反馈", type = ActionTypeEnum.OTHER, description = "下载附件[id=#attachmentId]")
    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Resource> downloadAttachment(
        @PathVariable Long attachmentId,
        @Parameter(description = "原始文件名（用于响应头）") @RequestParam(required = false) String fileName
    ) {
        String filePath = feedbackService.getAttachmentFilePath(attachmentId);
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        String encodedName = URLEncoder.encode(
            fileName != null ? fileName : file.getName(), StandardCharsets.UTF_8
        ).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    /**
     * 附件预览
     */
    @Operation(summary = "附件预览")
    @GetMapping("/attachments/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Resource> previewAttachment(
        @Parameter(description = "附件相对路径") @RequestParam String path
    ) {
        String safePath = path == null ? "" : path.replace("\\", "/");
        if (safePath.isBlank() || safePath.contains("..") || safePath.startsWith("/") || safePath.contains(":")) {
            return ResponseEntity.badRequest().build();
        }

        File rootDir = new File(System.getProperty("user.home"), "bank-uploads/feedback");
        File file = new File(rootDir, safePath);
        try {
            String rootCanonicalPath = rootDir.getCanonicalPath();
            String fileCanonicalPath = file.getCanonicalPath();
            if (!fileCanonicalPath.startsWith(rootCanonicalPath + File.separator)) {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            log.warn("预览附件路径解析失败: {}", safePath, e);
            return ResponseEntity.badRequest().build();
        }

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getName())
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
            .contentType(mediaType)
            .body(resource);
    }

    /**
     * 删除附件
     */
    @Operation(summary = "删除附件")
    @Log(module = "用户反馈", type = ActionTypeEnum.DELETE, description = "删除附件[id=#attachmentId]")
    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId) {
        feedbackService.deleteAttachment(attachmentId);
        return Result.success();
    }

    /**
     * 逻辑删除反馈
     */
    @Operation(summary = "删除反馈")
    @Log(module = "用户反馈", type = ActionTypeEnum.DELETE, description = "删除反馈[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return Result.success();
    }
}
