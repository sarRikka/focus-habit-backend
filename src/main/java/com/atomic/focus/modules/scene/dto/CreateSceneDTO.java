package com.atomic.focus.modules.scene.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSceneDTO {

    @NotBlank
    private String type;

    @NotBlank
    private String label;

    @NotNull
    @JsonProperty("start_date")
    private LocalDate startDate;

    @NotNull
    @JsonProperty("end_date")
    private LocalDate endDate;

    @NotBlank
    private String mode;

    @JsonProperty("shorten_to")
    private Integer shortenTo;

    @JsonProperty("extend_hours")
    private Integer extendHours;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
