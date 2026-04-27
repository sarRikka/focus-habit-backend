package com.atomic.focus.modules.notification.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private String id;

    private String event;

    private String title;

    private String content;

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
