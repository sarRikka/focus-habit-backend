package com.atomic.focus.modules.auth.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthVO {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("is_guest")
    private Boolean isGuest;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
