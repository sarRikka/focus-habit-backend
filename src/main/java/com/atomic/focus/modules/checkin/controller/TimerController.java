package com.atomic.focus.modules.checkin.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.R;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.checkin.dto.CreateCheckinDTO;
import com.atomic.focus.modules.checkin.service.CheckinService;
import com.atomic.focus.modules.checkin.vo.CheckinResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 计时器会话（多端同步用）。
 * 当前实现使用进程内 Map，仅作演示；生产应使用 Redis 持久化。
 */
@RestController
@RequestMapping("/api/v1/timer")
@RequiredArgsConstructor
public class TimerController {

    private final CheckinService checkinService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @PostMapping("/start")
    public R<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        String userId = UserContext.requireUserId();
        String goalId = (String) body.get("goal_id");
        if (goalId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "goal_id 不能为空");
        }
        Session s = new Session();
        s.id = IdGenerator.session();
        s.userId = userId;
        s.goalId = goalId;
        s.startedAt = Instant.now();
        s.status = "running";
        sessions.put(s.id, s);
        Map<String, Object> resp = new HashMap<>();
        resp.put("session_id", s.id);
        resp.put("status", s.status);
        return R.ok(resp);
    }

    @PostMapping("/{sessionId}/pause")
    public R<Map<String, Object>> pause(@PathVariable String sessionId) {
        Session s = require(sessionId);
        if ("running".equals(s.status)) {
            s.elapsed += Instant.now().getEpochSecond() - s.startedAt.getEpochSecond();
            s.status = "paused";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("session_id", s.id);
        data.put("status", s.status);
        data.put("elapsed_seconds", s.elapsed);
        return R.ok(data);
    }

    @PostMapping("/{sessionId}/resume")
    public R<Map<String, Object>> resume(@PathVariable String sessionId) {
        Session s = require(sessionId);
        if ("paused".equals(s.status)) {
            s.startedAt = Instant.now();
            s.status = "running";
        }
        Map<String, Object> data = new HashMap<>();
        data.put("session_id", s.id);
        data.put("status", s.status);
        return R.ok(data);
    }

    @PostMapping("/{sessionId}/finish")
    public R<CheckinResultVO> finish(@PathVariable String sessionId,
                                     @RequestBody(required = false) Map<String, Object> body) {
        Session s = require(sessionId);
        if ("running".equals(s.status)) {
            s.elapsed += Instant.now().getEpochSecond() - s.startedAt.getEpochSecond();
        }
        sessions.remove(sessionId);

        CreateCheckinDTO dto = new CreateCheckinDTO();
        dto.setDuration(Math.max(0, (int) Math.ceil(s.elapsed / 60.0)));
        dto.setStatus("done");
        if (body != null && body.get("note") != null) dto.setNote(body.get("note").toString());
        return R.ok(checkinService.checkin(s.userId, s.goalId, dto));
    }

    @GetMapping("/active")
    public R<Map<String, Object>> active() {
        String userId = UserContext.requireUserId();
        return sessions.values().stream()
                .filter(s -> userId.equals(s.userId))
                .findFirst()
                .map(s -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("session_id", s.id);
                    data.put("goal_id", s.goalId);
                    data.put("status", s.status);
                    data.put("elapsed_seconds", s.elapsed);
                    return R.ok(data);
                })
                .orElseGet(() -> R.ok(null));
    }

    private Session require(String sessionId) {
        Session s = sessions.get(sessionId);
        if (s == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在或已结束");
        }
        if (!s.userId.equals(UserContext.requireUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return s;
    }

    private static class Session {
        String id;
        String userId;
        String goalId;
        Instant startedAt;
        long elapsed;
        String status;
    }
}
