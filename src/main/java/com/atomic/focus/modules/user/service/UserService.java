package com.atomic.focus.modules.user.service;

import com.atomic.focus.modules.user.dto.UpdateProfileDTO;
import com.atomic.focus.modules.user.entity.User;
import com.atomic.focus.modules.user.vo.AchievementVO;
import com.atomic.focus.modules.user.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface UserService {

    User requireUser(String userId);

    UserVO getMe(String userId);

    UserVO updateProfile(String userId, UpdateProfileDTO dto);

    Map<String, String> uploadAvatar(String userId, byte[] bytes, String filename);

    List<AchievementVO> achievements(String userId);

    /** 标记某个标签已获得（业务事件触发） */
    void grantBadge(String userId, String key, String name, String desc);
}
