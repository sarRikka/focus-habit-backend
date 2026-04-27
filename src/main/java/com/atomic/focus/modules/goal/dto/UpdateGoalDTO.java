package com.atomic.focus.modules.goal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateGoalDTO {

    private String name;

    @JsonProperty("final_goal")
    private String finalGoal;

    @JsonProperty("core_need")
    private String coreNeed;

    @JsonProperty("total_description")
    private String totalDescription;

    private LocalDate deadline;

    private String color;

    private String icon;

    @JsonProperty("daily_habit")
    private DailyHabitDTO dailyHabit;
}
