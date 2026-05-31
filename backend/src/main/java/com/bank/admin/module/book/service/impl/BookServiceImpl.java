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
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 记账 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl
        extends ServiceImpl<BookMapper, PersonalBook>
        implements BookService {

    private static final String SOURCE_TYPE_WECHAT = "WECHAT";
    private static final String SOURCE_TYPE_ALIPAY = "ALIPAY";
    private static final DateTimeFormatter WECHAT_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> REQUIRED_WECHAT_HEADERS = Set.of(
            "交易时间", "交易类型", "交易对方", "商品", "收/支", "金额(元)", "支付方式", "当前状态", "交易单号", "商户单号", "备注"
    );
    private static final Set<String> REQUIRED_ALIPAY_HEADERS = Set.of(
            "交易时间", "交易分类", "交易对方", "商品说明", "收/支", "金额", "收/付款方式", "交易状态", "交易订单号", "商家订单号", "备注"
    );

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
    public WechatBillImportResultVO previewWechatImport(MultipartFile file) {
        return parseWechatBill(file, false);
    }

    @Override
    public WechatBillImportResultVO previewAlipayImport(MultipartFile file) {
        return parseAlipayBill(file, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatBillImportResultVO importWechatBill(WechatBillImportConfirmDTO dto) {
        WechatBillImportResultVO result = prepareEditedImport(dto, SOURCE_TYPE_WECHAT);
        return saveImportedRows(result, SOURCE_TYPE_WECHAT, "WX-");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatBillImportResultVO importAlipayBill(WechatBillImportConfirmDTO dto) {
        WechatBillImportResultVO result = prepareEditedImport(dto, SOURCE_TYPE_ALIPAY);
        return saveImportedRows(result, SOURCE_TYPE_ALIPAY, "ALI-");
    }

    private WechatBillImportResultVO saveImportedRows(WechatBillImportResultVO result, String sourceType, String batchPrefix) {
        String batchNo = batchPrefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" +
                UUID.randomUUID().toString().substring(0, 8);
        result.setBatchNo(batchNo);
        List<PersonalBook> rows = new ArrayList<>();
        for (WechatBillImportRowVO row : result.getRows()) {
            if (!"READY".equals(row.getImportStatus()) && !"PENDING".equals(row.getImportStatus())) {
                continue;
            }
            PersonalBook entity = new PersonalBook();
            entity.setBookDate(row.getBookDate());
            entity.setBookTime(row.getBookTime());
            entity.setBookType(row.getBookType());
            entity.setAmount(row.getAmount());
            entity.setCategoryId(row.getCategoryId());
            entity.setAccountId(row.getAccountId());
            entity.setTargetAccountId(row.getTargetAccountId());
            entity.setMerchant(limit(row.getCounterparty(), 100));
            entity.setDescription(limit(buildImportDescription(row, sourceType), 255));
            entity.setSourceType(sourceType);
            entity.setSourceTradeNo(limit(row.getTradeNo(), 80));
            entity.setSourceHash(row.getSourceHash());
            entity.setImportBatchNo(batchNo);
            rows.add(entity);
        }
        if (!rows.isEmpty()) {
            saveBatch(rows);
            rows.forEach(row -> applyAccountEffect(row, false));
        }
        result.setImportedRows(rows.size());
        return result;
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

    private WechatBillImportResultVO parseWechatBill(MultipartFile file, boolean forImport) {
        validateWechatFile(file);
        ensureDefaultCategories();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findWechatHeaderRow(sheet, formatter);
            Row headerRow = sheet.getRow(headerRowIndex);
            Map<String, Integer> headerMap = buildHeaderMap(headerRow, formatter);

            List<WechatBillImportRowVO> rows = new ArrayList<>();
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || rowIsBlank(row, formatter)) {
                    continue;
                }
                rows.add(parseWechatRow(row, i + 1, headerMap, formatter));
            }
            if (rows.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "微信账单没有可导入的交易明细");
            }
            markDuplicatesAndSummarize(rows, forImport, SOURCE_TYPE_WECHAT);
            return buildImportResult(rows);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "读取微信账单失败");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "解析微信账单失败：" + e.getMessage());
        }
    }

    private WechatBillImportResultVO prepareEditedImport(WechatBillImportConfirmDTO dto, String sourceType) {
        if (dto == null || dto.getRows() == null || dto.getRows().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入明细不能为空");
        }
        List<WechatBillImportRowVO> rows = dto.getRows().stream()
                .filter(Objects::nonNull)
                .map(row -> normalizeEditedImportRow(row, sourceType))
                .toList();
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入明细不能为空");
        }
        markDuplicatesAndSummarize(rows, true, sourceType);
        return buildImportResult(rows);
    }

    private WechatBillImportRowVO normalizeEditedImportRow(WechatBillImportRowVO row, String sourceType) {
        try {
            if (row.getBookDate() == null) {
                LocalDateTime tradeTime = LocalDateTime.parse(row.getTradeTime(), WECHAT_TIME_FMT);
                row.setBookDate(tradeTime.toLocalDate());
                row.setBookTime(tradeTime.toLocalTime());
            }
            if (row.getBookTime() == null && StringUtils.hasText(row.getTradeTime())) {
                row.setBookTime(LocalDateTime.parse(row.getTradeTime(), WECHAT_TIME_FMT).toLocalTime());
            }
            if (row.getBookType() == null) {
                row.setBookType(resolveBookType(row.getIncomeExpense()));
            }
            row.setBookTypeDesc(bookTypeDesc(row.getBookType()));
            if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "金额必须大于0");
            }
            row.setAmount(row.getAmount().abs().setScale(2, RoundingMode.HALF_UP));
            row.setSourceHash(buildSourceHash(sourceType, row));
            return row;
        } catch (Exception e) {
            row.setImportStatus("ERROR");
            row.setImportStatusDesc("异常");
            row.setMessage(e instanceof BusinessException ? e.getMessage() : "编辑内容解析失败：" + e.getMessage());
            return row;
        }
    }

    private void validateWechatFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择微信账单文件");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持导入微信支付导出的xlsx文件");
        }
    }

    private WechatBillImportResultVO parseAlipayBill(MultipartFile file, boolean forImport) {
        validateAlipayFile(file);
        ensureDefaultCategories();
        try {
            List<String> lines = readAlipayLines(file);
            int headerIndex = findAlipayHeaderLine(lines);
            Map<String, Integer> headerMap = buildCsvHeaderMap(parseCsvLine(lines.get(headerIndex)));
            List<WechatBillImportRowVO> rows = new ArrayList<>();
            for (int i = headerIndex + 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!StringUtils.hasText(line) || line.startsWith("---")) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.stream().noneMatch(StringUtils::hasText)) {
                    continue;
                }
                rows.add(parseAlipayRow(values, i + 1, headerMap));
            }
            if (rows.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "支付宝账单没有可导入的交易明细");
            }
            markDuplicatesAndSummarize(rows, forImport, SOURCE_TYPE_ALIPAY);
            return buildImportResult(rows);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "解析支付宝账单失败：" + e.getMessage());
        }
    }

    private void validateAlipayFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择支付宝账单文件");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持导入支付宝导出的csv文件");
        }
    }

    private List<String> readAlipayLines(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("GB18030"), Charset.forName("GBK"))) {
            String text = new String(bytes, charset);
            List<String> lines = text.lines().map(line -> stripBom(line).trim()).toList();
            if (lines.stream().anyMatch(line -> buildCsvHeaderMap(parseCsvLine(line)).keySet().containsAll(REQUIRED_ALIPAY_HEADERS))) {
                return lines;
            }
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "未识别到支付宝账单明细表头");
    }

    private int findAlipayHeaderLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Map<String, Integer> headerMap = buildCsvHeaderMap(parseCsvLine(lines.get(i)));
            if (headerMap.keySet().containsAll(REQUIRED_ALIPAY_HEADERS)) {
                return i;
            }
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "未识别到支付宝账单明细表头");
    }

    private Map<String, Integer> buildCsvHeaderMap(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String text = stripBom(trimToEmpty(headers.get(i)));
            if (StringUtils.hasText(text)) {
                map.put(text, i);
            }
        }
        return map;
    }

    private WechatBillImportRowVO parseAlipayRow(List<String> values, int rowNo, Map<String, Integer> headerMap) {
        WechatBillImportRowVO vo = new WechatBillImportRowVO();
        vo.setRowNo(rowNo);
        vo.setTradeTime(readCsvValue(values, headerMap, "交易时间"));
        vo.setTradeType(readCsvValue(values, headerMap, "交易分类"));
        vo.setCounterparty(readCsvValue(values, headerMap, "交易对方"));
        vo.setProduct(readCsvValue(values, headerMap, "商品说明"));
        vo.setIncomeExpense(readCsvValue(values, headerMap, "收/支"));
        vo.setPaymentMethod(readCsvValue(values, headerMap, "收/付款方式"));
        vo.setStatus(readCsvValue(values, headerMap, "交易状态"));
        vo.setTradeNo(stripBacktick(readCsvValue(values, headerMap, "交易订单号")));
        vo.setMerchantTradeNo(stripBacktick(readCsvValue(values, headerMap, "商家订单号")));
        vo.setRemark(readCsvValue(values, headerMap, "备注"));
        try {
            LocalDateTime tradeTime = LocalDateTime.parse(vo.getTradeTime(), WECHAT_TIME_FMT);
            vo.setBookDate(tradeTime.toLocalDate());
            vo.setBookTime(tradeTime.toLocalTime());
            vo.setAmount(parseMoney(readCsvValue(values, headerMap, "金额")));
            vo.setBookType(resolveBookType(vo.getIncomeExpense()));
            vo.setBookTypeDesc(bookTypeDesc(vo.getBookType()));
            resolveCategory(vo);
            resolveAccount(vo);
            vo.setSourceHash(buildSourceHash(SOURCE_TYPE_ALIPAY, vo));
            return vo;
        } catch (Exception e) {
            vo.setImportStatus("ERROR");
            vo.setImportStatusDesc("异常");
            vo.setMessage(e instanceof BusinessException ? e.getMessage() : "解析失败：" + e.getMessage());
            return vo;
        }
    }

    private int findWechatHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            Map<String, Integer> headerMap = buildHeaderMap(row, formatter);
            if (headerMap.keySet().containsAll(REQUIRED_WECHAT_HEADERS)) {
                return i;
            }
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "未识别到微信账单明细表头");
    }

    private Map<String, Integer> buildHeaderMap(Row row, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        if (row == null) {
            return map;
        }
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String text = cellText(row, i, formatter);
            if (StringUtils.hasText(text)) {
                map.put(text.trim(), i);
            }
        }
        return map;
    }

    private WechatBillImportRowVO parseWechatRow(Row row, int rowNo, Map<String, Integer> headerMap, DataFormatter formatter) {
        WechatBillImportRowVO vo = new WechatBillImportRowVO();
        vo.setRowNo(rowNo);
        vo.setTradeTime(readCell(row, headerMap, formatter, "交易时间"));
        vo.setTradeType(readCell(row, headerMap, formatter, "交易类型"));
        vo.setCounterparty(readCell(row, headerMap, formatter, "交易对方"));
        vo.setProduct(readCell(row, headerMap, formatter, "商品"));
        vo.setIncomeExpense(readCell(row, headerMap, formatter, "收/支"));
        vo.setPaymentMethod(readCell(row, headerMap, formatter, "支付方式"));
        vo.setStatus(readCell(row, headerMap, formatter, "当前状态"));
        vo.setTradeNo(stripBacktick(readCell(row, headerMap, formatter, "交易单号")));
        vo.setMerchantTradeNo(stripBacktick(readCell(row, headerMap, formatter, "商户单号")));
        vo.setRemark(readCell(row, headerMap, formatter, "备注"));
        try {
            LocalDateTime tradeTime = LocalDateTime.parse(vo.getTradeTime(), WECHAT_TIME_FMT);
            vo.setBookDate(tradeTime.toLocalDate());
            vo.setBookTime(tradeTime.toLocalTime());
            vo.setAmount(parseMoney(readCell(row, headerMap, formatter, "金额(元)")));
            vo.setBookType(resolveBookType(vo.getIncomeExpense()));
            vo.setBookTypeDesc(bookTypeDesc(vo.getBookType()));
            resolveCategory(vo);
            resolveAccount(vo);
            vo.setSourceHash(buildSourceHash(SOURCE_TYPE_WECHAT, vo));
            return vo;
        } catch (Exception e) {
            vo.setImportStatus("ERROR");
            vo.setImportStatusDesc("异常");
            vo.setMessage(e instanceof BusinessException ? e.getMessage() : "解析失败：" + e.getMessage());
            return vo;
        }
    }

    private void resolveCategory(WechatBillImportRowVO vo) {
        if (!Integer.valueOf(1).equals(vo.getBookType()) && !Integer.valueOf(2).equals(vo.getBookType())) {
            return;
        }
        List<BookCategory> categories = bookCategoryMapper.selectList(new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getCreateBy, currentUsername())
                .eq(BookCategory::getType, vo.getBookType())
                .eq(BookCategory::getStatus, 0));
        BookCategory category = chooseCategory(categories, vo);
        if (category != null) {
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getName());
        }
    }

    private BookCategory chooseCategory(List<BookCategory> categories, WechatBillImportRowVO vo) {
        String haystack = (trimToEmpty(vo.getTradeType()) + " " + trimToEmpty(vo.getCounterparty()) + " " + trimToEmpty(vo.getProduct())).toLowerCase();
        String[] preferredNames;
        if (Integer.valueOf(1).equals(vo.getBookType())) {
            preferredNames = new String[]{"其他收入", "鍏朵粬鏀跺叆"};
        } else if (containsAny(haystack, "茶", "饮品", "小面", "餐", "饭", "咖啡", "奶茶", "堂食", "美团", "饿了么", "食品", "餐饮美食")) {
            preferredNames = new String[]{"餐饮", "椁愰ギ", "零食饮料", "闆堕楗枡"};
        } else if (containsAny(haystack, "打车", "地铁", "公交", "客运", "高铁", "机票", "加油", "停车", "索道", "交通出行", "高德")) {
            preferredNames = new String[]{"交通", "浜ら€?", "打车", "鎵撹溅"};
        } else if (containsAny(haystack, "京东", "淘宝", "购物", "超市", "商场", "便利", "好又多", "日用", "日用百货", "淘宝闪购", "盒马")) {
            preferredNames = new String[]{"购物", "璐墿", "日用品", "鏃ョ敤鍝?"};
        } else if (containsAny(haystack, "医院", "药", "医疗")) {
            preferredNames = new String[]{"医疗", "鍖荤枟"};
        } else if (containsAny(haystack, "学", "课程", "教育")) {
            preferredNames = new String[]{"教育", "鏁欒偛"};
        } else if (containsAny(haystack, "旅游", "门票", "电影", "娱乐", "景区", "黄山")) {
            preferredNames = new String[]{"娱乐", "濞变箰"};
        } else if (containsAny(haystack, "充值缴费", "话费", "电信", "移动", "联通", "水电", "燃气", "宽带")) {
            preferredNames = new String[]{"居住", "物业宽带", "水电燃气"};
        } else if (containsAny(haystack, "公益捐赠", "捐赠")) {
            preferredNames = new String[]{"人情", "其他支出"};
        } else if (containsAny(haystack, "投资理财", "基金", "余额宝", "蚂蚁财富")) {
            preferredNames = new String[]{"投资理财", "其他支出"};
        } else {
            preferredNames = new String[]{"其他支出", "鍏朵粬鏀嚭"};
        }
        for (String name : preferredNames) {
            BookCategory matched = categories.stream()
                    .filter(cat -> Objects.equals(cat.getName(), name))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    private void resolveAccount(WechatBillImportRowVO vo) {
        if (Integer.valueOf(3).equals(vo.getBookType())) {
            return;
        }
        String method = trimToEmpty(vo.getPaymentMethod()).toLowerCase();
        if (!StringUtils.hasText(method)) {
            return;
        }
        List<BookAccount> accounts = bookAccountMapper.selectList(new LambdaQueryWrapper<BookAccount>()
                .eq(BookAccount::getCreateBy, currentUsername())
                .eq(BookAccount::getStatus, 0));
        BookAccount matched = accounts.stream()
                .filter(account -> accountMatchesPayment(account, method))
                .findFirst()
                .orElse(null);
        if (matched != null) {
            vo.setAccountId(matched.getId());
            vo.setAccountName(matched.getName());
        }
    }

    private boolean accountMatchesPayment(BookAccount account, String paymentMethod) {
        String name = trimToEmpty(account.getName()).toLowerCase();
        if (!StringUtils.hasText(name)) {
            return false;
        }
        if (paymentMethod.contains(name) || name.contains(paymentMethod)) {
            return true;
        }
        if (paymentMethod.contains("零钱") && name.contains("零钱")) {
            return true;
        }
        if (paymentMethod.contains("分付") && name.contains("分付")) {
            return true;
        }
        if (paymentMethod.contains("余额宝") && name.contains("余额宝")) {
            return true;
        }
        String digits = paymentMethod.replaceAll("\\D+", "");
        return StringUtils.hasText(digits) && name.contains(digits);
    }

    private WechatBillImportResultVO buildImportResult(List<WechatBillImportRowVO> rows) {
        WechatBillImportResultVO result = new WechatBillImportResultVO();
        result.setRows(rows);
        result.setTotalRows(rows.size());
        result.setImportableRows((int) rows.stream().filter(row -> "READY".equals(row.getImportStatus()) || "PENDING".equals(row.getImportStatus())).count());
        result.setDuplicateRows((int) rows.stream().filter(row -> "DUPLICATE".equals(row.getImportStatus())).count());
        result.setPendingRows((int) rows.stream().filter(row -> "PENDING".equals(row.getImportStatus())).count());
        result.setErrorRows((int) rows.stream().filter(row -> "ERROR".equals(row.getImportStatus())).count());
        return result;
    }

    private void markDuplicatesAndSummarize(List<WechatBillImportRowVO> rows, boolean forImport, String sourceType) {
        Set<String> existingHashes = loadExistingSourceHashes(rows, sourceType);
        Set<String> seenInFile = new HashSet<>();
        for (WechatBillImportRowVO row : rows) {
            if ("ERROR".equals(row.getImportStatus())) {
                continue;
            }
            if (!seenInFile.add(row.getSourceHash()) || existingHashes.contains(row.getSourceHash())) {
                row.setImportStatus("DUPLICATE");
                row.setImportStatusDesc("重复");
                row.setMessage(forImport ? "已导入，跳过" : "系统中已存在，确认导入时会跳过");
                continue;
            }
            boolean pending = row.getCategoryId() == null || row.getAccountId() == null || Integer.valueOf(3).equals(row.getBookType());
            row.setImportStatus(pending ? "PENDING" : "READY");
            row.setImportStatusDesc(pending ? "待补全" : "可导入");
            row.setMessage(pending ? "账户/分类或转账账户待补全" : "");
        }
    }

    private Set<String> loadExistingSourceHashes(List<WechatBillImportRowVO> rows, String sourceType) {
        List<String> hashes = rows.stream()
                .map(WechatBillImportRowVO::getSourceHash)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (hashes.isEmpty()) {
            return Set.of();
        }
        return list(new LambdaQueryWrapper<PersonalBook>()
                .select(PersonalBook::getSourceHash)
                .eq(PersonalBook::getCreateBy, currentUsername())
                .eq(PersonalBook::getSourceType, sourceType)
                .in(PersonalBook::getSourceHash, hashes))
                .stream()
                .map(PersonalBook::getSourceHash)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private static boolean rowIsBlank(Row row, DataFormatter formatter) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (StringUtils.hasText(cellText(row, i, formatter))) {
                return false;
            }
        }
        return true;
    }

    private static String readCell(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String header) {
        Integer index = headerMap.get(header);
        return index == null ? "" : cellText(row, index, formatter);
    }

    private static String readCsvValue(List<String> values, Map<String, Integer> headerMap, String header) {
        Integer index = headerMap.get(header);
        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }
        return trimToEmpty(values.get(index));
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private static String cellText(Row row, int index, DataFormatter formatter) {
        if (row == null || index < 0) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private static BigDecimal parseMoney(String value) {
        String normalized = trimToEmpty(value).replace(",", "").replace("¥", "").replace("￥", "").replace("元", "");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额不能为空");
        }
        return new BigDecimal(normalized).abs().setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer resolveBookType(String incomeExpense) {
        String value = trimToEmpty(incomeExpense);
        if ("收入".equals(value)) {
            return 1;
        }
        if ("支出".equals(value)) {
            return 2;
        }
        return 3;
    }

    private static String buildSourceHash(String sourceType, WechatBillImportRowVO row) {
        return sha256(String.join("|",
                sourceType,
                trimToEmpty(row.getTradeTime()),
                trimToEmpty(row.getIncomeExpense()),
                row.getAmount() == null ? "" : row.getAmount().toPlainString(),
                trimToEmpty(row.getCounterparty()),
                trimToEmpty(row.getTradeNo())));
    }

    private static String buildImportDescription(WechatBillImportRowVO row, String sourceType) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(row.getProduct()) && !"/".equals(row.getProduct())) {
            parts.add(row.getProduct());
        }
        String sourceName = SOURCE_TYPE_ALIPAY.equals(sourceType) ? "支付宝" : "微信";
        if (StringUtils.hasText(row.getTradeType())) {
            parts.add(sourceName + "类型:" + row.getTradeType());
        }
        if (StringUtils.hasText(row.getPaymentMethod())) {
            parts.add((SOURCE_TYPE_ALIPAY.equals(sourceType) ? "收/付款方式:" : "支付方式:") + row.getPaymentMethod());
        }
        if (StringUtils.hasText(row.getStatus())) {
            parts.add("状态:" + row.getStatus());
        }
        if (StringUtils.hasText(row.getRemark()) && !"/".equals(row.getRemark())) {
            parts.add("备注:" + row.getRemark());
        }
        return String.join("；", parts);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String stripBacktick(String value) {
        String text = trimToEmpty(value);
        return text.startsWith("`") ? text.substring(1) : text;
    }

    private static String stripBom(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : encoded) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String limit(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
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

    private static String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }

    private static String currentUsername() {
        return CurrentUserUtil.getUsernameOrDefault("system");
    }
}
