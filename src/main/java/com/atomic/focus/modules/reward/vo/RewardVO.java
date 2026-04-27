package com.atomic.focus.modules.reward.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RewardVO {

    private String id;

    @JsonProperty("goal_id")
    private String goalId;

    @JsonProperty("goal_name")
    private String goalName;

    @JsonProperty("goal_icon")
    private String goalIcon;

    @JsonProperty("goal_color")
    private String goalColor;

    private String name;

    private String content;

    @JsonProperty("trigger_type")
    private String triggerType;

    @JsonProperty("trigger_value")
    private Integer triggerValue;

    @JsonProperty("trigger_label")
    private String triggerLabel;

    private String status;

    @JsonProperty("claimed_at")
    private LocalDateTime claimedAt;
}
