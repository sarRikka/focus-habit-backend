package com.atomic.focus.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDeviceDTO {

    @NotBlank
    private String platform;

    @NotBlank
    @JsonProperty("push_token")
    private String pushToken;

    @JsonProperty("device_id")
    private String deviceId;
}
