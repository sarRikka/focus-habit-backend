package com.atomic.focus.modules.checkin.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.checkin.dto.CreateCheckinDTO;
import com.atomic.focus.modules.checkin.dto.MissedCheckinDTO;
import com.atomic.focus.modules.checkin.service.CheckinService;
import com.atomic.focus.modules.checkin.vo.CheckinResultVO;
import com.atomic.focus.modules.checkin.vo.CheckinVO;
import com.atomic.focus.modules.checkin.vo.MissedResultVO;
import com.atomic.focus.modules.checkin.vo.TodayChecklistVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/goals/{goalId}/checkins")
    public R<CheckinResultVO> create(@PathVariable("goalId") String goalId,
                                     @RequestBody @Valid CreateCheckinDTO dto) {
        return R.ok(checkinService.checkin(UserContext.requireUserId(), goalId, dto));
    }

    @PostMapping("/goals/{goalId}/checkins/missed")
    public R<MissedResultVO> missed(@PathVariable("goalId") String goalId,
                                    @RequestBody MissedCheckinDTO dto) {
        return R.ok(checkinService.missed(UserContext.requireUserId(), goalId, dto));
    }

    @DeleteMapping("/goals/{goalId}/checkins/{date}")
    public R<Void> delete(@PathVariable("goalId") String goalId,
                          @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        checkinService.delete(UserContext.requireUserId(), goalId, date);
        return R.ok();
    }

    @GetMapping("/goals/{goalId}/checkins")
    public R<List<CheckinVO>> list(@PathVariable("goalId") String goalId,
                                   @RequestParam(value = "start_date", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(value = "end_date", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(value = "status", required = false) String status) {
        return R.ok(checkinService.list(UserContext.requireUserId(), goalId, startDate, endDate, status));
    }

    @GetMapping("/checkins/today")
    public R<TodayChecklistVO> today() {
        return R.ok(checkinService.today(UserContext.requireUserId()));
    }
}
