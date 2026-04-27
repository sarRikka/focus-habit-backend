package com.atomic.focus.modules.notification.service;

import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.modules.notification.dto.MarkReadDTO;
import com.atomic.focus.modules.notification.dto.RegisterDeviceDTO;
import com.atomic.focus.modules.notification.vo.NotificationVO;

public interface NotificationService {

    void registerDevice(String userId, RegisterDeviceDTO dto);

    PageResult<NotificationVO> list(String userId, Boolean unreadOnly, long page, long pageSize);

    void markRead(String userId, MarkReadDTO dto);
}
