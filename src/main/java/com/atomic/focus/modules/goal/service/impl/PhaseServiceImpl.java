package com.atomic.focus.modules.goal.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.goal.dto.CreatePhaseDTO;
import com.atomic.focus.modules.goal.dto.UpdatePhaseDTO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.atomic.focus.modules.goal.service.GoalAssembler;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.goal.service.PhaseService;
import com.atomic.focus.modules.goal.vo.PhaseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {

    private final PhaseMapper phaseMapper;
    private final GoalService goalService;
    private final GoalAssembler assembler;

    @Override
    public List<PhaseVO> list(String userId, String goalId) {
        goalService.requireGoal(userId, goalId);
        return phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                        .eq(Phase::getGoalId, goalId)
                        .orderByAsc(Phase::getSort)
                        .orderByAsc(Phase::getStartDate))
                .stream().map(assembler::toPhaseVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PhaseVO create(String userId, String goalId, CreatePhaseDTO dto) {
        goalService.requireGoal(userId, goalId);
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException(ResultCode.PHASE_DATE_INVALID, "end_date < start_date");
        }
        Long maxSort = phaseMapper.selectList(new LambdaQueryWrapper<Phase>()
                        .eq(Phase::getGoalId, goalId)
                        .orderByDesc(Phase::getSort).last("LIMIT 1"))
                .stream().map(p -> (long) p.getSort()).findFirst().orElse(-1L);

        Phase phase = new Phase();
        phase.setId(IdGenerator.phase());
        phase.setGoalId(goalId);
        phase.setUserId(userId);
        phase.setName(dto.getName());
        phase.setDescription(dto.getDescription());
        phase.setTotalMinutes(dto.getTotalMinutes() == null ? 0 : dto.getTotalMinutes());
        phase.setStartDate(dto.getStartDate());
        phase.setEndDate(dto.getEndDate());
        phase.setSort((int) (maxSort + 1));
        phase.setCompleted(false);
        phaseMapper.insert(phase);
        return assembler.toPhaseVO(phase);
    }

    @Override
    @Transactional
    public PhaseVO update(String userId, String goalId, String phaseId, UpdatePhaseDTO dto) {
        Phase phase = require(userId, goalId, phaseId);
        if (dto.getName() != null) phase.setName(dto.getName());
        if (dto.getDescription() != null) phase.setDescription(dto.getDescription());
        if (dto.getTotalMinutes() != null) phase.setTotalMinutes(dto.getTotalMinutes());
        if (dto.getStartDate() != null) phase.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) phase.setEndDate(dto.getEndDate());
        if (phase.getEndDate().isBefore(phase.getStartDate())) {
            throw new BusinessException(ResultCode.PHASE_DATE_INVALID, "end_date < start_date");
        }
        phaseMapper.updateById(phase);
        return assembler.toPhaseVO(phase);
    }

    @Override
    public void delete(String userId, String goalId, String phaseId) {
        Phase phase = require(userId, goalId, phaseId);
        phaseMapper.deleteById(phase.getId());
    }

    @Override
    @Transactional
    public PhaseVO complete(String userId, String goalId, String phaseId) {
        Phase phase = require(userId, goalId, phaseId);
        if (!Boolean.TRUE.equals(phase.getCompleted())) {
            phase.setCompleted(true);
            phase.setCompletedAt(LocalDateTime.now());
            phaseMapper.updateById(phase);
        }
        return assembler.toPhaseVO(phase);
    }

    private Phase require(String userId, String goalId, String phaseId) {
        Goal goal = goalService.requireGoal(userId, goalId);
        Phase phase = phaseMapper.selectById(phaseId);
        if (phase == null || !goal.getId().equals(phase.getGoalId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阶段不存在");
        }
        return phase;
    }
}
