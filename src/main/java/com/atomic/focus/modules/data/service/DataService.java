package com.atomic.focus.modules.data.service;

import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.modules.data.dto.ExportDTO;
import com.atomic.focus.modules.data.dto.ResetDTO;
import com.atomic.focus.modules.data.dto.SyncPushDTO;
import com.atomic.focus.modules.data.vo.HistoryItemVO;
import com.atomic.focus.modules.data.vo.SyncPullResultVO;
import com.atomic.focus.modules.data.vo.SyncPushResultVO;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

public interface DataService {

    PageResult<HistoryItemVO> history(String userId, String kind, String goalId,
                                      LocalDate startDate, LocalDate endDate,
                                      long page, long pageSize);

    Map<String, Object> exportData(String userId, ExportDTO dto);

    Map<String, Object> exportTask(String userId, String taskId);

    SyncPushResultVO push(String userId, SyncPushDTO dto);

    SyncPullResultVO pull(String userId, OffsetDateTime since);

    void reset(String userId, ResetDTO dto);
}
