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
@TableName("goal")
public class Goal extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    private String name;

    private String category;

    @TableField("custom_category_name")
    private String customCategoryName;

    @TableField("final_goal")
    private String finalGoal;

    @TableField("core_need")
    private String coreNeed;

    @TableField("total_description")
    private String totalDescription;

    private LocalDate deadline;

    private String color;

    private String icon;

    @TableField("dh_description")
    private String dhDescription;

    @TableField("dh_duration")
    private Integer dhDuration;

    @TableField("dh_auto_level_up")
    private Boolean dhAutoLevelUp;

    @TableField("dh_level_up_step")
    private Integer dhLevelUpStep;

    private Integer progress;

    @TableField("manual_deduction")
    private Integer manualDeduction;

    private Boolean archived;

    private Boolean fixed;

    @TableField("fixed_at")
    private LocalDateTime fixedAt;

    @TableField("client_op_id")
    private String clientOpId;
}
