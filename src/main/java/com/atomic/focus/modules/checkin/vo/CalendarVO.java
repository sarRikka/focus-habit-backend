package com.atomic.focus.modules.checkin.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CalendarVO {

    private Integer year;

    private Integer month;

    private List<Cell> cells;

    @Data
    public static class Cell {
        private LocalDate date;
        private String status;
        private Integer duration;
    }
}
