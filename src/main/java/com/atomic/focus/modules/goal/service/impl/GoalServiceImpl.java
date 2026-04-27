package com.atomic.focus.modules.goal.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.goal.dto.CreateGoalDTO;
import com.atomic.focus.modules.goal.dto.DailyHabitDTO;
import com.atomic.focus.modules.goal.dto.UpdateGoalDTO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.atomic.focus.modules.goal.service.GoalAssembler;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.goal.vo.GoalVO;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.mapper.RewardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final Set<String> CATEGORIES = Set.of("habit", "ability", "state", "custom");
    private static final Set<String> COLORS = Set.of("brand", "mint", "lavender", "peach");

    private final GoalMapper goalMapper;
    private final PhaseMapper phaseMapper;
    private final RewardMapper rewardMapper;
    private final CheckinMapper checkinMapper;
    private final GoalAssembler assembler;

    @Override
    @Transactional
    public R<GoalVO> create(String userId, CreateGoalDTO dto) {
        validateCategory(dto.getCategory());
        validateColor(dto.getColor());
        if (dto.getDeadline() == null || dto.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "deadline 必须 >= 今天");
        }
        if (dto.getClientOpId() != null) {
            Goal exist = goalMapper.selectOne(new LambdaQueryWrapper<Goal>()
                    .eq(Goal::getUserId, userId)
                    .eq(Goal::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) {
                return R.ok(assemble(exist, false));
            }
        }

        Goal goal = new Goal();
        goal.setId(IdGenerator.goal());
        goal.setUserId(userId);
        goal.setName(dto.getName());
        goal.setCategory(dto.getCategory());
        goal.setCustomCategoryName(dto.getCustomCategoryName());
        goal.setFinalGoal(dto.getFinalGoal());
        goal.setCoreNeed(dto.getCoreNeed());
        goal.setTotalDescription(dto.getTotalDescription());
        goal.setDeadline(dto.getDeadline());
        goal.setColor(dto.getColor() == null ? "brand" : dto.getColor());
        goal.setIcon(dto.getIcon());
        goal.setProgress(0);
        goal.setManualDeduction(0);
        goal.setArchived(false);
        goal.setFixed(false);
        goal.setClientOpId(dto.getClientOpId());

        boolean durationWarn = false;
        if (dto.getDailyHabit() != null) {
            DailyHabitDTO dh = dto.getDailyHabit();
            goal.setDhDescription(dh.getDescription());
            goal.setDhDuration(dh.getDuration() == null ? 10 : dh.getDuration());
            goal.setDhAutoLevelUp(Boolean.TRUE.equals(dh.getAutoLevelUp()));
            goal.setDhLevelUpStep(dh.getLevelUpStep() == null ? 1 : dh.getLevelUpStep());
            durationWarn = goal.getDhDuration() != null && goal.getDhDuration() > 10;
        } else {
            goal.setDhDuration(10);
            goal.setDhAutoLevelUp(false);
            goal.setDhLevelUpStep(1);
        }

        goalMapper.insert(goal);

        if (dto.getPhases() != null) {
            int order = 0;
            for (CreateGoalDTO.PhaseDTO p : dto.getPhases()) {
                Phase phase = new Phase();
                phase.setId(IdGenerator.phase());
                phase.setGoalId(goal.getId());
                phase.setUserId(userId);
                phase.setName(p.getName());
                phase.setDescription(p.getDescription());
                phase.setTotalMinutes(p.getTotalMinutes() == null ? 0 : p.getTotalMinutes());
                phase.setStartDate(p.getStartDate());
                phase.setEndDate(p.getEndDate());
                if (phase.getStartDate() == null || phase.getEndDate() == null
                        || phase.getEndDate().isBefore(phase.getStartDate())) {
                    throw new BusinessException(ResultCode.PHASE_DATE_INVALID, "阶段日期范围非法");
                }
                phase.setSort(order++);
                phase.setCompleted(false);
                phaseMapper.insert(phase);
            }
        }

        GoalVO vo = assemble(goal, false);
        return durationWarn ? R.warn(ResultCode.DURATION_WARNING, vo) : R.ok(vo);
    }

    @Override
    public PageResult<GoalVO> list(String userId, String status, String category, String keyword,
                                   long page, long pageSize) {
        LambdaQueryWrapper<Goal> w = new LambdaQueryWrapper<>();
        w.eq(Goal::getUserId, userId);
        if (status == null || status.isBlank()) {
            status = "active";
        }
        switch (status) {
            case "active" -> w.eq(Goal::getArchived, false).eq(Goal::getFixed, false);
            case "fixed" -> w.eq(Goal::getFixed, true).eq(Goal::getArchived, false);
            case "archived" -> w.eq(Goal::getArchived, true);
            case "all" -> { /* no-op */ }
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "status 取值非法");
        }
        if (category != null && !category.isBlank()) {
            validateCategory(category);
            w.eq(Goal::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(Goal::getName, keyword).or().like(Goal::getFinalGoal, keyword));
        }
        w.orderByDesc(Goal::getCreatedAt);

        Page<Goal> p = Page.of(page, pageSize);
        Page<Goal> resultPage = goalMapper.selectPage(p, w);
        List<GoalVO> items = resultPage.getRecords().stream()
                .map(g -> assemble(g, false))
                .collect(Collectors.toList());
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    @Override
    public GoalVO detail(String userId, String goalId) {
        Goal goal = requireGoal(userId, goalId);
        return assemble(goal, true);
    }

    @Override
    @Transactional
    public GoalVO update(String userId, String goalId, UpdateGoalDTO dto) {
        Goal goal = requireGoal(userId, goalId);
        if (dto.getName() != null) goal.setName(dto.getName());
        if (dto.getFinalGoal() != null) goal.setFinalGoal(dto.getFinalGoal());
        if (dto.getCoreNeed() != null) goal.setCoreNeed(dto.getCoreNeed());
        if (dto.getTotalDescription() != null) goal.setTotalDescription(dto.getTotalDescription());
        if (dto.getDeadline() != null) {
            if (dto.getDeadline().isBefore(LocalDate.now())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "deadline 必须 >= 今天");
            }
            goal.setDeadline(dto.getDeadline());
        }
        if (dto.getColor() != null) {
            validateColor(dto.getColor());
            goal.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) goal.setIcon(dto.getIcon());

        if (dto.getDailyHabit() != null) {
            DailyHabitDTO dh = dto.getDailyHabit();
            if (dh.getDescription() != null) goal.setDhDescription(dh.getDescription());
            if (dh.getDuration() != null) goal.setDhDuration(dh.getDuration());
            if (dh.getAutoLevelUp() != null) goal.setDhAutoLevelUp(dh.getAutoLevelUp());
            if (dh.getLevelUpStep() != null) goal.setDhLevelUpStep(dh.getLevelUpStep());
        }
        goalMapper.updateById(goal);
        return assemble(goal, false);
    }

    @Override
    public void archive(String userId, String goalId) {
        Goal goal = requireGoal(userId, goalId);
        goal.setArchived(true);
        goalMapper.updateById(goal);
    }

    @Override
    public void delete(String userId, String goalId, boolean hard) {
        Goal goal = requireGoal(userId, goalId);
        // 软删除（逻辑删除）由 MyBatis-Plus 自动处理；hard 模式保留扩展位
        goalMapper.deleteById(goal.getId());
    }

    @Override
    public Goal requireGoal(String userId, String goalId) {
        Goal goal = goalMapper.selectById(goalId);
        if (goal == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标不存在");
        }
        if (!userId.equals(goal.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该目标");
        }
        return goal;
    }

    // ---------- private ----------

    private GoalVO assemble(Goal goal, boolean withCheckins) {
        GoalVO vo = assembler.toGoalVO(goal);

        List<Phase> phases = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                .eq(Phase::getGoalId, goal.getId())
                .orderByAsc(Phase::getSort)
                .orderByAsc(Phase::getStartDate));
        vo.setPhases(phases.stream().map(assembler::toPhaseVO).collect(Collectors.toList()));

        List<Reward> rewards = rewardMapper.selectList(new LambdaQueryWrapper<Reward>()
                .eq(Reward::getGoalId, goal.getId())
                .orderByAsc(Reward::getSort)
                .orderByAsc(Reward::getCreatedAt));
        vo.setRewards(rewards.stream().map(r -> assembler.toRewardVO(r, goal)).collect(Collectors.toList()));

        if (withCheckins) {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(34);
            List<Checkin> list = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getGoalId, goal.getId())
                    .between(Checkin::getCheckinDate, start, end));
            vo.setCheckins(list.stream()
                    .sorted(Comparator.comparing(Checkin::getCheckinDate))
                    .map(assembler::toCheckinVO)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private void validateCategory(String c) {
        if (c != null && !CATEGORIES.contains(c)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "category 取值非法");
        }
    }

    private void validateColor(String c) {
        if (c != null && !COLORS.contains(c)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "color 取值非法");
        }
    }
}
