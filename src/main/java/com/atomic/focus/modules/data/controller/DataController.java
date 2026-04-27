package com.atomic.focus.modules.data.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.data.dto.ExportDTO;
import com.atomic.focus.modules.data.dto.ResetDTO;
import com.atomic.focus.modules.data.dto.SyncPushDTO;
import com.atomic.focus.modules.data.service.DataService;
import com.atomic.focus.modules.data.vo.HistoryItemVO;
import com.atomic.focus.modules.data.vo.SyncPullResultVO;
import com.atomic.focus.modules.data.vo.SyncPushResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping("/history")
    public R<PageResult<HistoryItemVO>> history(
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "goal_id", required = false) String goalId,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize) {
        return R.ok(dataService.history(UserContext.requireUserId(), kind, goalId,
                startDate, endDate, page, pageSize));
    }

    @PostMapping("/data/export")
    public R<Map<String, Object>> exportData(@RequestBody ExportDTO dto) {
        return R.ok(dataService.exportData(UserContext.requireUserId(), dto));
    }

    @GetMapping("/data/export/{taskId}")
    public R<Map<String, Object>> exportTask(@PathVariable("taskId") String taskId) {
        return R.ok(dataService.exportTask(UserContext.requireUserId(), taskId));
    }

    @PostMapping("/sync/push")
    public R<SyncPushResultVO> syncPush(@RequestBody SyncPushDTO dto) {
        return R.ok(dataService.push(UserContext.requireUserId(), dto));
    }

    @GetMapping("/sync/pull")
    public R<SyncPullResultVO> syncPull(
            @RequestParam(value = "since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        return R.ok(dataService.pull(UserContext.requireUserId(), since));
    }

    @PostMapping("/data/reset")
    public R<Void> reset(@RequestBody @Valid ResetDTO dto) {
        dataService.reset(UserContext.requireUserId(), dto);
        return R.ok();
    }
}
