package com.bank.admin.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Monitor timeline item")
public class MonitorTimelineItemVO {

    @Schema(description = "Event type: OPERATION or CALENDAR")
    private String type;

    @Schema(description = "Operator")
    private String operator;

    @Schema(description = "Event time")
    private LocalDateTime time;

    @Schema(description = "Module")
    private String module;

    @Schema(description = "Action")
    private String action;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Result")
    private Integer result;

    @Schema(description = "Source record id")
    private Long sourceId;
}
