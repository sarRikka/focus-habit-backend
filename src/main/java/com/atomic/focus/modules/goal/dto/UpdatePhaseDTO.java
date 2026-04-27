package com.atomic.focus.modules.goal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePhaseDTO {

    private String name;

    private String description;

    @JsonProperty("total_minutes")
    private Integer totalMinutes;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;
}
