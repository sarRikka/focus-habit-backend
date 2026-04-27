package com.atomic.focus.modules.data.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HistoryItemVO {

    /** goal | checkin | review | reward */
    private String kind;

    private String id;

    private String title;

    private String summary;

    private LocalDate date;

    private String goalId;
}
