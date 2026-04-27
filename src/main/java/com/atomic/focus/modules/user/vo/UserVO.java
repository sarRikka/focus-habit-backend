package com.atomic.focus.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserVO {

    @JsonProperty("user_id")
    private String userId;

    private String nickname;

    private String avatar;

    @JsonProperty("is_guest")
    private Boolean isGuest;

    @JsonProperty("joined_at")
    private LocalDate joinedAt;

    private List<String> badges;

    private Stats stats;

    @Data
    public static class Stats {
        @JsonProperty("total_checkin_days")
        private Integer totalCheckinDays;
        @JsonProperty("fixed_habits_count")
        private Integer fixedHabitsCount;
        @JsonProperty("active_goals_count")
        private Integer activeGoalsCount;
        @JsonProperty("continuous_days")
        private Integer continuousDays;
    }
}
