package com.atomic.focus.modules.auth.service;

/**
 * 将游客的各类业务数据归并到已存在的正式账号下，并硬删游客主表及相关设置。
 */
public interface GuestMergeService {

    /** 把 guestUserId 下的数据迁入 targetUserId，然后删除 guest 用户及其 user_settings（业务数据已全部迁走）。 */
    void mergeGuestIntoUser(String guestUserId, String targetUserId);
}
