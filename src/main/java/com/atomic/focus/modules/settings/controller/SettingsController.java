package com.atomic.focus.modules.settings.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.settings.dto.AddEncouragementDTO;
import com.atomic.focus.modules.settings.dto.UpdateSettingsDTO;
import com.atomic.focus.modules.settings.service.SettingsService;
import com.atomic.focus.modules.settings.vo.SettingsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public R<SettingsVO> get() {
        return R.ok(settingsService.get(UserContext.requireUserId()));
    }

    @PatchMapping
    public R<SettingsVO> update(@RequestBody UpdateSettingsDTO dto) {
        return R.ok(settingsService.update(UserContext.requireUserId(), dto));
    }

    @PostMapping("/encouragements")
    public R<Void> addEncouragement(@RequestBody @Valid AddEncouragementDTO dto) {
        settingsService.addEncouragement(UserContext.requireUserId(), dto);
        return R.ok();
    }

    @DeleteMapping("/encouragements/{index}")
    public R<Void> deleteEncouragement(@PathVariable("index") int index) {
        settingsService.deleteEncouragement(UserContext.requireUserId(), index);
        return R.ok();
    }
}
