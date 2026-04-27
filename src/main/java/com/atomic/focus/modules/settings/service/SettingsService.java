package com.atomic.focus.modules.settings.service;

import com.atomic.focus.modules.settings.dto.AddEncouragementDTO;
import com.atomic.focus.modules.settings.dto.UpdateSettingsDTO;
import com.atomic.focus.modules.settings.entity.UserSettings;
import com.atomic.focus.modules.settings.vo.SettingsVO;

public interface SettingsService {

    /** 用户首次创建时初始化默认设置 */
    void initDefaults(String userId);

    SettingsVO get(String userId);

    SettingsVO update(String userId, UpdateSettingsDTO dto);

    UserSettings getRaw(String userId);

    void addEncouragement(String userId, AddEncouragementDTO dto);

    void deleteEncouragement(String userId, int index);
}
