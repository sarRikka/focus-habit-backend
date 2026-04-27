package com.atomic.focus.modules.goal.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.goal.dto.CreateGoalDTO;
import com.atomic.focus.modules.goal.dto.UpdateGoalDTO;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.goal.vo.GoalVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public R<GoalVO> create(@RequestBody @Valid CreateGoalDTO dto) {
        return goalService.create(UserContext.requireUserId(), dto);
    }

    @GetMapping
    public R<PageResult<GoalVO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize) {
        return R.ok(goalService.list(UserContext.requireUserId(), status, category, keyword, page, pageSize));
    }

    @GetMapping("/{goalId}")
    public R<GoalVO> detail(@PathVariable("goalId") String goalId) {
        return R.ok(goalService.detail(UserContext.requireUserId(), goalId));
    }

    @PatchMapping("/{goalId}")
    public R<GoalVO> update(@PathVariable("goalId") String goalId,
                            @RequestBody UpdateGoalDTO dto) {
        return R.ok(goalService.update(UserContext.requireUserId(), goalId, dto));
    }

    @PostMapping("/{goalId}/archive")
    public R<Void> archive(@PathVariable("goalId") String goalId) {
        goalService.archive(UserContext.requireUserId(), goalId);
        return R.ok();
    }

    @DeleteMapping("/{goalId}")
    public R<Void> delete(@PathVariable("goalId") String goalId,
                          @RequestParam(value = "hard", defaultValue = "false") boolean hard) {
        goalService.delete(UserContext.requireUserId(), goalId, hard);
        return R.ok();
    }
}
