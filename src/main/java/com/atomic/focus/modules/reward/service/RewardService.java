package com.atomic.focus.modules.reward.service;

import com.atomic.focus.modules.reward.dto.CreateRewardDTO;
import com.atomic.focus.modules.reward.dto.UpdateRewardDTO;
import com.atomic.focus.modules.reward.vo.RewardVO;

import java.util.List;

public interface RewardService {

    List<RewardVO> listByGoal(String userId, String goalId);

    List<RewardVO> listAll(String userId, String status);

    RewardVO create(String userId, String goalId, CreateRewardDTO dto);

    RewardVO update(String userId, String goalId, String rewardId, UpdateRewardDTO dto);

    void delete(String userId, String goalId, String rewardId);

    RewardVO claim(String userId, String goalId, String rewardId);
}
