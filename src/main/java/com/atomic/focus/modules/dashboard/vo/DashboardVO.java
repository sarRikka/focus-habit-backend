package com.atomic.focus.modules.dashboard.vo;

import com.atomic.focus.modules.reward.vo.RewardVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DashboardVO {

    private Today today;

    private Stats stats;

    @JsonProperty("weekly_rates")
    private List<DailyRate> weeklyRates;

    @JsonProperty("available_rewards")
    private List<RewardVO> availableRewards;

    private String motto;

    @JsonProperty("active_scene")
    private Object activeScene;

    @Data
    public static class Today {
        private LocalDate date;
        private String weekday;
        @JsonProperty("checked_count")
        private Integer checkedCount;
        @JsonProperty("total_count")
        private Integer totalCount;
        @JsonProperty("progress_percent")
        private Integer progressPercent;
    }

    @Data
    public static class Stats {
        @JsonProperty("active_goals")
        private Integer activeGoals;
        @JsonProperty("continuous_days")
        private Integer continuousDays;
        @JsonProperty("fixed_habits")
        private Integer fixedHabits;
    }

    @Data
    public static class DailyRate {
        private LocalDate date;
        private Integer rate;
    }
}
