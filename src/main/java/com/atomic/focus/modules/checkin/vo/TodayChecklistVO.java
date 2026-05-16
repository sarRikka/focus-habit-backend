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

        /** PRD §7.5：考虑 shorten 场景后的当日有效目标分钟数 */
        @JsonProperty("effective_daily_target_minutes")
        private Integer effectiveDailyTargetMinutes;

        /** 完成打卡（done/late）允许的最低时长（⌈effective/2⌉，至少 1） */
        @JsonProperty("minimum_completed_minutes")
        private Integer minimumCompletedMinutes;

        private Boolean checked;
    }
}
