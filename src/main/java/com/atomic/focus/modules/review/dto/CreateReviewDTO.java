package com.atomic.focus.modules.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReviewDTO {

    /** weekly | monthly | manual */
    @NotBlank
    private String type;

    private String title;

    private LocalDate date;

    @JsonProperty("goal_id")
    private String goalId;

    private String content;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
