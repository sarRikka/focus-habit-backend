package com.atomic.focus.modules.user.entity;

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
@TableName("user")
public class User extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String phone;

    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    private String avatar;

    @TableField("is_guest")
    private Boolean isGuest;

    @TableField("device_id")
    private String deviceId;

    @TableField("joined_at")
    private LocalDate joinedAt;

    @TableField("total_checkin_days")
    private Integer totalCheckinDays;

    @TableField("fixed_habits_count")
    private Integer fixedHabitsCount;

    @TableField("continuous_days")
    private Integer continuousDays;
}
