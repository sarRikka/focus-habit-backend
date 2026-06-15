package com.atomic.focus.modules.checkin.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.checkin.dto.CreateCheckinDTO;
import com.atomic.focus.modules.checkin.dto.MissedCheckinDTO;
import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.checkin.service.CheckinService;
import com.atomic.focus.modules.checkin.service.EffectiveDailyTargetMinutesResolver;
import com.atomic.focus.modules.checkin.service.EncouragementService;
import com.atomic.focus.modules.checkin.service.ProgressCalculator;
import com.atomic.focus.modules.checkin.vo.CalendarVO;
import com.atomic.focus.modules.checkin.vo.CheckinResultVO;
import com.atomic.focus.modules.checkin.vo.CheckinVO;
import com.atomic.focus.modules.checkin.vo.MissedResultVO;
import com.atomic.focus.modules.checkin.vo.TodayChecklistVO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.atomic.focus.modules.goal.service.GoalAssembler;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.mapper.RewardMapper;
import com.atomic.focus.modules.settings.service.SettingsService;
import com.atomic.focus.modules.user.entity.User;
import com.atomic.focus.modules.user.mapper.UserMapper;
import com.atomic.focus.modules.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final Set<String> CREATE_STATUS = Set.of("done", "late");

    private final CheckinMapper checkinMapper;
    private final GoalMapper goalMapper;
    private final GoalService goalService;
    private final GoalAssembler goalAssembler;
    private final PhaseMapper phaseMapper;
    private final RewardMapper rewardMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final SettingsService settingsService;
    private final ProgressCalculator progressCalculator;
    private final EncouragementService encouragementService;
    private final EffectiveDailyTargetMinutesResolver effectiveDailyTargetMinutesResolver;

    @Override
    @Transactional
    public CheckinResultVO checkin(String userId, String goalId, CreateCheckinDTO dto) {
        Goal goal = goalService.requireGoal(userId, goalId);
        if (Boolean.TRUE.equals(goal.getArchived())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "目标已归档，不能打卡");
        }
        String status = dto.getStatus() == null ? "done" : dto.getStatus();
        if (!CREATE_STATUS.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "status 仅支持 done | late");
        }
        LocalDate date = dto.getDate() == null ? LocalDate.now() : dto.getDate();
        int duration = dto.getDuration() == null ? 0 : dto.getDuration();
        if (duration < 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "duration 不能为负数");
        }

        // client_op_id 幂等
        if (dto.getClientOpId() != null) {
            Checkin exist = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getUserId, userId)
                    .eq(Checkin::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) {
                return buildResult(goal, exist, false, null, List.of());
            }
        }

        boolean isFirstToday = false;
        Checkin existing = findByDate(goalId, date);
        if (existing != null) {
            existing.setStatus(status);
            existing.setDuration(duration);
            if (dto.getNote() != null) existing.setNote(dto.getNote());
            existing.setClientOpId(dto.getClientOpId());
            checkinMapper.updateById(existing);
        } else {
            Checkin c = new Checkin();
            c.setId(IdGenerator.checkin());
            c.setGoalId(goalId);
            c.setUserId(userId);
            c.setCheckinDate(date);
            c.setStatus(status);
            c.setDuration(duration);
            c.setNote(dto.getNote());
            c.setClientOpId(dto.getClientOpId());
            checkinMapper.insert(c);
            existing = c;
            isFirstToday = true;
        }

        // 重新计算进度
        int oldProgress = goal.getProgress() == null ? 0 : goal.getProgress();
        int newProgress = progressCalculator.recompute(goal);
        boolean fixedNow = false;
        if (newProgress >= 100 && !Boolean.TRUE.equals(goal.getFixed())) {
            goal.setFixed(true);
            goal.setFixedAt(LocalDateTime.now());
            fixedNow = true;
        }
        goal.setProgress(newProgress);
        goalMapper.updateById(goal);

        // 用户聚合统计
        if (isFirstToday) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setTotalCheckinDays((u.getTotalCheckinDays() == null ? 0 : u.getTotalCheckinDays()) + 1);
                u.setContinuousDays(progressCalculator.continuousDays(goalId));
                userMapper.updateById(u);
            }
        }
        if (fixedNow) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setFixedHabitsCount((u.getFixedHabitsCount() == null ? 0 : u.getFixedHabitsCount()) + 1);
                userMapper.updateById(u);
            }
            userService.grantBadge(userId, "habit_master", "习惯掌控者", "至少固化一个习惯");
        }

        // 阶段达成检测：找到包含当前日期、未完成、且其总时长已被覆盖的阶段
        Phase phaseCompleted = autoCompletePhaseIfNeeded(goal, date);

        // 奖励解锁
        List<Reward> unlocked = unlockRewards(goal, oldProgress, newProgress, phaseCompleted);

        return buildResult(goal, existing, fixedNow, phaseCompleted, unlocked);
    }

    @Override
    @Transactional
    public MissedResultVO missed(String userId, String goalId, MissedCheckinDTO dto) {
        Goal goal = goalService.requireGoal(userId, goalId);
        LocalDate date = dto.getDate() == null ? LocalDate.now() : dto.getDate();

        if (dto.getClientOpId() != null) {
            Checkin exist = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getUserId, userId)
                    .eq(Checkin::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) {
                MissedResultVO vo = new MissedResultVO();
                vo.setGoalProgress(goal.getProgress());
                vo.setDeducted(0);
                vo.setManualDeductionTotal(goal.getManualDeduction());
                return vo;
            }
        }

        Checkin existing = findByDate(goalId, date);
        if (existing == null) {
            existing = new Checkin();
            existing.setId(IdGenerator.checkin());
            existing.setGoalId(goalId);
            existing.setUserId(userId);
            existing.setCheckinDate(date);
            existing.setDuration(0);
            existing.setStatus("missed");
            existing.setClientOpId(dto.getClientOpId());
            checkinMapper.insert(existing);
        } else {
            existing.setStatus("missed");
            existing.setDuration(0);
            existing.setClientOpId(dto.getClientOpId());
            checkinMapper.updateById(existing);
        }

        int deducted = 0;
        if (Boolean.TRUE.equals(dto.getDeductProgress())) {
            int per = settingsService.getRaw(userId).getDefaultProgressDeduction();
            deducted = per;
            int total = (goal.getManualDeduction() == null ? 0 : goal.getManualDeduction()) + per;
            goal.setManualDeduction(total);
        }
        int newProgress = progressCalculator.recompute(goal);
        goal.setProgress(newProgress);
        goalMapper.updateById(goal);

        MissedResultVO vo = new MissedResultVO();
        vo.setGoalProgress(newProgress);
        vo.setDeducted(deducted);
        vo.setManualDeductionTotal(goal.getManualDeduction());
        return vo;
    }

    @Override
    @Transactional
    public void delete(String userId, String goalId, LocalDate date) {
        Goal goal = goalService.requireGoal(userId, goalId);
        Checkin c = findByDate(goalId, date);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对应日期未打卡");
        }
        checkinMapper.deleteById(c.getId());
        int newProgress = progressCalculator.recompute(goal);
        goal.setProgress(newProgress);
        goalMapper.updateById(goal);
    }

    @Override
    public List<CheckinVO> list(String userId, String goalId, LocalDate startDate, LocalDate endDate, String status) {
        goalService.requireGoal(userId, goalId);
        LambdaQueryWrapper<Checkin> w = new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getGoalId, goalId);
        if (startDate != null) w.ge(Checkin::getCheckinDate, startDate);
        if (endDate != null) w.le(Checkin::getCheckinDate, endDate);
        if (status != null && !status.isBlank()) w.eq(Checkin::getStatus, status);
        w.orderByDesc(Checkin::getCheckinDate);
        return checkinMapper.selectList(w).stream().map(goalAssembler::toCheckinVO).collect(Collectors.toList());
    }

    @Override
    public TodayChecklistVO today(String userId) {
        LocalDate date = LocalDate.now();
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId)
                .eq(Goal::getArchived, false));

        List<TodayChecklistVO.Item> items = new ArrayList<>();
        int checked = 0;
        for (Goal g : goals) {
            Checkin c = findByDate(g.getId(), date);
            boolean isChecked = c != null && ("done".equals(c.getStatus()) || "late".equals(c.getStatus()));
            if (isChecked) checked++;
            TodayChecklistVO.Item it = new TodayChecklistVO.Item();
            it.setGoalId(g.getId());
            it.setGoalName(g.getName());
            it.setGoalIcon(g.getIcon());
            it.setDailyHabit(g.getDhDescription());
            it.setDurationTarget(g.getDhDuration());
            int effective = effectiveDailyTargetMinutesResolver.resolveEffectiveDailyTarget(g, date, userId);
            it.setEffectiveDailyTargetMinutes(effective);
            it.setMinimumCompletedMinutes(0);
            it.setChecked(isChecked);
            items.add(it);
        }
        TodayChecklistVO vo = new TodayChecklistVO();
        vo.setDate(date);
        vo.setItems(items);
        vo.setCheckedCount(checked);
        vo.setTotalCount(items.size());
        vo.setProgressPercent(items.isEmpty() ? 0 : (int) Math.round(checked * 100.0 / items.size()));
        return vo;
    }

    @Override
    public CalendarVO calendar(String userId, String goalId, int year, int month) {
        goalService.requireGoal(userId, goalId);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<LocalDate, Checkin> map = new HashMap<>();
        for (Checkin c : checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getGoalId, goalId)
                .between(Checkin::getCheckinDate, start, end))) {
            map.put(c.getCheckinDate(), c);
        }
        List<CalendarVO.Cell> cells = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            CalendarVO.Cell cell = new CalendarVO.Cell();
            cell.setDate(d);
            Checkin c = map.get(d);
            if (c == null) {
                cell.setStatus("pending");
                cell.setDuration(0);
            } else {
                cell.setStatus(c.getStatus());
                cell.setDuration(c.getDuration() == null ? 0 : c.getDuration());
            }
            cells.add(cell);
        }
        CalendarVO vo = new CalendarVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setCells(cells);
        return vo;
    }

    // ---------- private ----------

    private Checkin findByDate(String goalId, LocalDate date) {
        return checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getGoalId, goalId)
                .eq(Checkin::getCheckinDate, date)
                .last("LIMIT 1"));
    }

    private Phase autoCompletePhaseIfNeeded(Goal goal, LocalDate date) {
        List<Phase> phases = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                .eq(Phase::getGoalId, goal.getId())
                .eq(Phase::getCompleted, false)
                .le(Phase::getStartDate, date)
                .ge(Phase::getEndDate, date));
        if (phases.isEmpty()) return null;
        Phase phase = phases.get(0);
        // 计算阶段时长是否完成
        int phaseDone = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getGoalId, goal.getId())
                        .between(Checkin::getCheckinDate, phase.getStartDate(), phase.getEndDate())
                        .in(Checkin::getStatus, "done", "late"))
                .stream().mapToInt(c -> c.getDuration() == null ? 0 : c.getDuration()).sum();
        if (phase.getTotalMinutes() != null && phase.getTotalMinutes() > 0
                && phaseDone >= phase.getTotalMinutes()) {
            phase.setCompleted(true);
            phase.setCompletedAt(LocalDateTime.now());
            phaseMapper.updateById(phase);
            return phase;
        }
        return null;
    }

    private List<Reward> unlockRewards(Goal goal, int oldProgress, int newProgress, Phase phaseCompleted) {
        List<Reward> all = rewardMapper.selectList(new LambdaQueryWrapper<Reward>()
                .eq(Reward::getGoalId, goal.getId())
                .ne(Reward::getStatus, "claimed"));
        List<Reward> unlocked = new ArrayList<>();
        int continuous = progressCalculator.continuousDays(goal.getId());
        // 已完成阶段数（包含本次）
        int completedPhases = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                .eq(Phase::getGoalId, goal.getId())
                .eq(Phase::getCompleted, true)).size();

        for (Reward r : all) {
            boolean reached = false;
            switch (r.getTriggerType()) {
                case "progress" -> reached = newProgress >= r.getTriggerValue() && oldProgress < r.getTriggerValue();
                case "phase" -> reached = phaseCompleted != null && completedPhases >= r.getTriggerValue();
                case "days" -> reached = continuous >= r.getTriggerValue();
                default -> reached = false;
            }
            if (reached && !"available".equals(r.getStatus())) {
                r.setStatus("available");
                rewardMapper.updateById(r);
                unlocked.add(r);
            }
        }
        return unlocked;
    }

    private CheckinResultVO buildResult(Goal goal, Checkin checkin,
                                        boolean fixedNow, Phase phase, List<Reward> rewards) {
        CheckinResultVO vo = new CheckinResultVO();
        vo.setCheckin(goalAssembler.toCheckinVO(checkin));
        vo.setGoalProgress(goal.getProgress());
        vo.setHabitFixed(fixedNow);
        if (phase != null) {
            vo.setPhaseCompleted(goalAssembler.toPhaseVO(phase));
        }
        if (rewards != null) {
            vo.setRewardsUnlocked(rewards.stream()
                    .map(r -> goalAssembler.toRewardVO(r, goal))
                    .sorted(Comparator.comparing(r -> r.getTriggerValue() == null ? 0 : r.getTriggerValue()))
                    .collect(Collectors.toList()));
        }
        if (fixedNow) {
            vo.setEncouragement(encouragementService.pickFixed(goal.getUserId()));
        } else if (rewards != null && !rewards.isEmpty()) {
            vo.setEncouragement(encouragementService.pickReward(goal.getUserId()));
        } else {
            vo.setEncouragement(encouragementService.pickDaily(goal.getUserId()));
        }
        return vo;
    }
}
