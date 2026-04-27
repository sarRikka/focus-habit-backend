package com.atomic.focus.modules.settings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateSettingsDTO {

    @JsonProperty("reminder_time")
    private String reminderTime;

    @JsonProperty("reminder_repeat")
    private Integer reminderRepeat;

    @JsonProperty("review_reminder_enabled")
    private Boolean reviewReminderEnabled;

    @JsonProperty("review_reminder_time")
    private String reviewReminderTime;

    @JsonProperty("push_enabled")
    private Boolean pushEnabled;

    private String theme;

    @JsonProperty("data_retention")
    private String dataRetention;

    @JsonProperty("default_progress_deduction")
    private Integer defaultProgressDeduction;
}
