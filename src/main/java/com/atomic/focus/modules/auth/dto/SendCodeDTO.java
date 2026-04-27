package com.atomic.focus.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeDTO {

    @NotBlank
    private String phone;

    /** login | bind | reset */
    private String scene = "login";
}
