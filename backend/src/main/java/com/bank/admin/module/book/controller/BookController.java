package com.bank.admin.module.book.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.book.dto.BookQueryDTO;
import com.bank.admin.module.book.dto.BookSaveDTO;
import com.bank.admin.module.book.dto.CategorySaveDTO;
import com.bank.admin.module.book.service.BookService;
import com.bank.admin.module.book.vo.BookCategoryVO;
import com.bank.admin.module.book.vo.BookVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 个人记账 Controller
 */
@Tag(name = "个人记账")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // ==================== 记账 CRUD ====================

    @Operation(summary = "分页查询记账记录")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<BookVO>> page(@Valid BookQueryDTO query) {
        return Result.success(bookService.page(query));
    }

    @Operation(summary = "新增记账记录")
    @Log(module = "个人记账", type = ActionTypeEnum.INSERT, description = "新增记账记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> save(@Valid @RequestBody BookSaveDTO dto) {
        bookService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑记账记录")
    @Log(module = "个人记账", type = ActionTypeEnum.UPDATE, description = "编辑记账记录")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody BookSaveDTO dto) {
        bookService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除记账记录")
    @Log(module = "个人记账", type = ActionTypeEnum.DELETE, description = "删除记账记录[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除记账记录")
    @Log(module = "个人记账", type = ActionTypeEnum.DELETE, description = "批量删除记账记录")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        bookService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "导出记账Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public void export(BookQueryDTO query, HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode("个人记账", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        bookService.exportExcel(query, response.getOutputStream());
    }

    @Operation(summary = "获取月度收支统计")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return Result.success(bookService.getSummary(yearMonth));
    }

    // ==================== 分类管理 ====================

    @Operation(summary = "查询分类列表")
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<BookCategoryVO>> categories(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "true") Boolean enabledOnly) {
        return Result.success(bookService.listCategory(type, enabledOnly));
    }

    @Operation(summary = "新增分类")
    @Log(module = "个人记账", type = ActionTypeEnum.INSERT, description = "新增记账分类")
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> saveCategory(@Valid @RequestBody CategorySaveDTO dto) {
        bookService.saveCategory(dto);
        return Result.success();
    }

    @Operation(summary = "编辑分类")
    @Log(module = "个人记账", type = ActionTypeEnum.UPDATE, description = "编辑记账分类")
    @PutMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateCategory(@Valid @RequestBody CategorySaveDTO dto) {
        bookService.updateCategory(dto);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @Log(module = "个人记账", type = ActionTypeEnum.DELETE, description = "删除记账分类")
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        bookService.deleteCategory(id);
        return Result.success();
    }
}
