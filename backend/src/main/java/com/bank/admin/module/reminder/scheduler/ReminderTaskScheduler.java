package com.bank.admin.module.reminder.scheduler;

import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.reminder.service.ReminderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 提醒任务定时扫描器
 * <p>
 * 每天凌晨 1:00 执行一次，自动生成当日及未来的提醒任务。
 * 需在启动类或配置类上添加 @EnableScheduling 才会生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTaskScheduler {

    private final CardBillService cardBillService;
    private final ReminderTaskService reminderTaskService;

    /**
     * 每天 01:00 扫描账单到期提醒
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleBillDueReminders() {
        log.info("[定时任务] 触发账单生命周期刷新与到期提醒扫描...");
        try {
            cardBillService.refreshBillLifecycle();
            reminderTaskService.scanBillDueReminders();
        } catch (Exception e) {
            log.error("[定时任务] 账单生命周期刷新或到期提醒扫描异常", e);
        }
    }

    /**
     * 每天 01:10 扫描卡片即将过期提醒
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void scheduleCardExpiringReminders() {
        log.info("[定时任务] 触发卡片即将过期提醒扫描...");
        try {
            reminderTaskService.scanCardExpiringReminders();
        } catch (Exception e) {
            log.error("[定时任务] 卡片即将过期提醒扫描异常", e);
        }
    }
}
