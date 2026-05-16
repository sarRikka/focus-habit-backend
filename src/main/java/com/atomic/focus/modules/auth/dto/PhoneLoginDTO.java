package com.atomic.focus.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhoneLoginDTO {

    @NotBlank(message = "phone 不能为空")
    private String phone;

    /** 明文口令，HTTPS 传输；服务端仅存哈希 */
    @NotBlank(message = "password 不能为空")
    @Size(min = 8, max = 72, message = "password 长度需在 8-72 之间")
    private String password;

    /** 可选：游客升级或登录时并入游客侧数据 */
    private String mergeGuestUserId;
}
