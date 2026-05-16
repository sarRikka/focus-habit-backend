package com.atomic.focus.modules.auth.controller;

import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.auth.dto.GuestLoginDTO;
import com.atomic.focus.modules.auth.dto.PhoneLoginDTO;
import com.atomic.focus.modules.auth.dto.RefreshTokenDTO;
import com.atomic.focus.modules.auth.dto.SendCodeDTO;
import com.atomic.focus.modules.auth.service.AuthService;
import com.atomic.focus.modules.auth.vo.AuthVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证模块。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 创建游客账户 */
    @PostMapping("/guest")
    public R<AuthVO> guest(@RequestBody(required = false) GuestLoginDTO dto) {
        return R.ok(authService.guestLogin(dto));
    }

    /** 手机号 + 密码登录 */
    @PostMapping("/login")
    public R<AuthVO> login(@RequestBody @Valid PhoneLoginDTO dto) {
        return R.ok(authService.phoneLogin(dto));
    }

    /** 手机号注册（可与游客数据合并）；参见 API §2.3 */
    @PostMapping("/register")
    public R<AuthVO> register(@RequestBody @Valid PhoneLoginDTO dto) {
        return R.ok(authService.register(dto));
    }

    /** 发送短信验证码 */
    @PostMapping("/send-code")
    public R<Void> sendCode(@RequestBody @Valid SendCodeDTO dto) {
        authService.sendCode(dto);
        return R.ok();
    }

    /** 刷新 Token */
    @PostMapping("/refresh")
    public R<AuthVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return R.ok(authService.refresh(dto));
    }

    /** 登出 */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}
