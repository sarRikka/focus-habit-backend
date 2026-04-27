package com.atomic.focus.modules.reward.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.goal.service.GoalAssembler;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.reward.dto.CreateRewardDTO;
import com.atomic.focus.modules.reward.dto.UpdateRewardDTO;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.mapper.RewardMapper;
import com.atomic.focus.modules.reward.service.RewardService;
import com.atomic.focus.modules.reward.vo.RewardVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private static final Set<String> TRIGGER_TYPES = Set.of("progress", "phase", "days");
    private static final Set<String> STATUS_FILTER = Set.of("locked", "available", "claimed", "all");

    private final RewardMapper rewardMapper;
    private final GoalMapper goalMapper;
    private final GoalService goalService;
    private final GoalAssembler assembler;

    @Override
    public List<RewardVO> listByGoal(String userId, String goalId) {
        Goal goal = goalService.requireGoal(userId, goalId);
        return rewardMapper.selectList(new LambdaQueryWrapper<Reward>()
                        .eq(Reward::getGoalId, goalId)
                        .orderByAsc(Reward::getSort)
                        .orderByAsc(Reward::getCreatedAt))
                .stream().map(r -> assembler.toRewardVO(r, goal)).collect(Collectors.toList());
    }

    @Override
    public List<RewardVO> listAll(String userId, String status) {
        if (status != null && !status.isBlank() && !STATUS_FILTER.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "status 取值非法");
        }
        LambdaQueryWrapper<Reward> w = new LambdaQueryWrapper<Reward>().eq(Reward::getUserId, userId);
        if (status != null && !"all".equals(status) && !status.isBlank()) {
            w.eq(Reward::getStatus, status);
        }
        w.orderByDesc(Reward::getUpdatedAt);
        List<Reward> list = rewardMapper.selectList(w);
        Map<String, Goal> goalMap = new HashMap<>();
        for (Reward r : list) {
            goalMap.computeIfAbsent(r.getGoalId(), goalMapper::selectById);
        }
        return list.stream().map(r -> assembler.toRewardVO(r, goalMap.get(r.getGoalId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RewardVO create(String userId, String goalId, CreateRewardDTO dto) {
        Goal goal = goalService.requireGoal(userId, goalId);
        if (!TRIGGER_TYPES.contains(dto.getTriggerType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "trigger_type 取值非法");
        }
        validateTriggerValue(dto.getTriggerType(), dto.getTriggerValue());
        if (dto.getClientOpId() != null) {
            Reward exist = rewardMapper.selectOne(new LambdaQueryWrapper<Reward>()
                    .eq(Reward::getUserId, userId)
                    .eq(Reward::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) return assembler.toRewardVO(exist, goal);
        }

        int maxSort = rewardMapper.selectList(new LambdaQueryWrapper<Reward>()
                        .eq(Reward::getGoalId, goalId)
                        .orderByDesc(Reward::getSort)
                        .last("LIMIT 1"))
                .stream().map(Reward::getSort).findFirst().orElse(-1);

        Reward r = new Reward();
        r.setId(IdGenerator.reward());
        r.setGoalId(goalId);
        r.setUserId(userId);
        r.setName(dto.getName());
        r.setContent(dto.getContent());
        r.setTriggerType(dto.getTriggerType());
        r.setTriggerValue(dto.getTriggerValue());
        r.setSort(maxSort + 1);
        r.setStatus("locked");
        r.setClientOpId(dto.getClientOpId());
        rewardMapper.insert(r);
        return assembler.toRewardVO(r, goal);
    }

    @Override
    @Transactional
    public RewardVO update(String userId, String goalId, String rewardId, UpdateRewardDTO dto) {
        Goal goal = goalService.requireGoal(userId, goalId);
        Reward r = require(goalId, rewardId);
        if (dto.getName() != null) r.setName(dto.getName());
        if (dto.getContent() != null) r.setContent(dto.getContent());
        if (dto.getTriggerType() != null) {
            if (!TRIGGER_TYPES.contains(dto.getTriggerType())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "trigger_type 取值非法");
            }
            r.setTriggerType(dto.getTriggerType());
        }
        if (dto.getTriggerValue() != null) {
            validateTriggerValue(r.getTriggerType(), dto.getTriggerValue());
            r.setTriggerValue(dto.getTriggerValue());
        }
        rewardMapper.updateById(r);
        return assembler.toRewardVO(r, goal);
    }

    @Override
    public void delete(String userId, String goalId, String rewardId) {
        goalService.requireGoal(userId, goalId);
        Reward r = require(goalId, rewardId);
        rewardMapper.deleteById(r.getId());
    }

    @Override
    @Transactional
    public RewardVO claim(String userId, String goalId, String rewardId) {
        Goal goal = goalService.requireGoal(userId, goalId);
        Reward r = require(goalId, rewardId);
        if (!"available".equals(r.getStatus())) {
            throw new BusinessException(ResultCode.REWARD_NOT_REACHED);
        }
        r.setStatus("claimed");
        r.setClaimedAt(LocalDateTime.now());
        rewardMapper.updateById(r);
        return assembler.toRewardVO(r, goal);
    }

    private Reward require(String goalId, String rewardId) {
        Reward r = rewardMapper.selectById(rewardId);
        if (r == null || !goalId.equals(r.getGoalId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "奖励不存在");
        }
        return r;
    }

    private void validateTriggerValue(String type, int value) {
        switch (type) {
            case "progress" -> {
                if (value < 0 || value > 100) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "progress 触发值需在 0-100 之间");
                }
            }
            case "phase", "days" -> {
                if (value < 1) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, type + " 触发值需 ≥1");
                }
            }
            default -> { /* validated outside */ }
        }
    }
}
