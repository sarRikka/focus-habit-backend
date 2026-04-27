package com.atomic.focus.modules.goal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePhaseDTO {

    @NotBlank
    private String name;

    private String description;

    @JsonProperty("total_minutes")
    private Integer totalMinutes;

    @NotNull
    @JsonProperty("start_date")
    private LocalDate startDate;

    @NotNull
    @JsonProperty("end_date")
    private LocalDate endDate;
}
