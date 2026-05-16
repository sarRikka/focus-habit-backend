package com.atomic.focus.modules.auth.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 将游客数据并入正式账号：按表更新 user_id；处理唯一键冲突后硬删游客主表。
 */
@Mapper
public interface GuestDataMergeMapper {

    @Delete("""
            DELETE FROM user_achievement
            WHERE user_id = #{guestId}
              AND key_code IN (
                SELECT key_code FROM (
                  SELECT key_code FROM user_achievement WHERE user_id = #{targetId}
                ) t
              )""")
    int deleteAchievementConflicts(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Delete("""
            DELETE FROM sync_op_log
            WHERE user_id = #{guestId}
              AND client_op_id IN (
                SELECT client_op_id FROM (
                  SELECT client_op_id FROM sync_op_log WHERE user_id = #{targetId}
                ) t
              )""")
    int deleteSyncOpConflicts(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Delete("""
            DELETE FROM notification_device
            WHERE user_id = #{guestId}
              AND push_token IN (
                SELECT push_token FROM (
                  SELECT push_token FROM notification_device WHERE user_id = #{targetId}
                ) t
              )""")
    int deleteDeviceConflicts(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE goal SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignGoals(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE phase SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignPhases(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE reward SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignRewards(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE checkin SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignCheckins(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE review SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignReviews(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE scene SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignScenes(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE notification SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignNotifications(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE notification_device SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignNotificationDevices(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE sync_op_log SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignSyncOpLogs(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE audit_log SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignAuditLogs(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE user_encouragement SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignEncouragements(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Update("UPDATE user_achievement SET user_id = #{targetId} WHERE user_id = #{guestId}")
    void reassignAchievements(@Param("guestId") String guestId, @Param("targetId") String targetId);

    @Delete("DELETE FROM user_settings WHERE user_id = #{guestId}")
    void deleteGuestSettings(@Param("guestId") String guestId);

    @Delete("DELETE FROM `user` WHERE id = #{guestId}")
    void hardDeleteUser(@Param("guestId") String guestId);
}
