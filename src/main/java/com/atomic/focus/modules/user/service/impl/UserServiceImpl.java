package com.atomic.focus.modules.user.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.user.dto.UpdateProfileDTO;
import com.atomic.focus.modules.user.entity.User;
import com.atomic.focus.modules.user.entity.UserAchievement;
import com.atomic.focus.modules.user.mapper.UserAchievementMapper;
import com.atomic.focus.modules.user.mapper.UserMapper;
import com.atomic.focus.modules.user.service.UserService;
import com.atomic.focus.modules.user.vo.AchievementVO;
import com.atomic.focus.modules.user.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAchievementMapper achievementMapper;
    private final GoalMapper goalMapper;

    @Override
    public User requireUser(String userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return u;
    }

    @Override
    public UserVO getMe(String userId) {
        User u = requireUser(userId);
        return toVO(u);
    }

    @Override
    public UserVO updateProfile(String userId, UpdateProfileDTO dto) {
        User u = requireUser(userId);
        if (dto.getNickname() != null) {
            u.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            u.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(u);
        return toVO(u);
    }

    @Override
    public Map<String, String> uploadAvatar(String userId, byte[] bytes, String filename) {
        // 占位实现：返回一个伪 URL；真实场景应上传到对象存储后回写
        String url = "https://cdn.atomic.app/u/" + userId + "_" + System.currentTimeMillis() + "_" + filename;
        User u = requireUser(userId);
        u.setAvatar(url);
        userMapper.updateById(u);
        return Map.of("url", url);
    }

    @Override
    public List<AchievementVO> achievements(String userId) {
        return achievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .orderByDesc(UserAchievement::getEarned)
                        .orderByDesc(UserAchievement::getEarnedAt))
                .stream().map(this::toAchievementVO).collect(Collectors.toList());
    }

    @Override
    public void grantBadge(String userId, String key, String name, String desc) {
        UserAchievement existing = achievementMapper.selectOne(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .eq(UserAchievement::getKeyCode, key)
                        .last("LIMIT 1"));
        if (existing == null) {
            UserAchievement a = new UserAchievement();
            a.setUserId(userId);
            a.setKeyCode(key);
            a.setName(name);
            a.setDescription(desc);
            a.setEarned(true);
            a.setEarnedAt(LocalDate.now());
            achievementMapper.insert(a);
        } else if (!Boolean.TRUE.equals(existing.getEarned())) {
            existing.setEarned(true);
            existing.setEarnedAt(LocalDate.now());
            achievementMapper.updateById(existing);
        }
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setUserId(u.getId());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setIsGuest(Boolean.TRUE.equals(u.getIsGuest()));
        vo.setJoinedAt(u.getJoinedAt());

        List<String> badges = achievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, u.getId())
                        .eq(UserAchievement::getEarned, true))
                .stream().map(UserAchievement::getName).collect(Collectors.toList());
        vo.setBadges(badges);

        Long activeGoals = goalMapper.selectCount(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, u.getId())
                .eq(Goal::getArchived, false));

        UserVO.Stats stats = new UserVO.Stats();
        stats.setTotalCheckinDays(u.getTotalCheckinDays());
        stats.setFixedHabitsCount(u.getFixedHabitsCount());
        stats.setActiveGoalsCount(activeGoals == null ? 0 : activeGoals.intValue());
        stats.setContinuousDays(u.getContinuousDays());
        vo.setStats(stats);
        return vo;
    }

    private AchievementVO toAchievementVO(UserAchievement a) {
        AchievementVO vo = new AchievementVO();
        vo.setKey(a.getKeyCode());
        vo.setName(a.getName());
        vo.setDesc(a.getDescription());
        vo.setEarned(Boolean.TRUE.equals(a.getEarned()));
        vo.setEarnedAt(a.getEarnedAt());
        return vo;
    }
}
