package com.atomic.focus.modules.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEncouragementDTO {

    @NotBlank(message = "content 不能为空")
    private String content;
}
