package com.atomic.focus.modules.auth.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.common.util.JwtUtil;
import com.atomic.focus.modules.auth.dto.GuestLoginDTO;
import com.atomic.focus.modules.auth.dto.PhoneLoginDTO;
import com.atomic.focus.modules.auth.dto.RefreshTokenDTO;
import com.atomic.focus.modules.auth.dto.SendCodeDTO;
import com.atomic.focus.modules.auth.service.AuthService;
import com.atomic.focus.modules.auth.vo.AuthVO;
import com.atomic.focus.modules.settings.service.SettingsService;
import com.atomic.focus.modules.user.entity.User;
import com.atomic.focus.modules.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 认证服务实现：游客登录、手机号登录、Token 刷新。
 *
 * 注意：
 * - 短信验证码当前使用固定 "123456" 进行联调；生产环境应接入真实短信服务并使用 Redis 校验。
 * - 数据合并（merge_guest_user_id）当前以接管用户身份的方式实现：将该游客账号转为正式用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEMO_CODE = "123456";

    private final UserMapper userMapper;
    private final SettingsService settingsService;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthVO guestLogin(GuestLoginDTO dto) {
        User user = null;
        if (dto != null && dto.getDeviceId() != null && !dto.getDeviceId().isBlank()) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getDeviceId, dto.getDeviceId())
                    .eq(User::getIsGuest, true)
                    .last("LIMIT 1"));
        }
        if (user == null) {
            user = createGuest(dto == null ? null : dto.getDeviceId());
        }
        return buildAuth(user);
    }

    @Override
    @Transactional
    public AuthVO phoneLogin(PhoneLoginDTO dto) {
        if (!DEMO_CODE.equals(dto.getCode())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "验证码错误");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, dto.getPhone())
                .last("LIMIT 1"));

        if (user == null) {
            // 若提供 mergeGuestUserId，则把游客账号升级为正式账号
            if (dto.getMergeGuestUserId() != null && !dto.getMergeGuestUserId().isBlank()) {
                user = userMapper.selectById(dto.getMergeGuestUserId());
                if (user != null && Boolean.TRUE.equals(user.getIsGuest())) {
                    user.setIsGuest(false);
                    user.setPhone(dto.getPhone());
                    if (user.getNickname() == null || user.getNickname().isBlank()) {
                        user.setNickname(defaultNickname(dto.getPhone()));
                    }
                    userMapper.updateById(user);
                }
            }
            if (user == null) {
                user = createPhoneUser(dto.getPhone());
            }
        }
        return buildAuth(user);
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        // TODO 接入真实短信平台；当前固定验证码用于联调
        log.info("[模拟短信] 向 {} 发送验证码 {}（场景: {}）", dto.getPhone(), DEMO_CODE, dto.getScene());
    }

    @Override
    public AuthVO refresh(RefreshTokenDTO dto) {
        try {
            Claims claims = jwtUtil.parse(dto.getRefreshToken());
            if (!jwtUtil.isRefreshToken(claims)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "非法 refresh_token");
            }
            String userId = claims.getSubject();
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
            }
            return buildAuth(user);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void logout() {
        // 由于使用无状态 JWT，此处不做服务端注销处理；如需注销则将 token 加入黑名单。
    }

    // ---------- 私有方法 ----------

    private User createGuest(String deviceId) {
        User user = new User();
        user.setId(IdGenerator.user());
        user.setIsGuest(true);
        user.setDeviceId(deviceId);
        user.setNickname("访客" + user.getId().substring(2, 8));
        user.setJoinedAt(LocalDate.now());
        user.setTotalCheckinDays(0);
        user.setFixedHabitsCount(0);
        user.setContinuousDays(0);
        userMapper.insert(user);
        settingsService.initDefaults(user.getId());
        return user;
    }

    private User createPhoneUser(String phone) {
        User user = new User();
        user.setId(IdGenerator.user());
        user.setIsGuest(false);
        user.setPhone(phone);
        user.setNickname(defaultNickname(phone));
        user.setJoinedAt(LocalDate.now());
        user.setTotalCheckinDays(0);
        user.setFixedHabitsCount(0);
        user.setContinuousDays(0);
        userMapper.insert(user);
        settingsService.initDefaults(user.getId());
        return user;
    }

    private String defaultNickname(String phone) {
        String tail = phone == null ? "用户" : phone.substring(Math.max(0, phone.length() - 4));
        return "原子用户" + tail;
    }

    private AuthVO buildAuth(User user) {
        AuthVO vo = new AuthVO();
        vo.setUserId(user.getId());
        vo.setIsGuest(Boolean.TRUE.equals(user.getIsGuest()));
        vo.setAccessToken(jwtUtil.issueAccessToken(user.getId(), vo.getIsGuest()));
        vo.setRefreshToken(jwtUtil.issueRefreshToken(user.getId(), vo.getIsGuest()));
        vo.setExpiresIn(jwtUtil.getAccessExpireSeconds());
        return vo;
    }
}
