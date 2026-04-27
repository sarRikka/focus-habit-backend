package com.atomic.focus.modules.checkin.service;

import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 进度计算器：根据打卡时长 / 目标总时长 计算进度，并在末尾扣除 manual_deduction。
 */
@RequiredArgsConstructor
@Component
public class ProgressCalculator {

    private final CheckinMapper checkinMapper;
    private final PhaseMapper phaseMapper;

    /**
     * 重新计算并返回进度（0-100）。
     * 进度 = 累计有效时长 / 目标总时长 * 100 - manual_deduction，最低不小于 0。
     */
    public int recompute(Goal goal) {
        int totalMinutes = computeTotalMinutes(goal);
        int doneMinutes = sumDoneMinutes(goal.getId());
        int progress;
        if (totalMinutes <= 0) {
            // 没有阶段总时长，使用 deadline 与 daily duration 估算
            progress = estimateByDailyHabit(goal, doneMinutes);
        } else {
            progress = (int) Math.round(doneMinutes * 100.0 / totalMinutes);
        }
        progress -= goal.getManualDeduction() == null ? 0 : goal.getManualDeduction();
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        return progress;
    }

    public int computeTotalMinutes(Goal goal) {
        List<Phase> phases = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                .eq(Phase::getGoalId, goal.getId()));
        int sum = phases.stream().mapToInt(p -> p.getTotalMinutes() == null ? 0 : p.getTotalMinutes()).sum();
        if (sum > 0) return sum;
        // 退化：使用 daily_habit.duration * 剩余天数
        int duration = goal.getDhDuration() == null ? 10 : goal.getDhDuration();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
        if (days < 1) days = 1;
        return (int) (days * duration);
    }

    public int sumDoneMinutes(String goalId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getGoalId, goalId)
                        .in(Checkin::getStatus, "done", "late"))
                .stream().mapToInt(c -> c.getDuration() == null ? 0 : c.getDuration()).sum();
    }

    /**
     * 计算 goal 的当前连续打卡天数（以今日为准向前回溯，遇到非 done/late/paused 即中断）。
     */
    public int continuousDays(String goalId) {
        List<Checkin> list = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getGoalId, goalId)
                .orderByDesc(Checkin::getCheckinDate));
        if (list.isEmpty()) return 0;
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        for (Checkin c : list) {
            if (c.getCheckinDate().equals(cursor) || c.getCheckinDate().equals(cursor.minusDays(1))) {
                if (!"missed".equals(c.getStatus())) {
                    streak++;
                    cursor = c.getCheckinDate().minusDays(1);
                } else {
                    break;
                }
            } else if (c.getCheckinDate().isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }

    private int estimateByDailyHabit(Goal goal, int doneMinutes) {
        int duration = goal.getDhDuration() == null ? 10 : goal.getDhDuration();
        long totalDays = ChronoUnit.DAYS.between(goal.getCreatedAt() == null
                ? LocalDate.now() : goal.getCreatedAt().toLocalDate(), goal.getDeadline());
        if (totalDays < 1) totalDays = 1;
        int total = (int) (totalDays * duration);
        return total <= 0 ? 0 : (int) Math.round(doneMinutes * 100.0 / total);
    }
}
