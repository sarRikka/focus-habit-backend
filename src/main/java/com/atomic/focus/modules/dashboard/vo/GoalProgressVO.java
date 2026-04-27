package com.atomic.focus.modules.dashboard.vo;

import com.atomic.focus.modules.goal.vo.PhaseVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoalProgressVO {

    private Integer progress;

    @JsonProperty("manual_deduction")
    private Integer manualDeduction;

    @JsonProperty("completed_minutes")
    private Integer completedMinutes;

    @JsonProperty("total_minutes")
    private Integer totalMinutes;

    @JsonProperty("remaining_minutes")
    private Integer remainingMinutes;

    @JsonProperty("checkin_stats")
    private CheckinStats checkinStats;

    @JsonProperty("continuous_days")
    private Integer continuousDays;

    private List<PhaseVO> phases;

    @Data
    public static class CheckinStats {
        private Integer total;
        private Integer done;
        private Integer late;
        private Integer missed;
    }
}
