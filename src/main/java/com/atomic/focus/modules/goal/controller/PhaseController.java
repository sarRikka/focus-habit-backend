package com.atomic.focus.modules.goal.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.goal.dto.CreatePhaseDTO;
import com.atomic.focus.modules.goal.dto.UpdatePhaseDTO;
import com.atomic.focus.modules.goal.service.PhaseService;
import com.atomic.focus.modules.goal.vo.PhaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals/{goalId}/phases")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    @GetMapping
    public R<List<PhaseVO>> list(@PathVariable("goalId") String goalId) {
        return R.ok(phaseService.list(UserContext.requireUserId(), goalId));
    }

    @PostMapping
    public R<PhaseVO> create(@PathVariable("goalId") String goalId,
                             @RequestBody @Valid CreatePhaseDTO dto) {
        return R.ok(phaseService.create(UserContext.requireUserId(), goalId, dto));
    }

    @PatchMapping("/{phaseId}")
    public R<PhaseVO> update(@PathVariable("goalId") String goalId,
                             @PathVariable("phaseId") String phaseId,
                             @RequestBody UpdatePhaseDTO dto) {
        return R.ok(phaseService.update(UserContext.requireUserId(), goalId, phaseId, dto));
    }

    @DeleteMapping("/{phaseId}")
    public R<Void> delete(@PathVariable("goalId") String goalId,
                          @PathVariable("phaseId") String phaseId) {
        phaseService.delete(UserContext.requireUserId(), goalId, phaseId);
        return R.ok();
    }

    @PostMapping("/{phaseId}/complete")
    public R<PhaseVO> complete(@PathVariable("goalId") String goalId,
                               @PathVariable("phaseId") String phaseId) {
        return R.ok(phaseService.complete(UserContext.requireUserId(), goalId, phaseId));
    }
}
