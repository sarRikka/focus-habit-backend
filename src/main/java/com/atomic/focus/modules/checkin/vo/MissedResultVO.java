package com.atomic.focus.modules.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MissedResultVO {

    @JsonProperty("goal_progress")
    private Integer goalProgress;

    private Integer deducted;

    @JsonProperty("manual_deduction_total")
    private Integer manualDeductionTotal;
}
