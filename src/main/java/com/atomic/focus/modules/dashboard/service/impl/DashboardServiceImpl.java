package com.atomic.focus.modules.dashboard.service.impl;

import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.checkin.service.EncouragementService;
import com.atomic.focus.modules.checkin.service.ProgressCalculator;
import com.atomic.focus.modules.dashboard.service.DashboardService;
import com.atomic.focus.modules.dashboard.vo.DashboardVO;
import com.atomic.focus.modules.dashboard.vo.GoalProgressVO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.atomic.focus.modules.goal.service.GoalAssembler;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.mapper.RewardMapper;
import com.atomic.focus.modules.reward.vo.RewardVO;
import com.atomic.focus.modules.scene.entity.Scene;
import com.atomic.focus.modules.scene.mapper.SceneMapper;
import com.atomic.focus.modules.user.entity.User;
import com.atomic.focus.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final GoalMapper goalMapper;
    private final CheckinMapper checkinMapper;
    private final RewardMapper rewardMapper;
    private final SceneMapper sceneMapper;
    private final PhaseMapper phaseMapper;
    private final UserMapper userMapper;
    private final GoalService goalService;
    private final GoalAssembler goalAssembler;
    private final ProgressCalculator progressCalculator;
    private final EncouragementService encouragementService;

    @Override
    public DashboardVO dashboard(String userId) {
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();

        List<Goal> activeGoals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId)
                .eq(Goal::getArchived, false));

        // Today
        DashboardVO.Today todayVO = new DashboardVO.Today();
        todayVO.setDate(today);
        todayVO.setWeekday(weekdayCN(today.getDayOfWeek()));
        int total = activeGoals.size();
        int checked = 0;
        for (Goal g : activeGoals) {
            Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getGoalId, g.getId())
                    .eq(Checkin::getCheckinDate, today)
                    .last("LIMIT 1"));
            if (c != null && ("done".equals(c.getStatus()) || "late".equals(c.getStatus()))) {
                checked++;
            }
        }
        todayVO.setCheckedCount(checked);
        todayVO.setTotalCount(total);
        todayVO.setProgressPercent(total == 0 ? 0 : (int) Math.round(checked * 100.0 / total));
        vo.setToday(todayVO);

        // Stats
        DashboardVO.Stats stats = new DashboardVO.Stats();
        stats.setActiveGoals(total);
        User u = userMapper.selectById(userId);
        stats.setContinuousDays(u == null || u.getContinuousDays() == null ? 0 : u.getContinuousDays());
        stats.setFixedHabits(u == null || u.getFixedHabitsCount() == null ? 0 : u.getFixedHabitsCount());
        vo.setStats(stats);

        // 最近 7 日打卡率
        List<DashboardVO.DailyRate> rates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            DashboardVO.DailyRate dr = new DashboardVO.DailyRate();
            dr.setDate(d);
            if (total == 0) {
                dr.setRate(0);
            } else {
                int dayChecked = 0;
                for (Goal g : activeGoals) {
                    Checkin c = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                            .eq(Checkin::getGoalId, g.getId())
                            .eq(Checkin::getCheckinDate, d)
                            .last("LIMIT 1"));
                    if (c != null && ("done".equals(c.getStatus()) || "late".equals(c.getStatus()))) {
                        dayChecked++;
                    }
                }
                dr.setRate((int) Math.round(dayChecked * 100.0 / total));
            }
            rates.add(dr);
        }
        vo.setWeeklyRates(rates);

        // 可领取奖励
        List<Reward> available = rewardMapper.selectList(new LambdaQueryWrapper<Reward>()
                .eq(Reward::getUserId, userId)
                .eq(Reward::getStatus, "available"));
        Map<String, Goal> goalCache = activeGoals.stream().collect(Collectors.toMap(Goal::getId, x -> x, (a, b) -> a));
        List<RewardVO> availableVOs = available.stream()
                .map(r -> goalAssembler.toRewardVO(r, goalCache.get(r.getGoalId())))
                .collect(Collectors.toList());
        vo.setAvailableRewards(availableVOs);

        // 当前生效场景
        Scene scene = sceneMapper.selectOne(new LambdaQueryWrapper<Scene>()
                .eq(Scene::getUserId, userId)
                .le(Scene::getStartDate, today)
                .ge(Scene::getEndDate, today)
                .last("LIMIT 1"));
        if (scene != null) {
            Map<String, Object> activeScene = new HashMap<>();
            activeScene.put("id", scene.getId());
            activeScene.put("mode", scene.getMode());
            activeScene.put("shorten_to", scene.getShortenTo());
            activeScene.put("extend_hours", scene.getExtendHours());
            vo.setActiveScene(activeScene);
        }

        vo.setMotto(encouragementService.pickDaily(userId));
        return vo;
    }

    @Override
    public GoalProgressVO progress(String userId, String goalId) {
        Goal goal = goalService.requireGoal(userId, goalId);
        GoalProgressVO vo = new GoalProgressVO();
        vo.setProgress(goal.getProgress());
        vo.setManualDeduction(goal.getManualDeduction());
        int totalMinutes = progressCalculator.computeTotalMinutes(goal);
        int doneMinutes = progressCalculator.sumDoneMinutes(goalId);
        vo.setTotalMinutes(totalMinutes);
        vo.setCompletedMinutes(doneMinutes);
        vo.setRemainingMinutes(Math.max(0, totalMinutes - doneMinutes));

        List<Checkin> all = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getGoalId, goalId));
        GoalProgressVO.CheckinStats cs = new GoalProgressVO.CheckinStats();
        cs.setTotal(all.size());
        cs.setDone((int) all.stream().filter(c -> "done".equals(c.getStatus())).count());
        cs.setLate((int) all.stream().filter(c -> "late".equals(c.getStatus())).count());
        cs.setMissed((int) all.stream().filter(c -> "missed".equals(c.getStatus())).count());
        vo.setCheckinStats(cs);
        vo.setContinuousDays(progressCalculator.continuousDays(goalId));

        List<Phase> phases = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                .eq(Phase::getGoalId, goalId)
                .orderByAsc(Phase::getSort));
        vo.setPhases(phases.stream().map(goalAssembler::toPhaseVO).collect(Collectors.toList()));
        return vo;
    }

    private String weekdayCN(DayOfWeek dow) {
        return "周" + dow.getDisplayName(TextStyle.NARROW, Locale.SIMPLIFIED_CHINESE);
    }
}
