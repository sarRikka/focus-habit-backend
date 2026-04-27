package com.atomic.focus.modules.scene.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SceneListVO {

    private List<SceneVO> items;

    @JsonProperty("active_scene")
    private Map<String, Object> activeScene;
}
