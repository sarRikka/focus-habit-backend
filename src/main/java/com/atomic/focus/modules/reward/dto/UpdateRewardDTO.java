package com.atomic.focus.modules.reward.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateRewardDTO {

    private String name;

    private String content;

    @JsonProperty("trigger_type")
    private String triggerType;

    @JsonProperty("trigger_value")
    private Integer triggerValue;
}
