package com.atomic.focus.modules.goal.vo;

import com.atomic.focus.modules.checkin.vo.CheckinVO;
import com.atomic.focus.modules.reward.vo.RewardVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GoalVO {

    private String id;

    @JsonProperty("user_id")
    private String userId;

    private String name;

    private String category;

    @JsonProperty("custom_category_name")
    private String customCategoryName;

    @JsonProperty("final_goal")
    private String finalGoal;

    @JsonProperty("core_need")
    private String coreNeed;

    @JsonProperty("total_description")
    private String totalDescription;

    private LocalDate deadline;

    @JsonProperty("created_at")
    private LocalDate createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private String color;

    private String icon;

    @JsonProperty("daily_habit")
    private DailyHabitVO dailyHabit;

    private List<PhaseVO> phases;

    private List<RewardVO> rewards;

    private Integer progress;

    @JsonProperty("manual_deduction")
    private Integer manualDeduction;

    private Boolean archived;

    private Boolean fixed;

    /** 详情接口可携带最近 35 天打卡（列表接口为 null） */
    private List<CheckinVO> checkins;
}
