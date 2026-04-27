package com.atomic.focus.modules.goal.service;

import com.atomic.focus.modules.goal.dto.CreatePhaseDTO;
import com.atomic.focus.modules.goal.dto.UpdatePhaseDTO;
import com.atomic.focus.modules.goal.vo.PhaseVO;

import java.util.List;

public interface PhaseService {

    List<PhaseVO> list(String userId, String goalId);

    PhaseVO create(String userId, String goalId, CreatePhaseDTO dto);

    PhaseVO update(String userId, String goalId, String phaseId, UpdatePhaseDTO dto);

    void delete(String userId, String goalId, String phaseId);

    PhaseVO complete(String userId, String goalId, String phaseId);
}
