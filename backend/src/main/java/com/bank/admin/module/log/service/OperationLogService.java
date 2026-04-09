package com.bank.admin.module.log.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.log.dto.OperationLogQueryDTO;
import com.bank.admin.module.log.entity.OperationLog;

import java.io.OutputStream;

/**
 * 操作日志 Service 接口
 */
public interface OperationLogService {

    PageResult<OperationLog> page(OperationLogQueryDTO query);

    void saveLog(OperationLog log);

    void exportExcel(OperationLogQueryDTO query, OutputStream out);
}
