package com.atomic.focus.modules.checkin.vo;

import com.atomic.focus.modules.goal.vo.PhaseVO;
import com.atomic.focus.modules.reward.vo.RewardVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CheckinResultVO {

    private CheckinVO checkin;

    @JsonProperty("goal_progress")
    private Integer goalProgress;

    @JsonProperty("phase_completed")
    private PhaseVO phaseCompleted;

    @JsonProperty("habit_fixed")
    private Boolean habitFixed;

    @JsonProperty("rewards_unlocked")
    private List<RewardVO> rewardsUnlocked;

    private String encouragement;
}
