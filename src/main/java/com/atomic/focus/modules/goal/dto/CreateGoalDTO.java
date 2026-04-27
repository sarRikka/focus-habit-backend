package com.atomic.focus.modules.goal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateGoalDTO {

    @NotBlank(message = "name 不能为空")
    private String name;

    /** habit | ability | state | custom */
    private String category = "custom";

    @JsonProperty("custom_category_name")
    private String customCategoryName;

    @JsonProperty("final_goal")
    private String finalGoal;

    @JsonProperty("core_need")
    private String coreNeed;

    @JsonProperty("total_description")
    private String totalDescription;

    @NotNull(message = "deadline 不能为空")
    private LocalDate deadline;

    private String color = "brand";

    private String icon;

    private List<PhaseDTO> phases;

    @JsonProperty("daily_habit")
    private DailyHabitDTO dailyHabit;

    @JsonProperty("client_op_id")
    private String clientOpId;

    @Data
    public static class PhaseDTO {
        private String name;
        private String description;
        @JsonProperty("total_minutes")
        private Integer totalMinutes;
        @JsonProperty("start_date")
        private LocalDate startDate;
        @JsonProperty("end_date")
        private LocalDate endDate;
    }
}
