package com.atomic.focus.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneLoginDTO {

    @NotBlank(message = "phone 不能为空")
    private String phone;

    @NotBlank(message = "code 不能为空")
    private String code;

    /** 可选：将游客数据合并到正式账户 */
    private String mergeGuestUserId;
}
