package com.atomic.focus.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AchievementVO {

    private String key;

    private String name;

    private String desc;

    private Boolean earned;

    @JsonProperty("earned_at")
    private LocalDate earnedAt;
}
