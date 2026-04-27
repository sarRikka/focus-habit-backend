package com.atomic.focus.modules.scene.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.scene.dto.CreateSceneDTO;
import com.atomic.focus.modules.scene.dto.UpdateSceneDTO;
import com.atomic.focus.modules.scene.service.SceneService;
import com.atomic.focus.modules.scene.vo.SceneListVO;
import com.atomic.focus.modules.scene.vo.SceneVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @GetMapping
    public R<SceneListVO> list() {
        return R.ok(sceneService.list(UserContext.requireUserId()));
    }

    @PostMapping
    public R<SceneVO> create(@RequestBody @Valid CreateSceneDTO dto) {
        return R.ok(sceneService.create(UserContext.requireUserId(), dto));
    }

    @PatchMapping("/{sceneId}")
    public R<SceneVO> update(@PathVariable("sceneId") String sceneId,
                             @RequestBody UpdateSceneDTO dto) {
        return R.ok(sceneService.update(UserContext.requireUserId(), sceneId, dto));
    }

    @DeleteMapping("/{sceneId}")
    public R<Void> delete(@PathVariable("sceneId") String sceneId) {
        sceneService.delete(UserContext.requireUserId(), sceneId);
        return R.ok();
    }
}
