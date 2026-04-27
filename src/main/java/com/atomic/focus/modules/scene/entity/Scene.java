package com.atomic.focus.modules.scene.entity;

import com.atomic.focus.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene")
public class Scene extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    private String type;

    private String label;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    private String mode;

    @TableField("shorten_to")
    private Integer shortenTo;

    @TableField("extend_hours")
    private Integer extendHours;

    @TableField("client_op_id")
    private String clientOpId;
}
