package com.bank.admin.module.log.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.log.dto.OperationLogQueryDTO;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.mapper.OperationLogMapper;
import com.bank.admin.module.log.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl
        extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public PageResult<OperationLog> page(OperationLogQueryDTO query) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(query.getOperator()), OperationLog::getOperator, query.getOperator())
                .like(StringUtils.hasText(query.getModule()), OperationLog::getModule, query.getModule())
                .eq(query.getResult() != null, OperationLog::getResult, query.getResult())
                .ge(query.getStartDate() != null, OperationLog::getCreateTime,
                        query.getStartDate() != null ? query.getStartDate().atStartOfDay() : null)
                .le(query.getEndDate() != null, OperationLog::getCreateTime,
                        query.getEndDate() != null ? query.getEndDate().plusDays(1).atStartOfDay() : null)
                .orderByDesc(OperationLog::getCreateTime);

        Page<OperationLog> p = super.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(p);
    }

    @Override
    @Async
    public void saveLog(OperationLog log) {
        log.setCreateTime(LocalDateTime.now());
        super.save(log);
    }

    @Override
    public void exportExcel(OperationLogQueryDTO query, OutputStream out) {
        query.setCurrent(1);
        query.setSize(10000);
        PageResult<OperationLog> result = page(query);
        List<OperationLog> records = result.getRecords();

        List<List<String>> heads = new ArrayList<>();
        heads.add(List.of("操作人"));
        heads.add(List.of("模块"));
        heads.add(List.of("操作"));
        heads.add(List.of("描述"));
        heads.add(List.of("接口路径"));
        heads.add(List.of("结果"));
        heads.add(List.of("IP"));
        heads.add(List.of("耗时(ms)"));
        heads.add(List.of("操作时间"));

        List<List<Object>> dataList = new ArrayList<>();
        for (OperationLog log : records) {
            List<Object> row = new ArrayList<>();
            row.add(log.getOperator());
            row.add(log.getModule());
            row.add(log.getAction());
            row.add(log.getDescription());
            row.add(log.getRequestPath());
            row.add(log.getResult() == 0 ? "成功" : "失败");
            row.add(log.getClientIp());
            row.add(log.getDuration() != null ? log.getDuration().toString() : "-");
            row.add(log.getCreateTime() != null ? log.getCreateTime().toString().replace("T", " ").substring(0, 19) : "-");
            dataList.add(row);
        }

        EasyExcel.write(out)
                .head(heads)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("操作日志")
                .doWrite(dataList);
    }
}
