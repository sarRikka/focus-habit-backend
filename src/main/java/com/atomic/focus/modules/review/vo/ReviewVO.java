package com.atomic.focus.modules.review.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {

    private String id;

    private String type;

    private String title;

    private LocalDate date;

    @JsonProperty("goal_id")
    private String goalId;

    @JsonProperty("goal_name")
    private String goalName;

    private String content;

    private ReviewMetricsVO metrics;

    private List<String> suggestions;

    @JsonProperty("is_favorite")
    private Boolean isFavorite;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
