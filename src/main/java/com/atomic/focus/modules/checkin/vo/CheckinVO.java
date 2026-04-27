package com.atomic.focus.modules.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CheckinVO {

    private LocalDate date;

    private String status;

    private Integer duration;

    private String note;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
