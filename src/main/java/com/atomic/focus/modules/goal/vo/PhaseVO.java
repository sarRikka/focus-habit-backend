package com.atomic.focus.modules.goal.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PhaseVO {

    private String id;

    private String name;

    private String description;

    @JsonProperty("total_minutes")
    private Integer totalMinutes;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private Boolean completed;

    @JsonProperty("completed_at")
    private LocalDateTime completedAt;
}
