package com.atomic.focus.modules.data.dto;

import lombok.Data;

@Data
public class ExportDTO {

    /** json | csv */
    private String format = "json";
}
