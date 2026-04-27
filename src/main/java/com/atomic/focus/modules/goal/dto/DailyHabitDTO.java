package com.atomic.focus.modules.goal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DailyHabitDTO {

    private String description;

    private Integer duration;

    @JsonProperty("auto_level_up")
    private Boolean autoLevelUp;

    @JsonProperty("level_up_step")
    private Integer levelUpStep;
}
