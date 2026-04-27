package com.atomic.focus.modules.settings.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_settings")
public class UserSettings {

    @TableId(value = "user_id", type = IdType.INPUT)
    private String userId;

    @TableField("reminder_time")
    private String reminderTime;

    @TableField("reminder_repeat")
    private Integer reminderRepeat;

    @TableField("review_reminder_enabled")
    private Boolean reviewReminderEnabled;

    @TableField("review_reminder_time")
    private String reviewReminderTime;

    @TableField("push_enabled")
    private Boolean pushEnabled;

    private String theme;

    @TableField("data_retention")
    private String dataRetention;

    @TableField("default_progress_deduction")
    private Integer defaultProgressDeduction;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
