package com.atomic.focus.modules.notification.service.impl;

import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.modules.notification.dto.MarkReadDTO;
import com.atomic.focus.modules.notification.dto.RegisterDeviceDTO;
import com.atomic.focus.modules.notification.entity.Notification;
import com.atomic.focus.modules.notification.entity.NotificationDevice;
import com.atomic.focus.modules.notification.mapper.NotificationDeviceMapper;
import com.atomic.focus.modules.notification.mapper.NotificationMapper;
import com.atomic.focus.modules.notification.service.NotificationService;
import com.atomic.focus.modules.notification.vo.NotificationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationDeviceMapper deviceMapper;

    @Override
    public void registerDevice(String userId, RegisterDeviceDTO dto) {
        NotificationDevice exist = deviceMapper.selectOne(new LambdaQueryWrapper<NotificationDevice>()
                .eq(NotificationDevice::getUserId, userId)
                .eq(NotificationDevice::getPushToken, dto.getPushToken())
                .last("LIMIT 1"));
        if (exist != null) {
            exist.setPlatform(dto.getPlatform());
            exist.setDeviceId(dto.getDeviceId());
            deviceMapper.updateById(exist);
            return;
        }
        NotificationDevice d = new NotificationDevice();
        d.setUserId(userId);
        d.setPlatform(dto.getPlatform());
        d.setPushToken(dto.getPushToken());
        d.setDeviceId(dto.getDeviceId());
        deviceMapper.insert(d);
    }

    @Override
    public PageResult<NotificationVO> list(String userId, Boolean unreadOnly, long page, long pageSize) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (Boolean.TRUE.equals(unreadOnly)) w.eq(Notification::getIsRead, false);
        w.orderByDesc(Notification::getCreatedAt);
        Page<Notification> p = Page.of(page, pageSize);
        Page<Notification> r = notificationMapper.selectPage(p, w);
        List<NotificationVO> items = r.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(items, r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Override
    public void markRead(String userId, MarkReadDTO dto) {
        if (Boolean.TRUE.equals(dto.getAll())) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getUserId, userId)
                    .set(Notification::getIsRead, true));
            return;
        }
        if (dto.getNotificationIds() != null && !dto.getNotificationIds().isEmpty()) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getUserId, userId)
                    .in(Notification::getId, dto.getNotificationIds())
                    .set(Notification::getIsRead, true));
        }
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setEvent(n.getEvent());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setIsRead(Boolean.TRUE.equals(n.getIsRead()));
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
