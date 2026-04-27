package com.atomic.focus.modules.checkin.service;

import com.atomic.focus.modules.settings.entity.UserEncouragement;
import com.atomic.focus.modules.settings.mapper.UserEncouragementMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 鼓励语挑选：优先使用用户自定义，其次使用内置话术。
 */
@Service
@RequiredArgsConstructor
public class EncouragementService {

    private static final List<String> DAILY = List.of(
            "太棒啦！今天圆满完成打卡，微小的坚持终会有大改变",
            "完成胜过完美，继续保持这份节奏",
            "你又把『自律』二字往身上多写了一笔");

    private static final List<String> MISSED = List.of(
            "偶尔偷懒没关系，重新打卡，哪怕只完成 1 分钟，也是进步",
            "今日未完成打卡，调整一下状态，明天重新出发就好");

    private static final List<String> REWARD = List.of(
            "恭喜你达成奖励条件！好好享受属于你的奖励，继续奔赴下一个目标吧");

    private static final List<String> FIXED = List.of(
            "习惯固化达成！你已成为『习惯掌控者』");

    private final UserEncouragementMapper encouragementMapper;

    public String pickDaily(String userId) {
        return pick(userId, DAILY);
    }

    public String pickMissed(String userId) {
        return pick(userId, MISSED);
    }

    public String pickReward(String userId) {
        return pick(userId, REWARD);
    }

    public String pickFixed(String userId) {
        return pick(userId, FIXED);
    }

    private String pick(String userId, List<String> defaults) {
        List<UserEncouragement> custom = encouragementMapper.selectList(
                new LambdaQueryWrapper<UserEncouragement>()
                        .eq(UserEncouragement::getUserId, userId));
        if (!custom.isEmpty()) {
            return custom.get(ThreadLocalRandom.current().nextInt(custom.size())).getContent();
        }
        return defaults.get(ThreadLocalRandom.current().nextInt(defaults.size()));
    }
}
