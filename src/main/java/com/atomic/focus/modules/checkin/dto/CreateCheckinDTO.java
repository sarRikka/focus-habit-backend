package com.atomic.focus.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCheckinDTO {

    /** 默认今天 */
    private LocalDate date;

    @NotNull
    @Min(1)
    private Integer duration;

    /** done | late */
    private String status = "done";

    private String note;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
