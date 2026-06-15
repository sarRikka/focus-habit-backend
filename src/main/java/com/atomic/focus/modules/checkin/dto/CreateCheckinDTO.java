package com.atomic.focus.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCheckinDTO {

    /** 默认今天 */
    private LocalDate date;

    /** 实际完成分钟数，可为 0（一键完成）；省略时按 0 处理 */
    @Min(0)
    private Integer duration;

    /** done | late */
    private String status = "done";

    private String note;

    @JsonProperty("client_op_id")
    private String clientOpId;
}
