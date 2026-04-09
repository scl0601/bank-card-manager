package com.bank.admin.module.book.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.module.book.dto.BookQueryDTO;
import com.bank.admin.module.book.dto.BookSaveDTO;
import com.bank.admin.module.book.dto.CategorySaveDTO;
import com.bank.admin.module.book.entity.BookCategory;
import com.bank.admin.module.book.entity.PersonalBook;
import com.bank.admin.module.book.mapper.BookMapper;
import com.bank.admin.module.book.service.BookService;
import com.bank.admin.module.book.vo.BookCategoryVO;
import com.bank.admin.module.book.vo.BookVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记账 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl
        extends ServiceImpl<BookMapper, PersonalBook>
        implements BookService {

    private final com.bank.admin.module.book.mapper.BookCategoryMapper bookCategoryMapper;

    @Override
    public PageResult<BookVO> page(BookQueryDTO query) {
        Page<BookVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<BookVO> result = (Page<BookVO>)
                baseMapper.selectPageWithInfo(page,
                        query.getBookType(), query.getCategoryIds(),
                        query.getCardId(), query.getBookDateStart(), query.getBookDateEnd());

        result.getRecords().forEach(vo ->
                vo.setBookTypeDesc(vo.getBookType() != null && vo.getBookType() == 1 ? "收入" : "支出"));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(BookSaveDTO dto) {
        // 校验分类是否存在且类型匹配
        BookCategory category = bookCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类不存在或已停用");
        }
        if (!category.getType().equals(dto.getBookType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类与记账类型不匹配");
        }

        PersonalBook entity = new PersonalBook();
        BeanUtils.copyProperties(dto, entity);
        super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BookSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        PersonalBook existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "记账记录不存在");
        }
        // 校验分类
        BookCategory category = bookCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类不存在或已停用");
        }
        if (!category.getType().equals(dto.getBookType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类与记账类型不匹配");
        }
        BeanUtils.copyProperties(dto, existing);
        updateById(existing);
    }

    @Override
    public void delete(Long id) {
        PersonalBook entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "记账记录不存在");
        }
        removeById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        removeByIds(ids);
    }

    @Override
    public void exportExcel(BookQueryDTO query, OutputStream out) {
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<BookVO> result = page(query);
        List<BookVO> records = result.getRecords();

        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("日期"));
        heads.add(List.of("类型"));
        heads.add(List.of("分类"));
        heads.add(List.of("金额"));
        heads.add(List.of("描述"));
        heads.add(List.of("关联卡"));

        List<List<Object>> dataList = new ArrayList<>();
        for (BookVO vo : records) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getBookDate());
            row.add(vo.getBookTypeDesc());
            row.add(vo.getCategoryName());
            row.add(vo.getAmount() != null ? vo.getAmount().toString() : "0");
            row.add(vo.getDescription());
            row.add((vo.getBankName() != null ? vo.getBankName() + " *" : "") +
                    (vo.getCardNoLast4() != null ? vo.getCardNoLast4() : ""));
            dataList.add(row);
        }

        EasyExcel.write(out)
                .head(heads)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("个人记账")
                .doWrite(dataList);
    }

    @Override
    public Map<String, Object> getSummary(String yearMonth) {
        return baseMapper.sumByMonth(yearMonth);
    }

    // ==================== 分类管理 ====================

    @Override
    public List<BookCategoryVO> listCategory(Integer type, Boolean enabledOnly) {
        List<BookCategory> all;
        if (Boolean.TRUE.equals(enabledOnly)) {
            all = bookCategoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BookCategory>()
                            .eq(BookCategory::getStatus, 0));
        } else {
            all = bookCategoryMapper.selectList(null);
        }
        return buildTree(all, type);
    }

    private List<BookCategoryVO> buildTree(List<BookCategory> all, Integer type) {
        List<BookCategory> filtered = all;
        if (type != null) {
            filtered = all.stream()
                    .filter(c -> type.equals(c.getType()))
                    .collect(Collectors.toList());
        }

        Map<Long, List<BookCategory>> childrenMap = filtered.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0L)
                .collect(Collectors.groupingBy(BookCategory::getParentId));

        List<BookCategoryVO> result = new ArrayList<>();
        for (BookCategory cat : filtered) {
            if (cat.getParentId() == null || cat.getParentId() == 0L) {
                BookCategoryVO vo = toVO(cat);
                List<BookCategory> children = childrenMap.get(cat.getId());
                if (children != null && !children.isEmpty()) {
                    vo.setChildren(children.stream()
                            .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                            .map(this::toVO)
                            .collect(Collectors.toList()));
                }
                result.add(vo);
            }
        }
        result.sort((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()));
        return result;
    }

    private BookCategoryVO toVO(BookCategory entity) {
        BookCategoryVO vo = new BookCategoryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategory(CategorySaveDTO dto) {
        BookCategory entity = new BookCategory();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) entity.setStatus(0);
        if (entity.getSortOrder() == null) entity.setSortOrder(0);
        bookCategoryMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        BookCategory existing = bookCategoryMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        BeanUtils.copyProperties(dto, existing);
        bookCategoryMapper.updateById(existing);
    }

    @Override
    public void deleteCategory(Long id) {
        // 检查是否有子分类
        long childCount = bookCategoryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BookCategory>()
                        .eq(BookCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该分类下存在子分类，无法删除");
        }
        // 检查是否有关联的记账记录
        long bookCount = this.count(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PersonalBook>()
                        .eq(PersonalBook::getCategoryId, id));
        if (bookCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该分类下存在记账记录，无法删除");
        }
        bookCategoryMapper.deleteById(id);
    }
}
