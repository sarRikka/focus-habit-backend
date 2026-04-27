package com.atomic.focus.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MarkReadDTO {

    @JsonProperty("notification_ids")
    private List<String> notificationIds;

    private Boolean all = false;
}
