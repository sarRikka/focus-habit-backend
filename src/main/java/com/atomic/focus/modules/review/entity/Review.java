package com.atomic.focus.modules.review.entity;

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
@TableName("review")
public class Review extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("goal_id")
    private String goalId;

    private String type;

    private String title;

    @TableField("review_date")
    private LocalDate reviewDate;

    private String content;

    @TableField("metrics_json")
    private String metricsJson;

    @TableField("suggestions_json")
    private String suggestionsJson;

    @TableField("is_favorite")
    private Boolean isFavorite;

    @TableField("client_op_id")
    private String clientOpId;
}
