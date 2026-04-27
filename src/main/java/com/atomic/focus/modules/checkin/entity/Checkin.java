package com.atomic.focus.modules.checkin.entity;

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
@TableName("checkin")
public class Checkin extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("goal_id")
    private String goalId;

    @TableField("user_id")
    private String userId;

    @TableField("checkin_date")
    private LocalDate checkinDate;

    private String status;

    private Integer duration;

    private String note;

    @TableField("client_op_id")
    private String clientOpId;
}
