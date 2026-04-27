package com.atomic.focus.modules.checkin.service;

import com.atomic.focus.modules.checkin.dto.CreateCheckinDTO;
import com.atomic.focus.modules.checkin.dto.MissedCheckinDTO;
import com.atomic.focus.modules.checkin.vo.CalendarVO;
import com.atomic.focus.modules.checkin.vo.CheckinResultVO;
import com.atomic.focus.modules.checkin.vo.CheckinVO;
import com.atomic.focus.modules.checkin.vo.MissedResultVO;
import com.atomic.focus.modules.checkin.vo.TodayChecklistVO;

import java.time.LocalDate;
import java.util.List;

public interface CheckinService {

    CheckinResultVO checkin(String userId, String goalId, CreateCheckinDTO dto);

    MissedResultVO missed(String userId, String goalId, MissedCheckinDTO dto);

    void delete(String userId, String goalId, LocalDate date);

    List<CheckinVO> list(String userId, String goalId, LocalDate startDate, LocalDate endDate, String status);

    TodayChecklistVO today(String userId);

    CalendarVO calendar(String userId, String goalId, int year, int month);
}
