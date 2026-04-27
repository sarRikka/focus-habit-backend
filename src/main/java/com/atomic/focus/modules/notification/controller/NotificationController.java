package com.atomic.focus.modules.notification.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.notification.dto.MarkReadDTO;
import com.atomic.focus.modules.notification.dto.RegisterDeviceDTO;
import com.atomic.focus.modules.notification.service.NotificationService;
import com.atomic.focus.modules.notification.vo.NotificationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/devices")
    public R<Void> registerDevice(@RequestBody @Valid RegisterDeviceDTO dto) {
        notificationService.registerDevice(UserContext.requireUserId(), dto);
        return R.ok();
    }

    @GetMapping
    public R<PageResult<NotificationVO>> list(
            @RequestParam(value = "unread_only", defaultValue = "false") Boolean unreadOnly,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize) {
        return R.ok(notificationService.list(UserContext.requireUserId(), unreadOnly, page, pageSize));
    }

    @PostMapping("/read")
    public R<Void> markRead(@RequestBody MarkReadDTO dto) {
        notificationService.markRead(UserContext.requireUserId(), dto);
        return R.ok();
    }
}
