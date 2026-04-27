package com.atomic.focus.modules.settings.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.modules.settings.dto.AddEncouragementDTO;
import com.atomic.focus.modules.settings.dto.UpdateSettingsDTO;
import com.atomic.focus.modules.settings.entity.UserEncouragement;
import com.atomic.focus.modules.settings.entity.UserSettings;
import com.atomic.focus.modules.settings.mapper.UserEncouragementMapper;
import com.atomic.focus.modules.settings.mapper.UserSettingsMapper;
import com.atomic.focus.modules.settings.service.SettingsService;
import com.atomic.focus.modules.settings.vo.SettingsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private static final Set<String> THEMES = Set.of("light", "dark");
    private static final Set<String> RETENTIONS = Set.of("1y", "3y", "5y", "forever");

    private final UserSettingsMapper settingsMapper;
    private final UserEncouragementMapper encouragementMapper;

    @Override
    public void initDefaults(String userId) {
        if (settingsMapper.selectById(userId) != null) {
            return;
        }
        UserSettings s = new UserSettings();
        s.setUserId(userId);
        s.setReminderTime("19:00");
        s.setReminderRepeat(3);
        s.setReviewReminderEnabled(true);
        s.setReviewReminderTime("19:00");
        s.setPushEnabled(true);
        s.setTheme("light");
        s.setDataRetention("1y");
        s.setDefaultProgressDeduction(1);
        settingsMapper.insert(s);
    }

    @Override
    public SettingsVO get(String userId) {
        UserSettings s = getRaw(userId);
        return toVO(s, listEncouragements(userId));
    }

    @Override
    public SettingsVO update(String userId, UpdateSettingsDTO dto) {
        UserSettings s = getRaw(userId);
        if (dto.getReminderTime() != null) {
            validateHm(dto.getReminderTime(), "reminder_time");
            s.setReminderTime(dto.getReminderTime());
        }
        if (dto.getReminderRepeat() != null) {
            validateRange(dto.getReminderRepeat(), 1, 3, "reminder_repeat");
            s.setReminderRepeat(dto.getReminderRepeat());
        }
        if (dto.getReviewReminderEnabled() != null) {
            s.setReviewReminderEnabled(dto.getReviewReminderEnabled());
        }
        if (dto.getReviewReminderTime() != null) {
            validateHm(dto.getReviewReminderTime(), "review_reminder_time");
            s.setReviewReminderTime(dto.getReviewReminderTime());
        }
        if (dto.getPushEnabled() != null) {
            s.setPushEnabled(dto.getPushEnabled());
        }
        if (dto.getTheme() != null) {
            if (!THEMES.contains(dto.getTheme())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "theme 取值非法");
            }
            s.setTheme(dto.getTheme());
        }
        if (dto.getDataRetention() != null) {
            if (!RETENTIONS.contains(dto.getDataRetention())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "data_retention 取值非法");
            }
            s.setDataRetention(dto.getDataRetention());
        }
        if (dto.getDefaultProgressDeduction() != null) {
            validateRange(dto.getDefaultProgressDeduction(), 1, 5, "default_progress_deduction");
            s.setDefaultProgressDeduction(dto.getDefaultProgressDeduction());
        }
        settingsMapper.updateById(s);
        return toVO(s, listEncouragements(userId));
    }

    @Override
    public UserSettings getRaw(String userId) {
        UserSettings s = settingsMapper.selectById(userId);
        if (s == null) {
            initDefaults(userId);
            s = settingsMapper.selectById(userId);
        }
        return s;
    }

    @Override
    public void addEncouragement(String userId, AddEncouragementDTO dto) {
        UserEncouragement e = new UserEncouragement();
        e.setUserId(userId);
        e.setContent(dto.getContent());
        Integer maxSort = encouragementMapper.selectList(
                new LambdaQueryWrapper<UserEncouragement>()
                        .eq(UserEncouragement::getUserId, userId)
                        .orderByDesc(UserEncouragement::getSort)
                        .last("LIMIT 1"))
                .stream().map(UserEncouragement::getSort).findFirst().orElse(0);
        e.setSort(maxSort + 1);
        encouragementMapper.insert(e);
    }

    @Override
    public void deleteEncouragement(String userId, int index) {
        List<UserEncouragement> list = encouragementMapper.selectList(
                new LambdaQueryWrapper<UserEncouragement>()
                        .eq(UserEncouragement::getUserId, userId)
                        .orderByAsc(UserEncouragement::getSort)
                        .orderByAsc(UserEncouragement::getId));
        if (index < 0 || index >= list.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "鼓励语下标越界");
        }
        encouragementMapper.deleteById(list.get(index).getId());
    }

    // ---------- private ----------

    private List<String> listEncouragements(String userId) {
        return encouragementMapper.selectList(
                new LambdaQueryWrapper<UserEncouragement>()
                        .eq(UserEncouragement::getUserId, userId)
                        .orderByAsc(UserEncouragement::getSort)
                        .orderByAsc(UserEncouragement::getId))
                .stream().map(UserEncouragement::getContent).collect(Collectors.toList());
    }

    private SettingsVO toVO(UserSettings s, List<String> encouragements) {
        SettingsVO vo = new SettingsVO();
        vo.setReminderTime(s.getReminderTime());
        vo.setReminderRepeat(s.getReminderRepeat());
        vo.setReviewReminderEnabled(s.getReviewReminderEnabled());
        vo.setReviewReminderTime(s.getReviewReminderTime());
        vo.setPushEnabled(s.getPushEnabled());
        vo.setTheme(s.getTheme());
        vo.setDataRetention(s.getDataRetention());
        vo.setDefaultProgressDeduction(s.getDefaultProgressDeduction());
        vo.setCustomEncouragements(encouragements);
        return vo;
    }

    private void validateHm(String value, String field) {
        if (!value.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, field + " 必须是 HH:mm 格式");
        }
    }

    private void validateRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new BusinessException(ResultCode.PARAM_INVALID, field + " 必须在 " + min + "-" + max + " 范围内");
        }
    }
}
