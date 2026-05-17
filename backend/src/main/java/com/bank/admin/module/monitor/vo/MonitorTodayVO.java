package com.bank.admin.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Today monitor data")
public class MonitorTodayVO {

    @Schema(description = "Monitor date")
    private LocalDate date;

    @Schema(description = "Summary")
    private MonitorSummaryVO summary;

    @Schema(description = "Timeline")
    private List<MonitorTimelineItemVO> timeline;
}
