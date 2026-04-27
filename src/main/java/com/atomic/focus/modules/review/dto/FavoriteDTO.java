package com.atomic.focus.modules.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FavoriteDTO {

    @JsonProperty("is_favorite")
    private Boolean isFavorite;
}
