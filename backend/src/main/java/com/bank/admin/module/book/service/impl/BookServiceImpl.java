package com.bank.admin.module.book.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.common.util.CurrentUserUtil;
import com.bank.admin.module.book.dto.*;
import com.bank.admin.module.book.entity.BookAccount;
import com.bank.admin.module.book.entity.BookBudget;
import com.bank.admin.module.book.entity.BookCategory;
import com.bank.admin.module.book.entity.PersonalBook;
import com.bank.admin.module.book.mapper.BookAccountMapper;
import com.bank.admin.module.book.mapper.BookBudgetMapper;
import com.bank.admin.module.book.mapper.BookMapper;
import com.bank.admin.module.book.service.BookService;
import com.bank.admin.module.book.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final BookAccountMapper bookAccountMapper;
    private final BookBudgetMapper bookBudgetMapper;

    @Override
    public PageResult<BookVO> page(BookQueryDTO query) {
        Page<BookVO> page = new Page<>(query.getCurrent(), query.getSize());
        Page<BookVO> result = (Page<BookVO>)
                baseMapper.selectPageWithInfo(page,
                        query.getBookType(), query.getCategoryIds(),
                        query.getCardId(), query.getAccountId(), normalizeMonth(query.getYearMonth()),
                        trimToNull(query.getKeyword()), currentUsername(), query.getBookDateStart(), query.getBookDateEnd());

        result.getRecords().forEach(vo ->
                vo.setBookTypeDesc(bookTypeDesc(vo.getBookType())));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(BookSaveDTO dto) {
        validateBook(dto);

        PersonalBook entity = new PersonalBook();
        BeanUtils.copyProperties(dto, entity);
        super.save(entity);
        applyAccountEffect(entity, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BookSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ID不能为空");
        }
        PersonalBook existing = getOwnedBook(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "记账记录不存在");
        }
        validateBook(dto);
        applyAccountEffect(existing, true);
        BeanUtils.copyProperties(dto, existing);
        updateById(existing);
        applyAccountEffect(existing, false);
    }

    @Override
    public void delete(Long id) {
        PersonalBook entity = getOwnedBook(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "记账记录不存在");
        }
        applyAccountEffect(entity, true);
        removeById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<PersonalBook> rows = list(new LambdaQueryWrapper<PersonalBook>()
                .in(PersonalBook::getId, ids)
                .eq(PersonalBook::getCreateBy, currentUsername()));
        rows.forEach(row -> applyAccountEffect(row, true));
        remove(new LambdaQueryWrapper<PersonalBook>()
                .in(PersonalBook::getId, ids)
                .eq(PersonalBook::getCreateBy, currentUsername()));
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
        heads.add(List.of("账户"));
        heads.add(List.of("转入账户"));
        heads.add(List.of("商家/对象"));
        heads.add(List.of("描述"));
        heads.add(List.of("关联卡"));

        List<List<Object>> dataList = new ArrayList<>();
        for (BookVO vo : records) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getBookDate());
            row.add(vo.getBookTypeDesc());
            row.add(vo.getCategoryName());
            row.add(vo.getAmount() != null ? vo.getAmount().toString() : "0");
            row.add(vo.getAccountName());
            row.add(vo.getTargetAccountName());
            row.add(vo.getMerchant());
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
        return baseMapper.sumByMonth(normalizeMonthOrCurrent(yearMonth), currentUsername());
    }

    @Override
    public BookOverviewVO getOverview(String yearMonth) {
        String month = normalizeMonthOrCurrent(yearMonth);
        Map<String, Object> summary = baseMapper.sumByMonth(month, currentUsername());
        BookOverviewVO vo = new BookOverviewVO();
        vo.setYearMonth(month);
        vo.setTotalIncome(decimal(summary.get("totalIncome")));
        vo.setTotalExpense(decimal(summary.get("totalExpense")));
        vo.setNetAmount(vo.getTotalIncome().subtract(vo.getTotalExpense()));
        vo.setAccounts(listAccounts(true));
        vo.setTotalAssets(vo.getAccounts().stream()
                .map(BookAccountVO::getCurrentBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setBudgets(listBudgets(month));
        BigDecimal totalBudget = vo.getBudgets().stream()
                .filter(item -> item.getCategoryId() == null)
                .findFirst()
                .map(BookBudgetVO::getAmount)
                .orElse(BigDecimal.ZERO);
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            totalBudget = vo.getBudgets().stream()
                    .filter(item -> item.getCategoryId() != null)
                    .map(BookBudgetVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        vo.setTotalBudget(totalBudget);
        vo.setBudgetUsed(vo.getTotalExpense());
        vo.setBudgetRemaining(totalBudget.subtract(vo.getTotalExpense()));
        vo.setBudgetUsagePercent(percent(vo.getTotalExpense(), totalBudget));
        vo.setCategoryRanks(toCategoryRanks(baseMapper.categoryRank(month, currentUsername())));
        return vo;
    }

    @Override
    public List<BookTrendVO> getTrend(String yearMonth) {
        return baseMapper.dailyTrend(normalizeMonthOrCurrent(yearMonth), currentUsername()).stream().map(row -> {
            BookTrendVO vo = new BookTrendVO();
            vo.setDate(String.valueOf(row.get("date")));
            vo.setIncome(decimal(row.get("income")));
            vo.setExpense(decimal(row.get("expense")));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BookCalendarDayVO> getCalendar(String yearMonth) {
        return baseMapper.calendarSummary(normalizeMonthOrCurrent(yearMonth), currentUsername()).stream().map(row -> {
            BookCalendarDayVO vo = new BookCalendarDayVO();
            vo.setDate(String.valueOf(row.get("date")));
            vo.setIncome(decimal(row.get("income")));
            vo.setExpense(decimal(row.get("expense")));
            vo.setCount(decimal(row.get("count")).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 分类管理 ====================

    @Override
    public List<BookCategoryVO> listCategory(Integer type, Boolean enabledOnly) {
        ensureDefaultCategories();
        List<BookCategory> all;
        if (Boolean.TRUE.equals(enabledOnly)) {
            all = bookCategoryMapper.selectList(
                    new LambdaQueryWrapper<BookCategory>()
                            .eq(BookCategory::getCreateBy, currentUsername())
                            .eq(BookCategory::getStatus, 0));
        } else {
            all = bookCategoryMapper.selectList(
                    new LambdaQueryWrapper<BookCategory>()
                            .eq(BookCategory::getCreateBy, currentUsername()));
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
        BookCategory existing = getOwnedCategory(dto.getId());
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
                new LambdaQueryWrapper<BookCategory>()
                        .eq(BookCategory::getParentId, id)
                        .eq(BookCategory::getCreateBy, currentUsername()));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该分类下存在子分类，无法删除");
        }
        // 检查是否有关联的记账记录
        long bookCount = this.count(
                new LambdaQueryWrapper<PersonalBook>()
                        .eq(PersonalBook::getCategoryId, id)
                        .eq(PersonalBook::getCreateBy, currentUsername()));
        if (bookCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该分类下存在记账记录，无法删除");
        }
        bookCategoryMapper.deleteById(id);
    }

    @Override
    public List<BookAccountVO> listAccounts(Boolean enabledOnly) {
        LambdaQueryWrapper<BookAccount> wrapper = new LambdaQueryWrapper<BookAccount>()
                .eq(BookAccount::getCreateBy, currentUsername())
                .orderByAsc(BookAccount::getSortOrder)
                .orderByAsc(BookAccount::getId);
        if (Boolean.TRUE.equals(enabledOnly)) {
            wrapper.eq(BookAccount::getStatus, 0);
        }
        return bookAccountMapper.selectList(wrapper).stream().map(this::toAccountVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAccount(BookAccountSaveDTO dto) {
        BookAccount entity = new BookAccount();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getInitialBalance() == null) entity.setInitialBalance(BigDecimal.ZERO);
        if (entity.getCurrentBalance() == null) entity.setCurrentBalance(entity.getInitialBalance());
        if (entity.getStatus() == null) entity.setStatus(0);
        if (entity.getSortOrder() == null) entity.setSortOrder(0);
        bookAccountMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(BookAccountSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账户ID不能为空");
        }
        BookAccount existing = getOwnedAccount(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        BeanUtils.copyProperties(dto, existing);
        if (existing.getInitialBalance() == null) existing.setInitialBalance(BigDecimal.ZERO);
        if (existing.getCurrentBalance() == null) existing.setCurrentBalance(existing.getInitialBalance());
        if (existing.getStatus() == null) existing.setStatus(0);
        if (existing.getSortOrder() == null) existing.setSortOrder(0);
        bookAccountMapper.updateById(existing);
    }

    @Override
    public void deleteAccount(Long id) {
        long count = count(new LambdaQueryWrapper<PersonalBook>()
                .eq(PersonalBook::getCreateBy, currentUsername())
                .and(wrapper -> wrapper
                        .eq(PersonalBook::getAccountId, id)
                        .or()
                        .eq(PersonalBook::getTargetAccountId, id)));
        if (count > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "账户已有流水，不能删除，可停用账户");
        }
        BookAccount account = getOwnedAccount(id);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        bookAccountMapper.deleteById(id);
    }

    @Override
    public List<BookBudgetVO> listBudgets(String yearMonth) {
        String month = normalizeMonthOrCurrent(yearMonth);
        List<BookBudget> budgets = bookBudgetMapper.selectList(new LambdaQueryWrapper<BookBudget>()
                .eq(BookBudget::getBudgetMonth, month)
                .eq(BookBudget::getCreateBy, currentUsername())
                .orderByAsc(BookBudget::getCategoryId)
                .orderByAsc(BookBudget::getId));
        Map<Long, String> categoryMap = bookCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
        Map<Long, BigDecimal> expenseMap = expenseByCategory(month);
        BigDecimal totalExpense = baseMapper.sumByMonth(month, currentUsername()) == null
                ? BigDecimal.ZERO
                : decimal(baseMapper.sumByMonth(month, currentUsername()).get("totalExpense"));
        return budgets.stream().map(item -> {
            BookBudgetVO vo = new BookBudgetVO();
            BeanUtils.copyProperties(item, vo);
            vo.setCategoryName(item.getCategoryId() == null ? "总预算" : categoryMap.get(item.getCategoryId()));
            BigDecimal used = item.getCategoryId() == null ? totalExpense : expenseMap.getOrDefault(item.getCategoryId(), BigDecimal.ZERO);
            vo.setUsedAmount(used);
            vo.setRemainingAmount(nvl(item.getAmount()).subtract(used));
            vo.setUsagePercent(percent(used, item.getAmount()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBudget(BookBudgetSaveDTO dto) {
        validateMonth(dto.getBudgetMonth());
        BookBudget entity = new BookBudget();
        BeanUtils.copyProperties(dto, entity);
        bookBudgetMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBudget(BookBudgetSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预算ID不能为空");
        }
        validateMonth(dto.getBudgetMonth());
        BookBudget existing = getOwnedBudget(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "预算不存在");
        }
        BeanUtils.copyProperties(dto, existing);
        bookBudgetMapper.updateById(existing);
    }

    @Override
    public void deleteBudget(Long id) {
        BookBudget budget = getOwnedBudget(id);
        if (budget == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "预算不存在");
        }
        bookBudgetMapper.deleteById(id);
    }

    private void validateBook(BookSaveDTO dto) {
        if (dto.getBookType() == null || (dto.getBookType() != 1 && dto.getBookType() != 2 && dto.getBookType() != 3)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "记账类型不正确");
        }
        if (dto.getBookType() == 3) {
            if (dto.getAccountId() == null || dto.getTargetAccountId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "转账需要选择转出和转入账户");
            }
            if (dto.getAccountId().equals(dto.getTargetAccountId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "转出和转入账户不能相同");
            }
            assertAccountUsable(dto.getAccountId());
            assertAccountUsable(dto.getTargetAccountId());
            return;
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择分类");
        }
        BookCategory category = getOwnedCategory(dto.getCategoryId());
        if (category == null || category.getStatus() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类不存在或已停用");
        }
        if (!category.getType().equals(dto.getBookType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类与记账类型不匹配");
        }
        if (dto.getAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择账户");
        }
        assertAccountUsable(dto.getAccountId());
    }

    private void ensureDefaultCategories() {
        Long activeCount = bookCategoryMapper.selectCount(
                new LambdaQueryWrapper<BookCategory>()
                        .eq(BookCategory::getCreateBy, currentUsername())
                        .eq(BookCategory::getStatus, 0)
        );
        if (activeCount != null && activeCount > 0) {
            return;
        }

        createCategory("工资", "Money", 1, 1, 0L);
        createCategory("奖金", "Trophy", 1, 2, 0L);
        createCategory("兼职", "Briefcase", 1, 3, 0L);
        createCategory("投资理财", "TrendCharts", 1, 4, 0L);
        createCategory("其他收入", "MoreFilled", 1, 99, 0L);

        Long foodId = createCategory("餐饮", "Food", 2, 1, 0L);
        Long trafficId = createCategory("交通", "Van", 2, 2, 0L);
        Long shoppingId = createCategory("购物", "ShoppingBag", 2, 3, 0L);
        Long housingId = createCategory("居住", "House", 2, 4, 0L);
        createCategory("医疗", "FirstAidKit", 2, 5, 0L);
        createCategory("教育", "Reading", 2, 6, 0L);
        createCategory("娱乐", "VideoPlay", 2, 7, 0L);
        createCategory("人情", "Present", 2, 8, 0L);
        createCategory("其他支出", "MoreFilled", 2, 99, 0L);

        createCategory("早餐", null, 2, 1, foodId);
        createCategory("午餐", null, 2, 2, foodId);
        createCategory("晚餐", null, 2, 3, foodId);
        createCategory("零食饮料", null, 2, 4, foodId);
        createCategory("公交地铁", null, 2, 1, trafficId);
        createCategory("打车", null, 2, 2, trafficId);
        createCategory("加油停车", null, 2, 3, trafficId);
        createCategory("日用品", null, 2, 1, shoppingId);
        createCategory("服饰", null, 2, 2, shoppingId);
        createCategory("数码", null, 2, 3, shoppingId);
        createCategory("房租房贷", null, 2, 1, housingId);
        createCategory("水电燃气", null, 2, 2, housingId);
        createCategory("物业宽带", null, 2, 3, housingId);
    }

    private Long createCategory(String name, String icon, Integer type, Integer sortOrder, Long parentId) {
        BookCategory category = new BookCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setType(type);
        category.setSortOrder(sortOrder);
        category.setParentId(parentId);
        category.setStatus(0);
        bookCategoryMapper.insert(category);
        return category.getId();
    }

    private void applyAccountEffect(PersonalBook book, boolean reverse) {
        BigDecimal amount = nvl(book.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal sign = reverse ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        if (Integer.valueOf(1).equals(book.getBookType())) {
            changeAccountBalance(book.getAccountId(), amount.multiply(sign));
        } else if (Integer.valueOf(2).equals(book.getBookType())) {
            changeAccountBalance(book.getAccountId(), amount.negate().multiply(sign));
        } else if (Integer.valueOf(3).equals(book.getBookType())) {
            changeAccountBalance(book.getAccountId(), amount.negate().multiply(sign));
            changeAccountBalance(book.getTargetAccountId(), amount.multiply(sign));
        }
    }

    private void changeAccountBalance(Long accountId, BigDecimal delta) {
        if (accountId == null || delta.compareTo(BigDecimal.ZERO) == 0) return;
        BookAccount account = getOwnedAccount(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        account.setCurrentBalance(nvl(account.getCurrentBalance()).add(delta));
        bookAccountMapper.updateById(account);
    }

    private void assertAccountUsable(Long accountId) {
        BookAccount account = getOwnedAccount(accountId);
        if (account == null || Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账户不存在或已停用");
        }
    }

    private PersonalBook getOwnedBook(Long id) {
        if (id == null) return null;
        return getOne(new LambdaQueryWrapper<PersonalBook>()
                .eq(PersonalBook::getId, id)
                .eq(PersonalBook::getCreateBy, currentUsername()), false);
    }

    private BookCategory getOwnedCategory(Long id) {
        if (id == null) return null;
        return bookCategoryMapper.selectOne(new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getId, id)
                .eq(BookCategory::getCreateBy, currentUsername()));
    }

    private BookAccount getOwnedAccount(Long id) {
        if (id == null) return null;
        return bookAccountMapper.selectOne(new LambdaQueryWrapper<BookAccount>()
                .eq(BookAccount::getId, id)
                .eq(BookAccount::getCreateBy, currentUsername()));
    }

    private BookBudget getOwnedBudget(Long id) {
        if (id == null) return null;
        return bookBudgetMapper.selectOne(new LambdaQueryWrapper<BookBudget>()
                .eq(BookBudget::getId, id)
                .eq(BookBudget::getCreateBy, currentUsername()));
    }

    private BookAccountVO toAccountVO(BookAccount entity) {
        BookAccountVO vo = new BookAccountVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setAccountTypeDesc(accountTypeDesc(entity.getAccountType()));
        return vo;
    }

    private List<BookCategoryRankVO> toCategoryRanks(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            BookCategoryRankVO vo = new BookCategoryRankVO();
            vo.setCategoryId(decimal(row.get("categoryId")).longValue());
            vo.setCategoryName(String.valueOf(row.get("categoryName")));
            vo.setAmount(decimal(row.get("amount")));
            vo.setCount(decimal(row.get("count")).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> expenseByCategory(String month) {
        return baseMapper.categoryRank(month, currentUsername()).stream()
                .collect(Collectors.toMap(
                        row -> decimal(row.get("categoryId")).longValue(),
                        row -> decimal(row.get("amount")),
                        BigDecimal::add
                ));
    }

    private static String bookTypeDesc(Integer type) {
        if (Integer.valueOf(1).equals(type)) return "收入";
        if (Integer.valueOf(2).equals(type)) return "支出";
        if (Integer.valueOf(3).equals(type)) return "转账";
        return "-";
    }

    private static String accountTypeDesc(Integer type) {
        if (Integer.valueOf(1).equals(type)) return "现金";
        if (Integer.valueOf(2).equals(type)) return "银行卡";
        if (Integer.valueOf(3).equals(type)) return "电子钱包";
        if (Integer.valueOf(4).equals(type)) return "信用账户";
        return "其他";
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(String.valueOf(value));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal percent(BigDecimal used, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return nvl(used).multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static String normalizeMonthOrCurrent(String yearMonth) {
        String normalized = normalizeMonth(yearMonth);
        return normalized == null ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : normalized;
    }

    private static String normalizeMonth(String yearMonth) {
        String value = trimToNull(yearMonth);
        if (value == null) return null;
        validateMonth(value);
        return value;
    }

    private static void validateMonth(String yearMonth) {
        try {
            YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "月份格式必须为yyyy-MM");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String currentUsername() {
        return CurrentUserUtil.getUsernameOrDefault("system");
    }
}
