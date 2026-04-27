package com.atomic.focus.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MissedCheckinDTO {

    private LocalDate date;

    @JsonProperty("deduct_progress")
    private Boolean deductProgress = false;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
