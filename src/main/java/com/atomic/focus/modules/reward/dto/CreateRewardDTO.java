package com.atomic.focus.modules.reward.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRewardDTO {

    @NotBlank
    private String name;

    private String content;

    @NotBlank
    @JsonProperty("trigger_type")
    private String triggerType;

    @NotNull
    @JsonProperty("trigger_value")
    private Integer triggerValue;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
