package com.atomic.focus.modules.dashboard.service;

import com.atomic.focus.modules.dashboard.vo.DashboardVO;
import com.atomic.focus.modules.dashboard.vo.GoalProgressVO;

public interface DashboardService {

    DashboardVO dashboard(String userId);

    GoalProgressVO progress(String userId, String goalId);
}
