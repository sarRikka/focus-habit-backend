package com.atomic.focus.modules.dashboard.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.checkin.service.CheckinService;
import com.atomic.focus.modules.checkin.vo.CalendarVO;
import com.atomic.focus.modules.dashboard.service.DashboardService;
import com.atomic.focus.modules.dashboard.vo.DashboardVO;
import com.atomic.focus.modules.dashboard.vo.GoalProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CheckinService checkinService;

    @GetMapping("/dashboard")
    public R<DashboardVO> dashboard() {
        return R.ok(dashboardService.dashboard(UserContext.requireUserId()));
    }

    @GetMapping("/goals/{goalId}/progress")
    public R<GoalProgressVO> progress(@PathVariable("goalId") String goalId) {
        return R.ok(dashboardService.progress(UserContext.requireUserId(), goalId));
    }

    @GetMapping("/goals/{goalId}/calendar")
    public R<CalendarVO> calendar(@PathVariable("goalId") String goalId,
                                  @RequestParam(value = "year", required = false) Integer year,
                                  @RequestParam(value = "month", required = false) Integer month) {
        LocalDate today = LocalDate.now();
        int y = year == null ? today.getYear() : year;
        int m = month == null ? today.getMonthValue() : month;
        return R.ok(checkinService.calendar(UserContext.requireUserId(), goalId, y, m));
    }
}
