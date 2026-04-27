package com.atomic.focus.modules.goal.service;

import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.vo.CheckinVO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.vo.DailyHabitVO;
import com.atomic.focus.modules.goal.vo.GoalVO;
import com.atomic.focus.modules.goal.vo.PhaseVO;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.vo.RewardVO;
import org.springframework.stereotype.Component;

/**
 * 目标/阶段/奖励/打卡 等实体到 VO 的转换器（无业务依赖，便于测试与复用）。
 */
@Component
public class GoalAssembler {

    public GoalVO toGoalVO(Goal g) {
        if (g == null) {
            return null;
        }
        GoalVO vo = new GoalVO();
        vo.setId(g.getId());
        vo.setUserId(g.getUserId());
        vo.setName(g.getName());
        vo.setCategory(g.getCategory());
        vo.setCustomCategoryName(g.getCustomCategoryName());
        vo.setFinalGoal(g.getFinalGoal());
        vo.setCoreNeed(g.getCoreNeed());
        vo.setTotalDescription(g.getTotalDescription());
        vo.setDeadline(g.getDeadline());
        vo.setColor(g.getColor());
        vo.setIcon(g.getIcon());
        vo.setProgress(g.getProgress());
        vo.setManualDeduction(g.getManualDeduction());
        vo.setArchived(Boolean.TRUE.equals(g.getArchived()));
        vo.setFixed(Boolean.TRUE.equals(g.getFixed()));
        if (g.getCreatedAt() != null) {
            vo.setCreatedAt(g.getCreatedAt().toLocalDate());
        }
        vo.setUpdatedAt(g.getUpdatedAt());

        DailyHabitVO dh = new DailyHabitVO();
        dh.setDescription(g.getDhDescription());
        dh.setDuration(g.getDhDuration());
        dh.setAutoLevelUp(Boolean.TRUE.equals(g.getDhAutoLevelUp()));
        dh.setLevelUpStep(g.getDhLevelUpStep());
        vo.setDailyHabit(dh);
        return vo;
    }

    public PhaseVO toPhaseVO(Phase p) {
        if (p == null) {
            return null;
        }
        PhaseVO vo = new PhaseVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setDescription(p.getDescription());
        vo.setTotalMinutes(p.getTotalMinutes());
        vo.setStartDate(p.getStartDate());
        vo.setEndDate(p.getEndDate());
        vo.setCompleted(Boolean.TRUE.equals(p.getCompleted()));
        vo.setCompletedAt(p.getCompletedAt());
        return vo;
    }

    public RewardVO toRewardVO(Reward r, Goal goal) {
        if (r == null) {
            return null;
        }
        RewardVO vo = new RewardVO();
        vo.setId(r.getId());
        vo.setGoalId(r.getGoalId());
        if (goal != null) {
            vo.setGoalName(goal.getName());
            vo.setGoalIcon(goal.getIcon());
            vo.setGoalColor(goal.getColor());
        }
        vo.setName(r.getName());
        vo.setContent(r.getContent());
        vo.setTriggerType(r.getTriggerType());
        vo.setTriggerValue(r.getTriggerValue());
        vo.setTriggerLabel(triggerLabel(r));
        vo.setStatus(r.getStatus());
        vo.setClaimedAt(r.getClaimedAt());
        return vo;
    }

    public CheckinVO toCheckinVO(Checkin c) {
        if (c == null) {
            return null;
        }
        CheckinVO vo = new CheckinVO();
        vo.setDate(c.getCheckinDate());
        vo.setStatus(c.getStatus());
        vo.setDuration(c.getDuration());
        vo.setNote(c.getNote());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    private String triggerLabel(Reward r) {
        if (r == null || r.getTriggerType() == null) {
            return null;
        }
        return switch (r.getTriggerType()) {
            case "progress" -> "进度达 " + r.getTriggerValue() + "%";
            case "phase" -> "完成第 " + r.getTriggerValue() + " 阶段";
            case "days" -> "连续打卡 " + r.getTriggerValue() + " 天";
            default -> null;
        };
    }
}
