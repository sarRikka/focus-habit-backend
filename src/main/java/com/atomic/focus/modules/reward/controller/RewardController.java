package com.atomic.focus.modules.reward.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.reward.dto.CreateRewardDTO;
import com.atomic.focus.modules.reward.dto.UpdateRewardDTO;
import com.atomic.focus.modules.reward.service.RewardService;
import com.atomic.focus.modules.reward.vo.RewardVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/goals/{goalId}/rewards")
    public R<List<RewardVO>> list(@PathVariable("goalId") String goalId) {
        return R.ok(rewardService.listByGoal(UserContext.requireUserId(), goalId));
    }

    @PostMapping("/goals/{goalId}/rewards")
    public R<RewardVO> create(@PathVariable("goalId") String goalId,
                              @RequestBody @Valid CreateRewardDTO dto) {
        return R.ok(rewardService.create(UserContext.requireUserId(), goalId, dto));
    }

    @PatchMapping("/goals/{goalId}/rewards/{rewardId}")
    public R<RewardVO> update(@PathVariable("goalId") String goalId,
                              @PathVariable("rewardId") String rewardId,
                              @RequestBody UpdateRewardDTO dto) {
        return R.ok(rewardService.update(UserContext.requireUserId(), goalId, rewardId, dto));
    }

    @DeleteMapping("/goals/{goalId}/rewards/{rewardId}")
    public R<Void> delete(@PathVariable("goalId") String goalId,
                          @PathVariable("rewardId") String rewardId) {
        rewardService.delete(UserContext.requireUserId(), goalId, rewardId);
        return R.ok();
    }

    @PostMapping("/goals/{goalId}/rewards/{rewardId}/claim")
    public R<RewardVO> claim(@PathVariable("goalId") String goalId,
                             @PathVariable("rewardId") String rewardId) {
        return R.ok(rewardService.claim(UserContext.requireUserId(), goalId, rewardId));
    }

    @GetMapping("/rewards")
    public R<PageResult<RewardVO>> all(@RequestParam(value = "status", required = false) String status) {
        List<RewardVO> items = rewardService.listAll(UserContext.requireUserId(), status);
        return R.ok(PageResult.of(items, 1, items.size(), items.size()));
    }
}
