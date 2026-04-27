package com.atomic.focus.modules.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TodayChecklistVO {

    private LocalDate date;

    private List<Item> items;

    @JsonProperty("checked_count")
    private Integer checkedCount;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("progress_percent")
    private Integer progressPercent;

    @Data
    public static class Item {
        @JsonProperty("goal_id")
        private String goalId;
        @JsonProperty("goal_name")
        private String goalName;
        @JsonProperty("goal_icon")
        private String goalIcon;
        @JsonProperty("daily_habit")
        private String dailyHabit;
        @JsonProperty("duration_target")
        private Integer durationTarget;
        private Boolean checked;
    }
}
