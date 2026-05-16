package com.atomic.focus.modules.auth.service.impl;

import com.atomic.focus.modules.auth.mapper.GuestDataMergeMapper;
import com.atomic.focus.modules.auth.service.GuestMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 参见 API.md §15.2 merge_guest_user_id。
 */
@Service
@RequiredArgsConstructor
public class GuestMergeServiceImpl implements GuestMergeService {

    private final GuestDataMergeMapper guestDataMergeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeGuestIntoUser(String guestUserId, String targetUserId) {
        if (guestUserId.equals(targetUserId)) {
            return;
        }
        guestDataMergeMapper.deleteAchievementConflicts(guestUserId, targetUserId);
        guestDataMergeMapper.deleteSyncOpConflicts(guestUserId, targetUserId);
        guestDataMergeMapper.deleteDeviceConflicts(guestUserId, targetUserId);

        guestDataMergeMapper.reassignGoals(guestUserId, targetUserId);
        guestDataMergeMapper.reassignPhases(guestUserId, targetUserId);
        guestDataMergeMapper.reassignRewards(guestUserId, targetUserId);
        guestDataMergeMapper.reassignCheckins(guestUserId, targetUserId);
        guestDataMergeMapper.reassignReviews(guestUserId, targetUserId);
        guestDataMergeMapper.reassignScenes(guestUserId, targetUserId);
        guestDataMergeMapper.reassignNotifications(guestUserId, targetUserId);
        guestDataMergeMapper.reassignNotificationDevices(guestUserId, targetUserId);
        guestDataMergeMapper.reassignSyncOpLogs(guestUserId, targetUserId);
        guestDataMergeMapper.reassignAuditLogs(guestUserId, targetUserId);
        guestDataMergeMapper.reassignEncouragements(guestUserId, targetUserId);
        guestDataMergeMapper.reassignAchievements(guestUserId, targetUserId);

        guestDataMergeMapper.deleteGuestSettings(guestUserId);
        guestDataMergeMapper.hardDeleteUser(guestUserId);
    }
}
