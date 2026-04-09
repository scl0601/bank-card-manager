package com.bank.admin.module.reminder.service;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.reminder.dto.ReminderTaskQueryDTO;
import com.bank.admin.module.reminder.vo.ReminderTaskVO;

import java.util.List;

/**
 * 提醒任务 Service 接口
 */
public interface ReminderTaskService {

    /**
     * 分页查询提醒任务
     */
    PageResult<ReminderTaskVO> page(ReminderTaskQueryDTO query);

    /**
     * 标记为已处理
     */
    void markHandled(Long id);

    /**
     * 标记为已忽略
     */
    void markIgnored(Long id);

    /**
     * 批量标记已处理
     */
    void batchMarkHandled(List<Long> ids);

    /**
     * 批量标记已忽略
     */
    void batchMarkIgnored(List<Long> ids);

    /**
     * 扫描并生成账单到期提醒
     */
    void scanBillDueReminders();

    /**
     * 扫描并生成卡片即将过期提醒
     */
    void scanCardExpiringReminders();
}
