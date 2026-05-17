package com.bank.admin.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Monitor summary")
public class MonitorSummaryVO {

    @Schema(description = "Operation count")
    private long operationCount;

    @Schema(description = "Successful operation count")
    private long successCount;

    @Schema(description = "Failed operation count")
    private long failCount;

    @Schema(description = "Calendar event count")
    private long calendarCount;

    @Schema(description = "Completed calendar event count")
    private long completedCalendarCount;
}
