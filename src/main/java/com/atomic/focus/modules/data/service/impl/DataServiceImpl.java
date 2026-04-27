package com.atomic.focus.modules.data.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.common.util.JsonUtil;
import com.atomic.focus.modules.checkin.dto.CreateCheckinDTO;
import com.atomic.focus.modules.checkin.dto.MissedCheckinDTO;
import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.checkin.service.CheckinService;
import com.atomic.focus.modules.data.dto.ExportDTO;
import com.atomic.focus.modules.data.dto.ResetDTO;
import com.atomic.focus.modules.data.dto.SyncPushDTO;
import com.atomic.focus.modules.data.entity.SyncOpLog;
import com.atomic.focus.modules.data.mapper.SyncOpLogMapper;
import com.atomic.focus.modules.data.service.DataService;
import com.atomic.focus.modules.data.vo.HistoryItemVO;
import com.atomic.focus.modules.data.vo.SyncPullResultVO;
import com.atomic.focus.modules.data.vo.SyncPushResultVO;
import com.atomic.focus.modules.goal.dto.CreateGoalDTO;
import com.atomic.focus.modules.goal.dto.UpdateGoalDTO;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.entity.Phase;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.goal.mapper.PhaseMapper;
import com.atomic.focus.modules.goal.service.GoalService;
import com.atomic.focus.modules.review.dto.CreateReviewDTO;
import com.atomic.focus.modules.review.dto.FavoriteDTO;
import com.atomic.focus.modules.review.dto.UpdateReviewDTO;
import com.atomic.focus.modules.review.entity.Review;
import com.atomic.focus.modules.review.mapper.ReviewMapper;
import com.atomic.focus.modules.review.service.ReviewService;
import com.atomic.focus.modules.reward.dto.CreateRewardDTO;
import com.atomic.focus.modules.reward.dto.UpdateRewardDTO;
import com.atomic.focus.modules.reward.entity.Reward;
import com.atomic.focus.modules.reward.mapper.RewardMapper;
import com.atomic.focus.modules.reward.service.RewardService;
import com.atomic.focus.modules.scene.dto.CreateSceneDTO;
import com.atomic.focus.modules.scene.dto.UpdateSceneDTO;
import com.atomic.focus.modules.scene.entity.Scene;
import com.atomic.focus.modules.scene.mapper.SceneMapper;
import com.atomic.focus.modules.scene.service.SceneService;
import com.atomic.focus.modules.settings.dto.UpdateSettingsDTO;
import com.atomic.focus.modules.settings.entity.UserEncouragement;
import com.atomic.focus.modules.settings.mapper.UserEncouragementMapper;
import com.atomic.focus.modules.settings.mapper.UserSettingsMapper;
import com.atomic.focus.modules.settings.service.SettingsService;
import com.atomic.focus.modules.user.dto.UpdateProfileDTO;
import com.atomic.focus.modules.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final GoalMapper goalMapper;
    private final PhaseMapper phaseMapper;
    private final CheckinMapper checkinMapper;
    private final RewardMapper rewardMapper;
    private final ReviewMapper reviewMapper;
    private final SceneMapper sceneMapper;
    private final UserSettingsMapper settingsMapper;
    private final UserEncouragementMapper encouragementMapper;
    private final SyncOpLogMapper syncOpLogMapper;

    private final GoalService goalService;
    private final RewardService rewardService;
    private final ReviewService reviewService;
    private final SceneService sceneService;
    private final CheckinService checkinService;
    private final SettingsService settingsService;
    private final UserService userService;

    @Override
    public PageResult<HistoryItemVO> history(String userId, String kind, String goalId,
                                             LocalDate startDate, LocalDate endDate,
                                             long page, long pageSize) {
        List<HistoryItemVO> all = new ArrayList<>();
        boolean allKinds = kind == null || kind.isBlank() || "all".equals(kind);

        if (allKinds || "goal".equals(kind)) {
            LambdaQueryWrapper<Goal> w = new LambdaQueryWrapper<Goal>().eq(Goal::getUserId, userId);
            if (goalId != null) w.eq(Goal::getId, goalId);
            for (Goal g : goalMapper.selectList(w)) {
                HistoryItemVO it = new HistoryItemVO();
                it.setKind("goal");
                it.setId(g.getId());
                it.setTitle(g.getName());
                it.setSummary(g.getFinalGoal());
                it.setDate(g.getCreatedAt() == null ? null : g.getCreatedAt().toLocalDate());
                it.setGoalId(g.getId());
                all.add(it);
            }
        }
        if (allKinds || "checkin".equals(kind)) {
            LambdaQueryWrapper<Checkin> w = new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId);
            if (goalId != null) w.eq(Checkin::getGoalId, goalId);
            if (startDate != null) w.ge(Checkin::getCheckinDate, startDate);
            if (endDate != null) w.le(Checkin::getCheckinDate, endDate);
            for (Checkin c : checkinMapper.selectList(w)) {
                HistoryItemVO it = new HistoryItemVO();
                it.setKind("checkin");
                it.setId(c.getId());
                it.setTitle("打卡 - " + c.getStatus());
                it.setSummary("时长 " + c.getDuration() + " 分钟");
                it.setDate(c.getCheckinDate());
                it.setGoalId(c.getGoalId());
                all.add(it);
            }
        }
        if (allKinds || "review".equals(kind)) {
            LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId);
            if (goalId != null) w.eq(Review::getGoalId, goalId);
            if (startDate != null) w.ge(Review::getReviewDate, startDate);
            if (endDate != null) w.le(Review::getReviewDate, endDate);
            for (Review r : reviewMapper.selectList(w)) {
                HistoryItemVO it = new HistoryItemVO();
                it.setKind("review");
                it.setId(r.getId());
                it.setTitle(r.getTitle());
                it.setSummary(r.getContent());
                it.setDate(r.getReviewDate());
                it.setGoalId(r.getGoalId());
                all.add(it);
            }
        }
        if (allKinds || "reward".equals(kind)) {
            LambdaQueryWrapper<Reward> w = new LambdaQueryWrapper<Reward>().eq(Reward::getUserId, userId);
            if (goalId != null) w.eq(Reward::getGoalId, goalId);
            for (Reward r : rewardMapper.selectList(w)) {
                HistoryItemVO it = new HistoryItemVO();
                it.setKind("reward");
                it.setId(r.getId());
                it.setTitle(r.getName());
                it.setSummary(r.getStatus() + " - " + r.getContent());
                it.setDate(r.getClaimedAt() == null ? null : r.getClaimedAt().toLocalDate());
                it.setGoalId(r.getGoalId());
                all.add(it);
            }
        }

        all.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        int from = (int) Math.min(all.size(), (page - 1) * pageSize);
        int to = (int) Math.min(all.size(), from + pageSize);
        List<HistoryItemVO> slice = all.subList(from, to);
        return PageResult.of(slice, page, pageSize, all.size());
    }

    @Override
    public Map<String, Object> exportData(String userId, ExportDTO dto) {
        String taskId = IdGenerator.task();
        Map<String, Object> resp = new HashMap<>();
        resp.put("task_id", taskId);
        resp.put("status", "ready");
        resp.put("download_url", "/api/v1/data/export/" + taskId + "/download");
        return resp;
    }

    @Override
    public Map<String, Object> exportTask(String userId, String taskId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("task_id", taskId);
        resp.put("status", "ready");
        resp.put("download_url", "/api/v1/data/export/" + taskId + "/download");
        return resp;
    }

    @Override
    @Transactional
    public SyncPushResultVO push(String userId, SyncPushDTO dto) {
        SyncPushResultVO vo = new SyncPushResultVO();
        vo.setServerTs(OffsetDateTime.now(ZoneOffset.UTC));
        List<SyncPushResultVO.Item> items = new ArrayList<>();
        if (dto.getOperations() != null) {
            for (SyncPushDTO.Operation op : dto.getOperations()) {
                items.add(executeOp(userId, op));
            }
        }
        vo.setResults(items);
        return vo;
    }

    @Override
    public SyncPullResultVO pull(String userId, OffsetDateTime since) {
        SyncPullResultVO vo = new SyncPullResultVO();
        vo.setServerTs(OffsetDateTime.now(ZoneOffset.UTC));
        vo.setGoals(bucket(goalMapper.selectList(new LambdaQueryWrapper<Goal>().eq(Goal::getUserId, userId))));
        vo.setCheckins(bucket(checkinMapper.selectList(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId))));
        vo.setRewards(bucket(rewardMapper.selectList(new LambdaQueryWrapper<Reward>().eq(Reward::getUserId, userId))));
        vo.setReviews(bucket(reviewMapper.selectList(new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId))));
        vo.setScenes(bucket(sceneMapper.selectList(new LambdaQueryWrapper<Scene>().eq(Scene::getUserId, userId))));
        vo.setProfile(userService.getMe(userId));
        vo.setSettings(settingsService.get(userId));
        return vo;
    }

    @Override
    @Transactional
    public void reset(String userId, ResetDTO dto) {
        if (!"RESET".equals(dto.getConfirm())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "confirm 必须为 RESET");
        }
        for (Goal g : goalMapper.selectList(new LambdaQueryWrapper<Goal>().eq(Goal::getUserId, userId))) {
            goalMapper.deleteById(g.getId());
        }
        for (Phase p : phaseMapper.selectList(new LambdaQueryWrapper<Phase>().eq(Phase::getUserId, userId))) {
            phaseMapper.deleteById(p.getId());
        }
        for (Checkin c : checkinMapper.selectList(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId, userId))) {
            checkinMapper.deleteById(c.getId());
        }
        for (Reward r : rewardMapper.selectList(new LambdaQueryWrapper<Reward>().eq(Reward::getUserId, userId))) {
            rewardMapper.deleteById(r.getId());
        }
        for (Review r : reviewMapper.selectList(new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId))) {
            reviewMapper.deleteById(r.getId());
        }
        for (Scene s : sceneMapper.selectList(new LambdaQueryWrapper<Scene>().eq(Scene::getUserId, userId))) {
            sceneMapper.deleteById(s.getId());
        }
        for (UserEncouragement e : encouragementMapper.selectList(new LambdaQueryWrapper<UserEncouragement>()
                .eq(UserEncouragement::getUserId, userId))) {
            encouragementMapper.deleteById(e.getId());
        }
    }

    // ---------- private ----------

    private SyncPushResultVO.Item executeOp(String userId, SyncPushDTO.Operation op) {
        SyncPushResultVO.Item item = new SyncPushResultVO.Item();
        item.setClientOpId(op.getClientOpId());

        // 幂等检查
        if (op.getClientOpId() != null) {
            SyncOpLog exist = syncOpLogMapper.selectOne(new LambdaQueryWrapper<SyncOpLog>()
                    .eq(SyncOpLog::getUserId, userId)
                    .eq(SyncOpLog::getClientOpId, op.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) {
                item.setOk(Boolean.TRUE.equals(exist.getSuccess()));
                item.setData(exist.getResultJson() == null ? null
                        : JsonUtil.fromJson(exist.getResultJson(), Object.class));
                return item;
            }
        }

        try {
            Object result = dispatch(userId, op.getType(), op.getPayload());
            item.setOk(true);
            item.setData(result);
            recordLog(userId, op, true, result);
        } catch (BusinessException e) {
            item.setOk(false);
            Map<String, Object> err = new HashMap<>();
            err.put("code", e.getCode());
            err.put("message", e.getMessage());
            item.setError(err);
            recordLog(userId, op, false, err);
        } catch (Exception e) {
            item.setOk(false);
            Map<String, Object> err = new HashMap<>();
            err.put("code", ResultCode.INTERNAL_ERROR.getCode());
            err.put("message", e.getMessage());
            item.setError(err);
            recordLog(userId, op, false, err);
        }
        return item;
    }

    private void recordLog(String userId, SyncPushDTO.Operation op, boolean success, Object result) {
        if (op.getClientOpId() == null) return;
        SyncOpLog log = new SyncOpLog();
        log.setUserId(userId);
        log.setClientOpId(op.getClientOpId());
        log.setOpType(op.getType());
        log.setPayloadJson(JsonUtil.toJson(op.getPayload()));
        log.setResultJson(JsonUtil.toJson(result));
        log.setSuccess(success);
        try {
            syncOpLogMapper.insert(log);
        } catch (Exception ignored) { /* 唯一索引冲突视为已写入 */ }
    }

    private Object dispatch(String userId, String type, Map<String, Object> payload) {
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "type 不能为空");
        }
        return switch (type) {
            case "goal.create" -> goalService.create(userId, MAPPER.convertValue(payload, CreateGoalDTO.class)).getData();
            case "goal.update" -> goalService.update(userId, str(payload, "goal_id"),
                    MAPPER.convertValue(payload, UpdateGoalDTO.class));
            case "goal.archive" -> { goalService.archive(userId, str(payload, "goal_id")); yield null; }
            case "goal.delete" -> { goalService.delete(userId, str(payload, "goal_id"), false); yield null; }

            case "checkin.create" -> checkinService.checkin(userId, str(payload, "goal_id"),
                    MAPPER.convertValue(payload, CreateCheckinDTO.class));
            case "checkin.missed" -> checkinService.missed(userId, str(payload, "goal_id"),
                    MAPPER.convertValue(payload, MissedCheckinDTO.class));
            case "checkin.delete" -> {
                checkinService.delete(userId, str(payload, "goal_id"), LocalDate.parse(str(payload, "date")));
                yield null;
            }

            case "reward.create" -> rewardService.create(userId, str(payload, "goal_id"),
                    MAPPER.convertValue(payload, CreateRewardDTO.class));
            case "reward.update" -> rewardService.update(userId, str(payload, "goal_id"),
                    str(payload, "reward_id"), MAPPER.convertValue(payload, UpdateRewardDTO.class));
            case "reward.delete" -> {
                rewardService.delete(userId, str(payload, "goal_id"), str(payload, "reward_id"));
                yield null;
            }
            case "reward.claim" -> rewardService.claim(userId, str(payload, "goal_id"), str(payload, "reward_id"));

            case "review.create" -> reviewService.create(userId, MAPPER.convertValue(payload, CreateReviewDTO.class));
            case "review.update" -> reviewService.update(userId, str(payload, "review_id"),
                    MAPPER.convertValue(payload, UpdateReviewDTO.class));
            case "review.delete" -> { reviewService.delete(userId, str(payload, "review_id")); yield null; }
            case "review.favorite" -> reviewService.favorite(userId, str(payload, "review_id"),
                    MAPPER.convertValue(payload, FavoriteDTO.class));

            case "scene.create" -> sceneService.create(userId, MAPPER.convertValue(payload, CreateSceneDTO.class));
            case "scene.update" -> sceneService.update(userId, str(payload, "scene_id"),
                    MAPPER.convertValue(payload, UpdateSceneDTO.class));
            case "scene.delete" -> { sceneService.delete(userId, str(payload, "scene_id")); yield null; }

            case "profile.update" -> userService.updateProfile(userId,
                    MAPPER.convertValue(payload, UpdateProfileDTO.class));
            case "settings.update" -> settingsService.update(userId,
                    MAPPER.convertValue(payload, UpdateSettingsDTO.class));

            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的同步操作类型: " + type);
        };
    }

    private String str(Map<String, Object> payload, String key) {
        Object v = payload == null ? null : payload.get(key);
        if (v == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, key + " 不能为空");
        }
        return v.toString();
    }

    private SyncPullResultVO.Bucket bucket(List<?> upserts) {
        SyncPullResultVO.Bucket b = new SyncPullResultVO.Bucket();
        b.setUpserts(upserts == null ? new ArrayList<>() : upserts);
        b.setDeletes(List.of());
        return b;
    }
}
