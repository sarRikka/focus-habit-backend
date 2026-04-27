package com.atomic.focus.modules.goal.entity;

import com.atomic.focus.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("phase")
public class Phase extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("goal_id")
    private String goalId;

    @TableField("user_id")
    private String userId;

    private String name;

    private String description;

    @TableField("total_minutes")
    private Integer totalMinutes;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    private Integer sort;

    private Boolean completed;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}
