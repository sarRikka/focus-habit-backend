package com.atomic.focus.modules.goal.service;

import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.goal.dto.CreateGoalDTO;
import com.atomic.focus.modules.goal.dto.UpdateGoalDTO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.vo.GoalVO;

public interface GoalService {

    R<GoalVO> create(String userId, CreateGoalDTO dto);

    PageResult<GoalVO> list(String userId, String status, String category, String keyword, long page, long pageSize);

    GoalVO detail(String userId, String goalId);

    GoalVO update(String userId, String goalId, UpdateGoalDTO dto);

    void archive(String userId, String goalId);

    void delete(String userId, String goalId, boolean hard);

    Goal requireGoal(String userId, String goalId);
}
