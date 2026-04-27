package com.atomic.focus.modules.reward.entity;

import com.atomic.focus.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward")
public class Reward extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("goal_id")
    private String goalId;

    @TableField("user_id")
    private String userId;

    private String name;

    private String content;

    @TableField("trigger_type")
    private String triggerType;

    @TableField("trigger_value")
    private Integer triggerValue;

    private Integer sort;

    private String status;

    @TableField("claimed_at")
    private LocalDateTime claimedAt;

    @TableField("client_op_id")
    private String clientOpId;
}
