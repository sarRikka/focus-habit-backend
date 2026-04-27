package com.atomic.focus.modules.scene.service;

import com.atomic.focus.modules.scene.dto.CreateSceneDTO;
import com.atomic.focus.modules.scene.dto.UpdateSceneDTO;
import com.atomic.focus.modules.scene.vo.SceneListVO;
import com.atomic.focus.modules.scene.vo.SceneVO;

public interface SceneService {

    SceneListVO list(String userId);

    SceneVO create(String userId, CreateSceneDTO dto);

    SceneVO update(String userId, String sceneId, UpdateSceneDTO dto);

    void delete(String userId, String sceneId);
}
