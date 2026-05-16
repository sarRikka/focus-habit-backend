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
import com.atomic.focus.modules.auth.service.GuestMergeService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 认证服务：游客、手机号密码注册/登录、Token 刷新；游客合并见 API.md §15。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_FAILED_MSG = "手机号或密码错误";
    /** 可选短信验证码联调占位；当前 Web 走密码链路，一般不调用 */
    private static final String DEMO_CODE = "123456";

    private final UserMapper userMapper;
    private final SettingsService settingsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final GuestMergeService guestMergeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public AuthVO register(PhoneLoginDTO dto) {
        String phone = normalizePhone(dto.getPhone());
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.CONFLICT, "手机号已注册，请使用登录");
        }

        String mergeId = dto.getMergeGuestUserId();
        if (mergeId != null && !mergeId.isBlank()) {
            User guest = userMapper.selectById(mergeId.trim());
            if (guest == null || !Boolean.TRUE.equals(guest.getIsGuest())) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "merge_guest_user_id 无效");
            }
            if (guest.getPhone() != null && !guest.getPhone().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "merge_guest_user_id 无效");
            }
            guest.setPhone(phone);
            guest.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            guest.setIsGuest(false);
            if (guest.getNickname() != null && guest.getNickname().startsWith("访客")) {
                guest.setNickname(defaultNickname(phone));
            }
            userMapper.updateById(guest);
            return buildAuth(requireFreshUser(guest.getId()));
        }

        User user = createPhoneUser(phone, dto.getPassword());
        return buildAuth(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthVO phoneLogin(PhoneLoginDTO dto) {
        String phone = normalizePhone(dto.getPhone());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, LOGIN_FAILED_MSG);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, LOGIN_FAILED_MSG);
        }

        String mergeId = dto.getMergeGuestUserId();
        if (mergeId != null && !mergeId.isBlank()) {
            String gid = mergeId.trim();
            if (!gid.equals(user.getId())) {
                User guest = userMapper.selectById(gid);
                if (guest == null || !Boolean.TRUE.equals(guest.getIsGuest())) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "merge_guest_user_id 无效");
                }
                absorbGuestProfileStats(user, guest);
                userMapper.updateById(user);
                guestMergeService.mergeGuestIntoUser(gid, user.getId());
                user = requireFreshUser(user.getId());
            }
        }

        return buildAuth(user);
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        log.info("[可选短信占位] {} 验证码 {}（scene: {}），当前 Web 以密码为准", dto.getPhone(), DEMO_CODE,
                dto.getScene());
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
        // 无状态 JWT：此处不做服务端会话清理；需要时可做 token 黑名单
    }

    private User createGuest(String deviceId) {
        User u = new User();
        u.setId(IdGenerator.user());
        u.setIsGuest(true);
        u.setDeviceId(deviceId);
        u.setNickname("访客" + u.getId().substring(2, 8));
        u.setJoinedAt(LocalDate.now());
        u.setTotalCheckinDays(0);
        u.setFixedHabitsCount(0);
        u.setContinuousDays(0);
        userMapper.insert(u);
        settingsService.initDefaults(u.getId());
        return u;
    }

    private User createPhoneUser(String phone, String rawPassword) {
        User u = new User();
        u.setId(IdGenerator.user());
        u.setIsGuest(false);
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setNickname(defaultNickname(phone));
        u.setJoinedAt(LocalDate.now());
        u.setTotalCheckinDays(0);
        u.setFixedHabitsCount(0);
        u.setContinuousDays(0);
        userMapper.insert(u);
        settingsService.initDefaults(u.getId());
        return u;
    }

    private User requireFreshUser(String userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "用户数据异常");
        }
        return u;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.strip();
    }

    private static String defaultNickname(String phone) {
        String tail = phone == null || phone.isBlank() ? "用户" : phone.substring(Math.max(0, phone.length() - 4));
        return "原子用户" + tail;
    }

    /**
     * 合并前汇总游客侧计数到正式账号（正式侧业务数据随后在 merge 中归并）。
     */
    private static void absorbGuestProfileStats(User target, User guest) {
        int tCd = nz(target.getTotalCheckinDays());
        int gCd = nz(guest.getTotalCheckinDays());
        if (gCd > tCd) {
            target.setTotalCheckinDays(gCd);
        }
        int tFixed = nz(target.getFixedHabitsCount());
        int gFixed = nz(guest.getFixedHabitsCount());
        if (gFixed > tFixed) {
            target.setFixedHabitsCount(gFixed);
        }
        int tCo = nz(target.getContinuousDays());
        int gCo = nz(guest.getContinuousDays());
        if (gCo > tCo) {
            target.setContinuousDays(gCo);
        }
        if ((target.getDeviceId() == null || target.getDeviceId().isBlank())
                && guest.getDeviceId() != null && !guest.getDeviceId().isBlank()) {
            target.setDeviceId(guest.getDeviceId());
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
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
