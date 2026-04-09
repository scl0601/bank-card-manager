package com.bank.admin.module.book.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.book.dto.BookQueryDTO;
import com.bank.admin.module.book.dto.BookSaveDTO;
import com.bank.admin.module.book.dto.CategorySaveDTO;
import com.bank.admin.module.book.entity.BookCategory;
import com.bank.admin.module.book.vo.BookCategoryVO;
import com.bank.admin.module.book.vo.BookVO;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 记账 Service 接口
 */
public interface BookService {

    PageResult<BookVO> page(BookQueryDTO query);

    void save(BookSaveDTO dto);

    void update(BookSaveDTO dto);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void exportExcel(BookQueryDTO query, OutputStream out);

    Map<String, Object> getSummary(String yearMonth);

    List<BookCategoryVO> listCategory(Integer type, Boolean enabledOnly);
    void saveCategory(CategorySaveDTO dto);
    void updateCategory(CategorySaveDTO dto);
    void deleteCategory(Long id);
}
