package com.atomic.focus.modules.scene.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.modules.scene.dto.CreateSceneDTO;
import com.atomic.focus.modules.scene.dto.UpdateSceneDTO;
import com.atomic.focus.modules.scene.entity.Scene;
import com.atomic.focus.modules.scene.mapper.SceneMapper;
import com.atomic.focus.modules.scene.service.SceneService;
import com.atomic.focus.modules.scene.vo.SceneListVO;
import com.atomic.focus.modules.scene.vo.SceneVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SceneServiceImpl implements SceneService {

    private static final Set<String> TYPES = Set.of("holiday", "travel", "sick", "other");
    private static final Set<String> MODES = Set.of("shorten", "extend", "pause");

    private final SceneMapper sceneMapper;

    @Override
    public SceneListVO list(String userId) {
        LocalDate today = LocalDate.now();
        List<Scene> all = sceneMapper.selectList(new LambdaQueryWrapper<Scene>()
                .eq(Scene::getUserId, userId)
                .orderByDesc(Scene::getStartDate));
        SceneListVO vo = new SceneListVO();
        vo.setItems(all.stream().map(s -> toVO(s, today)).collect(Collectors.toList()));
        vo.getItems().stream().filter(SceneVO::getActive).findFirst().ifPresent(s -> {
            Map<String, Object> active = new HashMap<>();
            active.put("id", s.getId());
            active.put("mode", s.getMode());
            active.put("shorten_to", s.getShortenTo());
            active.put("extend_hours", s.getExtendHours());
            vo.setActiveScene(active);
        });
        return vo;
    }

    @Override
    @Transactional
    public SceneVO create(String userId, CreateSceneDTO dto) {
        validate(dto.getType(), dto.getMode(), dto.getStartDate(), dto.getEndDate(),
                dto.getShortenTo(), dto.getExtendHours());

        if (dto.getClientOpId() != null) {
            Scene exist = sceneMapper.selectOne(new LambdaQueryWrapper<Scene>()
                    .eq(Scene::getUserId, userId)
                    .eq(Scene::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) return toVO(exist, LocalDate.now());
        }

        // 重叠校验
        Long overlap = sceneMapper.selectCount(new LambdaQueryWrapper<Scene>()
                .eq(Scene::getUserId, userId)
                .le(Scene::getStartDate, dto.getEndDate())
                .ge(Scene::getEndDate, dto.getStartDate()));
        if (overlap != null && overlap > 0) {
            throw new BusinessException(ResultCode.SCENE_OVERLAP);
        }

        Scene s = new Scene();
        s.setId(IdGenerator.scene());
        s.setUserId(userId);
        s.setType(dto.getType());
        s.setLabel(dto.getLabel());
        s.setStartDate(dto.getStartDate());
        s.setEndDate(dto.getEndDate());
        s.setMode(dto.getMode());
        s.setShortenTo(dto.getShortenTo());
        s.setExtendHours(dto.getExtendHours());
        s.setClientOpId(dto.getClientOpId());
        sceneMapper.insert(s);
        return toVO(s, LocalDate.now());
    }

    @Override
    @Transactional
    public SceneVO update(String userId, String sceneId, UpdateSceneDTO dto) {
        Scene s = require(userId, sceneId);
        if (dto.getType() != null) s.setType(dto.getType());
        if (dto.getLabel() != null) s.setLabel(dto.getLabel());
        if (dto.getStartDate() != null) s.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) s.setEndDate(dto.getEndDate());
        if (dto.getMode() != null) s.setMode(dto.getMode());
        if (dto.getShortenTo() != null) s.setShortenTo(dto.getShortenTo());
        if (dto.getExtendHours() != null) s.setExtendHours(dto.getExtendHours());
        validate(s.getType(), s.getMode(), s.getStartDate(), s.getEndDate(),
                s.getShortenTo(), s.getExtendHours());
        sceneMapper.updateById(s);
        return toVO(s, LocalDate.now());
    }

    @Override
    public void delete(String userId, String sceneId) {
        Scene s = require(userId, sceneId);
        sceneMapper.deleteById(s.getId());
    }

    private Scene require(String userId, String sceneId) {
        Scene s = sceneMapper.selectById(sceneId);
        if (s == null || !userId.equals(s.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场景不存在");
        }
        return s;
    }

    private SceneVO toVO(Scene s, LocalDate today) {
        SceneVO vo = new SceneVO();
        vo.setId(s.getId());
        vo.setType(s.getType());
        vo.setLabel(s.getLabel());
        vo.setStartDate(s.getStartDate());
        vo.setEndDate(s.getEndDate());
        vo.setMode(s.getMode());
        vo.setShortenTo(s.getShortenTo());
        vo.setExtendHours(s.getExtendHours());
        vo.setActive(!today.isBefore(s.getStartDate()) && !today.isAfter(s.getEndDate()));
        return vo;
    }

    private void validate(String type, String mode, LocalDate start, LocalDate end,
                          Integer shortenTo, Integer extendHours) {
        if (type != null && !TYPES.contains(type)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "type 取值非法");
        }
        if (mode != null && !MODES.contains(mode)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "mode 取值非法");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "end_date 必须 >= start_date");
        }
        if ("pause".equals(mode) && start != null && end != null) {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            if (days > 3) throw new BusinessException(ResultCode.SCENE_PAUSE_LIMIT);
        }
        if ("shorten".equals(mode) && (shortenTo == null || shortenTo < 1)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "shorten_to 必须 ≥1");
        }
        if ("extend".equals(mode) && (extendHours == null || extendHours < 1 || extendHours > 3)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "extend_hours 必须在 1-3");
        }
    }
}
