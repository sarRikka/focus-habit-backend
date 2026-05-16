package com.atomic.focus.modules.checkin.service;

import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.scene.entity.Scene;
import com.atomic.focus.modules.scene.mapper.SceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * PRD §7.5 / API.md §17：当日「有效」每日习惯时长（考虑 shorten 场景）。
 */
@Component
@RequiredArgsConstructor
public class EffectiveDailyTargetMinutesResolver {

    private final SceneMapper sceneMapper;

    /**
     * 习惯每日时长（至少 1）；若有打卡日当天生效的 shorten 场景，取 min(习惯, shorten_to)。
     */
    public int resolveEffectiveDailyTarget(Goal goal, LocalDate date, String userId) {
        int habit = goal.getDhDuration() == null ? 10 : goal.getDhDuration();
        habit = Math.max(1, habit);

        List<Scene> scenes = sceneMapper.selectList(new LambdaQueryWrapper<Scene>()
                .eq(Scene::getUserId, userId)
                .le(Scene::getStartDate, date)
                .ge(Scene::getEndDate, date)
                .eq(Scene::getMode, "shorten"));

        int effective = habit;
        for (Scene sc : scenes) {
            Integer st = sc.getShortenTo();
            if (st != null && st >= 1) {
                effective = Math.min(effective, st);
            }
        }
        return Math.max(1, effective);
    }

    /** ⌈effective/2⌉，至少 1（与客户端计时拦截一致）。 */
    public static int minimumCompletedMinutes(int effectiveDailyMinutes) {
        int e = Math.max(1, effectiveDailyMinutes);
        return Math.max(1, (e + 1) / 2);
    }
}
