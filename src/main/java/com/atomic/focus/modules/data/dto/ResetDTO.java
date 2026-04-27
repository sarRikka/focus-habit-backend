package com.atomic.focus.modules.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetDTO {

    @NotBlank
    private String confirm;
}
