package com.atomic.focus.modules.scene.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SceneVO {

    private String id;

    private String type;

    private String label;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private String mode;

    @JsonProperty("shorten_to")
    private Integer shortenTo;

    @JsonProperty("extend_hours")
    private Integer extendHours;

    private Boolean active;
}
