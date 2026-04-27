package com.atomic.focus.modules.review.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReviewMetricsVO {

    @JsonProperty("checkin_rate")
    private Integer checkinRate;

    @JsonProperty("avg_duration")
    private Integer avgDuration;

    @JsonProperty("missed_days")
    private Integer missedDays;

    @JsonProperty("progress_delta")
    private Integer progressDelta;

    @JsonProperty("total_minutes")
    private Integer totalMinutes;
}
