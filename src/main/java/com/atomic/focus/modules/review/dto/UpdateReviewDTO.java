package com.atomic.focus.modules.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReviewDTO {

    private String title;

    private String content;

    private LocalDate date;

    @JsonProperty("goal_id")
    private String goalId;
}
